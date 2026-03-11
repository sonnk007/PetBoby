package com.sonnk.order.repository;

import com.sonnk.order.model.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

    /**
     * existsByEventIdAndConsumerGroup:
     * - By: điều kiện kép: EventId AND ConsumerGroup
     * - Trả về boolean (SELECT EXISTS): kiểm tra xem event đã được consumer group này xử lý chưa.
     * - Dùng để chặn duplicate trước khi chạy nghiệp vụ (idempotency check).
     */
    boolean existsByEventIdAndConsumerGroup(String eventId, String consumerGroup);

    /**
     * findByProcessedAtBefore:
     * - Before: processedAt < cutoffDate
     * - Dùng cho cleanup job: lấy danh sách event cũ hơn ngưỡng để xoá, tránh bảng phình lớn.
     */
    List<ProcessedEvent> findByProcessedAtBefore(LocalDateTime cutoffDate);
}
