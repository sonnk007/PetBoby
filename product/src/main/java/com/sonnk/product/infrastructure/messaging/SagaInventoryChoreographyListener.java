package com.sonnk.product.infrastructure.messaging;

import com.sonnk.product.infrastructure.idempotency.IdempotentConsumerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Inventory choreography listener phía product service — production-grade với idempotency.
 *
 * Idempotency pattern:
 * - Mỗi handler kiểm tra (eventId, consumerGroup) trước khi xử lý.
 * - INSERT processed_events và nghiệp vụ (publishReserved/publishReserveFailed) cùng transaction.
 * - Race condition được chặn bởi unique constraint tại DB.
 *
 * Demo logic:
 * - branchCode chứa "FAIL-INV" → inventory reserve thất bại (publishReserveFailed).
 * - Còn lại → reserve thành công (publishReserved).
 * - Compensation "release": trong demo chỉ log; production sẽ cập nhật tồn kho thật + idempotency.
 */
@Component
public class SagaInventoryChoreographyListener {

    private static final Logger log = LoggerFactory.getLogger(SagaInventoryChoreographyListener.class);

    static final String CONSUMER_GROUP = "product-saga-inventory";

    private final SagaInventoryEventPublisher sagaInventoryEventPublisher;
    private final IdempotentConsumerHelper idempotentConsumerHelper;

    public SagaInventoryChoreographyListener(SagaInventoryEventPublisher sagaInventoryEventPublisher,
                                             IdempotentConsumerHelper idempotentConsumerHelper) {
        this.sagaInventoryEventPublisher = sagaInventoryEventPublisher;
        this.idempotentConsumerHelper = idempotentConsumerHelper;
    }

    /**
     * Nhận SagaOrderCreatedEvent → giả lập reserve inventory → phát kết quả.
     * Idempotency: cùng SagaOrderCreatedEvent.eventId chỉ reserve một lần.
     */
    @Transactional
    @KafkaListener(
            topics = SagaTopics.SAGA_ORDER_CREATED,
            groupId = CONSUMER_GROUP,
            properties = "spring.json.value.default.type=com.sonnk.product.infrastructure.messaging.SagaOrderCreatedPayload"
    )
    public void onSagaOrderCreated(SagaOrderCreatedPayload payload) {
        if (payload == null) return;
        if (!idempotentConsumerHelper.tryMarkProcessed(payload.eventId(), CONSUMER_GROUP, SagaTopics.SAGA_ORDER_CREATED)) {
            return;
        }

        boolean shouldFail = payload.branchCode() != null && payload.branchCode().contains("FAIL-INV");
        if (shouldFail) {
            log.warn("[SAGA][INVENTORY] Reserve failed orderId={} eventId={} branchCode={}",
                    payload.orderId(), payload.eventId(), payload.branchCode());
            sagaInventoryEventPublisher.publishReserveFailed(
                    new InventoryReserveFailedPayload(
                            UUID.randomUUID().toString(),
                            payload.orderId(),
                            payload.orderCode(),
                            payload.branchCode(),
                            "Simulated inventory reserve failure"
                    )
            );
            return;
        }

        log.info("[SAGA][INVENTORY] Reserve success orderId={} eventId={} branchCode={}",
                payload.orderId(), payload.eventId(), payload.branchCode());
        sagaInventoryEventPublisher.publishReserved(
                new InventoryReservedPayload(
                        UUID.randomUUID().toString(),
                        payload.orderId(),
                        payload.orderCode(),
                        payload.branchCode()
                )
        );
    }

    /**
     * Compensation: inventory release khi payment thất bại.
     * Idempotency: cùng InventoryReleaseRequestedEvent.eventId chỉ release một lần.
     * Production: cập nhật tồn kho thật trong transaction này.
     */
    @Transactional
    @KafkaListener(
            topics = SagaTopics.SAGA_INVENTORY_RELEASE_REQUESTED,
            groupId = CONSUMER_GROUP,
            properties = "spring.json.value.default.type=com.sonnk.product.infrastructure.messaging.InventoryReleaseRequestedPayload"
    )
    public void onInventoryReleaseRequested(InventoryReleaseRequestedPayload payload) {
        if (payload == null) return;
        if (!idempotentConsumerHelper.tryMarkProcessed(payload.eventId(), CONSUMER_GROUP, SagaTopics.SAGA_INVENTORY_RELEASE_REQUESTED)) {
            return;
        }

        // Demo: chỉ log. Production: UPDATE inventory SET reserved = reserved - qty WHERE orderId = ? + idempotency
        log.info("[SAGA][INVENTORY] Compensation release orderId={} eventId={} reason={}",
                payload.orderId(), payload.eventId(), payload.reason());
    }
}
