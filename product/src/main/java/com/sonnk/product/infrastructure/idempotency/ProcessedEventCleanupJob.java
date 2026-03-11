package com.sonnk.product.infrastructure.idempotency;

import com.sonnk.product.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduled cleanup cho bảng processed_events trong product service.
 *
 * Quy tắc:
 * - Giữ lịch sử idempotency tối đa 5 năm (đủ cho đa số yêu cầu audit/report).
 * - Mỗi đêm xoá mọi bản ghi có processedAt < now() - 5 năm.
 */
@Component
public class ProcessedEventCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(ProcessedEventCleanupJob.class);

    private final ProcessedEventRepository processedEventRepository;

    public ProcessedEventCleanupJob(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    /**
     * Chạy hàng ngày lúc 03:20 (khác vài phút so với order để tránh peak I/O cùng lúc).
     */
    @Scheduled(cron = "0 20 3 * * *")
    @Transactional
    public void cleanupOldProcessedEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusYears(5);
        var oldEvents = processedEventRepository.findByProcessedAtBefore(cutoff);
        if (oldEvents.isEmpty()) {
            return;
        }
        int size = oldEvents.size();
        processedEventRepository.deleteAll(oldEvents);
        log.info("[IDEMPOTENCY][CLEANUP][PRODUCT] Deleted {} processed_events older than {}", size, cutoff);
    }
}

