package com.sonnk.product.model.entity;

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
 * Idempotent consumer table cho Kafka consumers phía product/inventory service.
 *
 * Cơ chế: INSERT (event_id, consumer_group) trong cùng transaction với nghiệp vụ.
 * Unique constraint là tường thành cuối cùng chặn race condition đa luồng.
 * Cần cleanup job xoá row cũ để tránh bảng phình theo thời gian.
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

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "consumer_group", nullable = false, length = 100)
    private String consumerGroup;

    @Column(nullable = false, length = 200)
    private String topic;

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
