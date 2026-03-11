package com.sonnk.product.repository;

import com.sonnk.product.model.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

    /**
     * existsByEventIdAndConsumerGroup:
     * - By: điều kiện kép EventId AND ConsumerGroup.
     * - Trả về boolean (EXISTS): kiểm tra xem event đã được consumer group này xử lý chưa.
     */
    boolean existsByEventIdAndConsumerGroup(String eventId, String consumerGroup);

    /**
     * findByProcessedAtBefore:
     * - Before: processedAt < cutoffDate → dùng cho cleanup job xoá row cũ.
     */
    List<ProcessedEvent> findByProcessedAtBefore(LocalDateTime cutoffDate);
}
