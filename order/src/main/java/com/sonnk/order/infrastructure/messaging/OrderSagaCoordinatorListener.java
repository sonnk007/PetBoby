package com.sonnk.order.infrastructure.messaging;

import com.sonnk.order.application.event.saga.InventoryReleaseRequestedEvent;
import com.sonnk.order.application.event.saga.InventoryReserveFailedEvent;
import com.sonnk.order.application.event.saga.InventoryReservedEvent;
import com.sonnk.order.application.event.saga.OrderCancelledEvent;
import com.sonnk.order.application.event.saga.OrderConfirmedEvent;
import com.sonnk.order.application.event.saga.PaymentCompletedEvent;
import com.sonnk.order.application.event.saga.PaymentFailedEvent;
import com.sonnk.order.application.event.saga.PaymentRequestedEvent;
import com.sonnk.order.infrastructure.idempotency.IdempotentConsumerHelper;
import com.sonnk.order.model.entity.Order;
import com.sonnk.order.model.entity.enums.OrderSagaState;
import com.sonnk.order.model.entity.enums.OrderStatus;
import com.sonnk.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Saga choreography coordinator (order service side) — production-grade với idempotency.
 *
 * Idempotency pattern:
 * - Mỗi handler gọi {@code idempotentConsumerHelper.tryMarkProcessed(eventId, GROUP, topic)} ngay đầu.
 * - INSERT processed_events + nghiệp vụ nằm trong CÙNG @Transactional:
 *   → Cả hai commit cùng lúc hoặc cả hai rollback → at-least-once an toàn, không double-process.
 * - Unique constraint (event_id, consumer_group) tại DB là tường thành chặn race condition đa instance.
 *
 * Xem chi tiết pattern: docs/kafka-data-integrity-and-deep-dive.md
 */
