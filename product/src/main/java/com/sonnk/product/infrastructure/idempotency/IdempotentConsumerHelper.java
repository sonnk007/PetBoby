package com.sonnk.product.infrastructure.idempotency;

import com.sonnk.product.model.entity.ProcessedEvent;
import com.sonnk.product.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Helper idempotency dùng chung cho mọi Kafka consumer trong product service.
 *
 * PATTERN: INSERT processed_events và nghiệp vụ trong CÙNG transaction.
 * - Business fail → cả hai rollback → message retry đúng semantic at-least-once.
 * - Race condition → DataIntegrityViolationException từ unique constraint → skip an toàn.
 *
 * Xem thêm lý giải chi tiết tại: docs/kafka-data-integrity-and-deep-dive.md
 */
@Component
public class IdempotentConsumerHelper {

    private static final Logger log = LoggerFactory.getLogger(IdempotentConsumerHelper.class);

    private final ProcessedEventRepository processedEventRepository;

    public IdempotentConsumerHelper(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    /**
     * Kiểm tra và đánh dấu event đã xử lý (trong cùng transaction với caller).
     *
     * @return true  → chưa xử lý, tiếp tục nghiệp vụ.
     *         false → duplicate, bỏ qua.
     */
    @Transactional
    public boolean tryMarkProcessed(String eventId, String consumerGroup, String topic) {
        if (eventId == null || eventId.isBlank()) {
            log.warn("[IDEMPOTENCY] eventId null/blank for group={} topic={}", consumerGroup, topic);
            return true;
        }

        if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, consumerGroup)) {
            log.warn("[IDEMPOTENCY] Duplicate skipped eventId={} group={} topic={}",
                    eventId, consumerGroup, topic);
            return false;
        }

        try {
            processedEventRepository.save(ProcessedEvent.of(eventId, consumerGroup, topic));
            log.debug("[IDEMPOTENCY] Marked processed eventId={} group={}", eventId, consumerGroup);
            return true;
        } catch (DataIntegrityViolationException ex) {
            log.warn("[IDEMPOTENCY] Race condition duplicate eventId={} group={} — skipping",
                    eventId, consumerGroup);
            return false;
        }
    }
}
