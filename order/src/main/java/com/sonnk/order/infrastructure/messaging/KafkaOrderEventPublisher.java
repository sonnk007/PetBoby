package com.sonnk.order.infrastructure.messaging;

import com.sonnk.order.application.event.OrderCreatedEvent;
import com.sonnk.order.application.port.OrderEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Implementation OrderEventPublisher bằng Kafka (Tuần 4 – Event-driven intro).
 *
 * Công dụng: Order service publish event bất đồng bộ; các service khác (product, user, notification) subscribe
 * mà không cần gọi HTTP. Topic + consumer group giúp scale consumer, at-least-once delivery.
 * Trade-off: eventual consistency; consumer phải xử lý duplicate (idempotency).
 */
@Component
public class KafkaOrderEventPublisher implements OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaOrderEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topicOrderCreated;

    public KafkaOrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                   @Value("${petboby.order.kafka.topic-order-created:order.created}") String topicOrderCreated) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicOrderCreated = topicOrderCreated;
    }

    @Override
    public void publishOrderCreated(OrderCreatedEvent event) {
        String key = event.orderId() != null ? event.orderId().toString() : event.orderCode();
        // Log request/message trước khi gửi (topic, key, payload summary) – trace và audit.
        log.info("[KAFKA-PRODUCER] Sending OrderCreatedEvent topic={} key={} orderId={} orderCode={} totalAmount={}",
                topicOrderCreated, key, event.orderId(), event.orderCode(), event.totalAmount());

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topicOrderCreated, key, event);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.warn("[KAFKA-PRODUCER] Failed to publish OrderCreatedEvent orderId={}: {}", event.orderId(), ex.getMessage());
            } else if (result != null && result.getRecordMetadata() != null) {
                var meta = result.getRecordMetadata();
                // Log sau khi gửi: partition, offset – để trace message đã vào partition/offset nào.
                log.info("[KAFKA-PRODUCER] Published OrderCreatedEvent orderId={} topic={} partition={} offset={}",
                        event.orderId(), meta.topic(), meta.partition(), meta.offset());
            }
        });
    }
}