@Component
public class OrderSagaCoordinatorListener {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaCoordinatorListener.class);

    static final String CONSUMER_GROUP = "order-saga-coordinator";

    private final OrderRepository orderRepository;
    private final SagaEventPublisher sagaEventPublisher;
    private final IdempotentConsumerHelper idempotentConsumerHelper;

    public OrderSagaCoordinatorListener(OrderRepository orderRepository,
                                        SagaEventPublisher sagaEventPublisher,
                                        IdempotentConsumerHelper idempotentConsumerHelper) {
        this.orderRepository = orderRepository;
        this.sagaEventPublisher = sagaEventPublisher;
        this.idempotentConsumerHelper = idempotentConsumerHelper;
    }

    /**
     * Bước 2 saga: inventory reserve thành công → chuyển sang payment.
     * Idempotency: cùng InventoryReservedEvent.eventId chỉ được xử lý một lần.
     */
    @Transactional
    @KafkaListener(
            topics = SagaTopics.SAGA_INVENTORY_RESERVED,
            groupId = CONSUMER_GROUP,
            properties = "spring.json.value.default.type=com.sonnk.order.application.event.saga.InventoryReservedEvent"
    )
    public void onInventoryReserved(InventoryReservedEvent event) {
        if (event == null) return;
        if (!idempotentConsumerHelper.tryMarkProcessed(event.eventId(), CONSUMER_GROUP, SagaTopics.SAGA_INVENTORY_RESERVED)) {
            return;
        }

        Order order = getOrderOrThrow(event.orderId());
        order.setSagaState(OrderSagaState.PAYMENT_PENDING);
        order.setStatus(OrderStatus.IN_PROGRESS);

        log.info("[SAGA] Inventory reserved orderId={} eventId={} → requesting payment",
                event.orderId(), event.eventId());
        sagaEventPublisher.publishPaymentRequested(
                new PaymentRequestedEvent(
                        UUID.randomUUID().toString(),
                        order.getId(),
                        order.getOrderCode(),
                        order.getBranchCode(),
                        order.getFinalAmount()
                )
        );
    }

    /**
     * Bước 2 saga: inventory reserve thất bại → cancel order ngay.
     * Không cần compensation vì inventory chưa reserve được.
     */
    @Transactional
    @KafkaListener(
            topics = SagaTopics.SAGA_INVENTORY_FAILED,
            groupId = CONSUMER_GROUP,
            properties = "spring.json.value.default.type=com.sonnk.order.application.event.saga.InventoryReserveFailedEvent"
    )
    public void onInventoryFailed(InventoryReserveFailedEvent event) {
        if (event == null) return;
        if (!idempotentConsumerHelper.tryMarkProcessed(event.eventId(), CONSUMER_GROUP, SagaTopics.SAGA_INVENTORY_FAILED)) {
            return;
        }

        Order order = getOrderOrThrow(event.orderId());
        order.setSagaState(OrderSagaState.INVENTORY_FAILED);
        order.setStatus(OrderStatus.CANCELLED);

        log.warn("[SAGA] Inventory failed orderId={} eventId={} reason={}",
                event.orderId(), event.eventId(), event.reason());
        sagaEventPublisher.publishOrderCancelled(
                new OrderCancelledEvent(UUID.randomUUID().toString(), order.getId(), order.getOrderCode(), event.reason())
        );
    }

    /**
     * Bước 3 saga: payment thành công → confirm order (saga COMPLETED).
     */
    @Transactional
    @KafkaListener(
            topics = SagaTopics.SAGA_PAYMENT_COMPLETED,
            groupId = CONSUMER_GROUP,
            properties = "spring.json.value.default.type=com.sonnk.order.application.event.saga.PaymentCompletedEvent"
    )
    public void onPaymentCompleted(PaymentCompletedEvent event) {
        if (event == null) return;
        if (!idempotentConsumerHelper.tryMarkProcessed(event.eventId(), CONSUMER_GROUP, SagaTopics.SAGA_PAYMENT_COMPLETED)) {
            return;
        }

        Order order = getOrderOrThrow(event.orderId());
        order.setSagaState(OrderSagaState.COMPLETED);
        order.setStatus(OrderStatus.PAID);

        log.info("[SAGA] Payment completed orderId={} eventId={} txRef={}",
                event.orderId(), event.eventId(), event.transactionRef());
        sagaEventPublisher.publishOrderConfirmed(
                new OrderConfirmedEvent(UUID.randomUUID().toString(), order.getId(), order.getOrderCode())
        );
    }

    /**
     * Bước 3 saga: payment thất bại → compensation: release inventory đã reserve, cancel order.
     * sagaState: COMPENSATING → COMPENSATED.
     */
    @Transactional
    @KafkaListener(
            topics = SagaTopics.SAGA_PAYMENT_FAILED,
            groupId = CONSUMER_GROUP,
            properties = "spring.json.value.default.type=com.sonnk.order.application.event.saga.PaymentFailedEvent"
    )
    public void onPaymentFailed(PaymentFailedEvent event) {
        if (event == null) return;
        if (!idempotentConsumerHelper.tryMarkProcessed(event.eventId(), CONSUMER_GROUP, SagaTopics.SAGA_PAYMENT_FAILED)) {
            return;
        }

        Order order = getOrderOrThrow(event.orderId());
        order.setSagaState(OrderSagaState.COMPENSATING);
        order.setStatus(OrderStatus.CANCELLED);

        log.warn("[SAGA] Payment failed orderId={} eventId={} reason={} → triggering compensation",
                event.orderId(), event.eventId(), event.reason());

        // Compensation: yêu cầu inventory service hoàn trả stock đã reserve.
        sagaEventPublisher.publishInventoryReleaseRequested(
                new InventoryReleaseRequestedEvent(
                        UUID.randomUUID().toString(),
                        order.getId(),
                        order.getOrderCode(),
                        "Payment failed: " + event.reason()
                )
        );

        order.setSagaState(OrderSagaState.COMPENSATED);
        sagaEventPublisher.publishOrderCancelled(
                new OrderCancelledEvent(UUID.randomUUID().toString(), order.getId(), order.getOrderCode(), event.reason())
        );
    }

    private Order getOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Order not found for saga: " + orderId));
    }
}
