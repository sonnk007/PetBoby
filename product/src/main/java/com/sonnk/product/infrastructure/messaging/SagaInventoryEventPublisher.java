package com.sonnk.product.infrastructure.messaging;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publisher cho cac inventory event trong demo Saga choreography.
 */
@Component
public class SagaInventoryEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SagaInventoryEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishReserved(InventoryReservedPayload event) {
        kafkaTemplate.send(SagaTopics.SAGA_INVENTORY_RESERVED, event.orderId().toString(), event);
    }

    public void publishReserveFailed(InventoryReserveFailedPayload event) {
        kafkaTemplate.send(SagaTopics.SAGA_INVENTORY_FAILED, event.orderId().toString(), event);
    }
}
