package com.sonnk.order.infrastructure.messaging;

import com.sonnk.order.application.event.saga.InventoryReleaseRequestedEvent;
import com.sonnk.order.application.event.saga.OrderCancelledEvent;
import com.sonnk.order.application.event.saga.OrderConfirmedEvent;
import com.sonnk.order.application.event.saga.PaymentRequestedEvent;
import com.sonnk.order.application.event.saga.SagaOrderCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Demo publisher cho Saga choreography.
 *
 * Cong dung:
 * - Gom cac publish event lien quan saga o mot noi de de theo doi.
 * - Skeleton hoc tap: chua bao gom outbox, retry policy, va schema versioning day du.
 */
@Component
public class SagaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SagaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishSagaStarted(SagaOrderCreatedEvent event) {
        kafkaTemplate.send(SagaTopics.SAGA_ORDER_CREATED, event.orderId().toString(), event);
    }

    public void publishPaymentRequested(PaymentRequestedEvent event) {
        kafkaTemplate.send(SagaTopics.SAGA_PAYMENT_REQUESTED, event.orderId().toString(), event);
    }

    public void publishInventoryReleaseRequested(InventoryReleaseRequestedEvent event) {
        kafkaTemplate.send(SagaTopics.SAGA_INVENTORY_RELEASE_REQUESTED, event.orderId().toString(), event);
    }

    public void publishOrderConfirmed(OrderConfirmedEvent event) {
        kafkaTemplate.send(SagaTopics.SAGA_ORDER_CONFIRMED, event.orderId().toString(), event);
    }

    public void publishOrderCancelled(OrderCancelledEvent event) {
        kafkaTemplate.send(SagaTopics.SAGA_ORDER_CANCELLED, event.orderId().toString(), event);
    }
}
