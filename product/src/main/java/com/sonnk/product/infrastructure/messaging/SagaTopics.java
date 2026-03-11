package com.sonnk.product.infrastructure.messaging;

/**
 * Topic constants cho demo Saga choreography phia product/inventory.
 */
public final class SagaTopics {
    private SagaTopics() {
    }

    public static final String SAGA_ORDER_CREATED = "saga.order.created";
    public static final String SAGA_INVENTORY_RESERVED = "saga.inventory.reserved";
    public static final String SAGA_INVENTORY_FAILED = "saga.inventory.failed";
    public static final String SAGA_INVENTORY_RELEASE_REQUESTED = "saga.inventory.release.requested";
}
