package com.sonnk.order.application.port;

import com.sonnk.order.application.event.OrderCreatedEvent;

/**
 * Port (interface) publish event đơn hàng – Clean Architecture.
 * Implementation dùng Kafka (KafkaTemplate); có thể đổi sang RabbitMQ/AMQP mà không đổi application layer.
 *
 * Công dụng: tách dependency messaging; test dễ (mock publisher); đổi broker không ảnh hưởng use case.
 */
public interface OrderEventPublisher {

    /**
     * Gửi event đơn vừa tạo lên topic (Kafka). Consumer có thể log, cập nhật tồn kho, gửi thông báo, v.v.
     */
    void publishOrderCreated(OrderCreatedEvent event);
}
