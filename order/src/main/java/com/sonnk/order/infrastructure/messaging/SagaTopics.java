package com.sonnk.order.infrastructure.messaging;

/**
 * Topic name cho demo Saga choreography.
 */
public final class SagaTopics {
    private SagaTopics() {
    }

    public static final String SAGA_ORDER_CREATED = "saga.order.created";
    public static final String SAGA_INVENTORY_RESERVED = "saga.inventory.reserved";
    public static final String SAGA_INVENTORY_FAILED = "saga.inventory.failed";
    public static final String SAGA_PAYMENT_REQUESTED = "saga.payment.requested";
    public static final String SAGA_PAYMENT_COMPLETED = "saga.payment.completed";
    public static final String SAGA_PAYMENT_FAILED = "saga.payment.failed";
    public static final String SAGA_INVENTORY_RELEASE_REQUESTED = "saga.inventory.release.requested";
    public static final String SAGA_ORDER_CONFIRMED = "saga.order.confirmed";
    public static final String SAGA_ORDER_CANCELLED = "saga.order.cancelled";
}
