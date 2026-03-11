package com.sonnk.order.infrastructure.idempotency;

import com.sonnk.order.model.entity.ProcessedEvent;
import com.sonnk.order.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Helper idempotency dùng chung cho mọi Kafka consumer trong order service.
 *
 * PATTERN (production-grade):
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  @Transactional (listener)                                          │
 * │  ┌─────────────────────────┐                                        │
 * │  │ tryMarkProcessed()       │ ← INSERT processed_events             │
 * │  │ → false nếu đã tồn tại  │   (cùng transaction với nghiệp vụ)    │
 * │  └─────────────────────────┘                                        │
 * │  if false → return (skip)                                           │
 * │  ... business logic ...                                             │
 * │  COMMIT (processed_events + business changes cùng lúc)             │
 * └─────────────────────────────────────────────────────────────────────┘
 *
 * Tại sao cùng transaction?
 * - Nếu business logic fail → cả INSERT processed_events lẫn thay đổi nghiệp vụ đều rollback.
 * - Message sẽ được retry → đúng at-least-once semantic.
 * - Ngược lại nếu dùng REQUIRES_NEW: processed_events commit trước, business fail → message
 *   bị bỏ qua mãi mãi dù chưa xử lý xong → mất event.
 *
 * Race condition:
 * - Unique constraint (event_id, consumer_group) tại DB là tường thành cuối cùng.
 * - Hai consumer xử lý cùng lúc: một sẽ thắng INSERT, một sẽ bị DataIntegrityViolationException
 *   → transaction rollback → Kafka retry → lần sau sẽ thấy đã processed → skip an toàn.
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
     * @param eventId       UUID của event (lấy từ payload).
     * @param consumerGroup Tên consumer group (@KafkaListener groupId).
     * @param topic         Topic Kafka (dùng để audit).
     * @return true  → chưa xử lý, tiếp tục nghiệp vụ.
     *         false → đã xử lý rồi, bỏ qua (duplicate).
     */
    @Transactional
    public boolean tryMarkProcessed(String eventId, String consumerGroup, String topic) {
        if (eventId == null || eventId.isBlank()) {
            log.warn("[IDEMPOTENCY] eventId is null/blank for consumerGroup={} topic={} — skipping idempotency check",
                    consumerGroup, topic);
            return true;
        }

        if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, consumerGroup)) {
            log.warn("[IDEMPOTENCY] Duplicate event skipped eventId={} consumerGroup={} topic={}",
                    eventId, consumerGroup, topic);
            return false;
        }

        try {
            processedEventRepository.save(ProcessedEvent.of(eventId, consumerGroup, topic));
            log.debug("[IDEMPOTENCY] Marked processed eventId={} consumerGroup={}", eventId, consumerGroup);
            return true;
        } catch (DataIntegrityViolationException ex) {
            // Race condition: another consumer instance inserted the same row concurrently.
            log.warn("[IDEMPOTENCY] Concurrent duplicate detected eventId={} consumerGroup={} — skipping",
                    eventId, consumerGroup);
            return false;
        }
    }
}
