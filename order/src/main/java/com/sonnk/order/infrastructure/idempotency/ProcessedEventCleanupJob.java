package com.sonnk.order.infrastructure.idempotency;

import com.sonnk.order.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduled cleanup cho bảng processed_events trong order service.
 *
 * Mục tiêu:
 * - Tránh bảng processed_events phình quá lớn theo thời gian.
 * - Giữ lại lịch sử idempotency trong 5 năm (phù hợp use case tài chính / audit dài hạn),
 *   xoá mọi bản ghi có processedAt < now() - 5 năm.
 *
 * Lưu ý:
 * - Cron chạy mỗi đêm lúc 03:15 (giờ server).
 * - Nếu traffic cực lớn, có thể chia nhỏ theo batch hoặc thêm index trên processedAt (đã có).
 */
@Component
public class ProcessedEventCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(ProcessedEventCleanupJob.class);

    private final ProcessedEventRepository processedEventRepository;

    public ProcessedEventCleanupJob(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    /**
     * Chạy hàng ngày lúc 03:15.
     * Xoá mọi processed_events có processedAt < now() - 5 năm.
     */
    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void cleanupOldProcessedEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusYears(5);
        var oldEvents = processedEventRepository.findByProcessedAtBefore(cutoff);
        if (oldEvents.isEmpty()) {
            return;
        }
        int size = oldEvents.size();
        processedEventRepository.deleteAll(oldEvents);
        log.info("[IDEMPOTENCY][CLEANUP] Deleted {} processed_events older than {}", size, cutoff);
    }
}

