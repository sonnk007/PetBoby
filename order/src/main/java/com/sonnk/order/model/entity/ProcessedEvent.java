package com.sonnk.order.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Idempotent consumer table cho Saga choreography (và mọi Kafka consumer quan trọng).
 *
 * Cơ chế hoạt động:
 * - Mỗi khi consumer nhận một message có eventId, trước khi xử lý nghiệp vụ ta INSERT một row vào bảng này.
 * - Unique constraint (event_id, consumer_group) đảm bảo dù at-least-once delivery gửi trùng message,
 *   chỉ lần đầu tiên INSERT thành công; các lần sau sẽ bị DB reject → consumer bỏ qua.
 *
 * Lưu ý:
 * - INSERT processed_events và nghiệp vụ chính nằm trong CÙNG một transaction.
 *   Nếu nghiệp vụ fail → cả hai rollback → message sẽ được retry (at-least-once).
 * - Cần có cleanup job (cron/TTL) xoá row cũ để tránh table phình lớn theo thời gian.
 */
@Entity
@Table(
    name = "processed_events",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_processed_event_id_group",
        columnNames = {"event_id", "consumer_group"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * eventId từ payload Kafka (UUID). Kết hợp với consumerGroup tạo thành khoá duy nhất.
     */
    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    /**
     * consumer group xử lý event (vd: "order-saga-coordinator", "product-saga-inventory").
     * Cho phép cùng eventId được xử lý bởi nhiều consumer group khác nhau.
     */
    @Column(name = "consumer_group", nullable = false, length = 100)
    private String consumerGroup;

    /**
     * Topic Kafka nguồn — để tra cứu khi audit.
     */
    @Column(nullable = false, length = 200)
    private String topic;

    /**
     * Thời điểm xử lý. Dùng để TTL/cleanup sau này.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime processedAt;

    public static ProcessedEvent of(String eventId, String consumerGroup, String topic) {
        ProcessedEvent pe = new ProcessedEvent();
        pe.eventId = eventId;
        pe.consumerGroup = consumerGroup;
        pe.topic = topic;
        pe.processedAt = LocalDateTime.now();
        return pe;
    }
}
