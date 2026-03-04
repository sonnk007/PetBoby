package com.sonnk.product.infrastructure.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer Kafka topic "order.created" (Tuần 4 – Event-driven intro).
 *
 * Công dụng: Product service subscribe event từ Order service mà không cần gọi HTTP.
 * Log request/message kèm topic, partition, offset, key để trace và debug (xem docs/kafka-operations.md).
 * Consumer group "product-service": mỗi message chỉ 1 instance trong group xử lý; scale bằng cách thêm instance.
 */
@Component
public class OrderCreatedKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedKafkaListener.class);

    public static final String TOPIC_ORDER_CREATED = "order.created";

    @KafkaListener(topics = TOPIC_ORDER_CREATED, groupId = "product-service")
    public void onOrderCreated(ConsumerRecord<String, OrderCreatedPayload> record) {
        OrderCreatedPayload payload = record.value();
        if (payload == null) return;

        // Log request/message: topic, partition, offset, key, eventId (idempotency key – xem docs/kafka-data-integrity-and-deep-dive.md), payload summary.
        log.info("[KAFKA-CONSUMER] Received OrderCreated topic={} partition={} offset={} key={} eventId={} | orderId={} orderCode={} totalAmount={}",
                record.topic(), record.partition(), record.offset(), record.key(), payload.eventId(),
                payload.orderId(), payload.orderCode(), payload.totalAmount());
        // Có thể mở rộng: gọi service cập nhật tồn kho, ghi audit, gửi notification...
    }
}
