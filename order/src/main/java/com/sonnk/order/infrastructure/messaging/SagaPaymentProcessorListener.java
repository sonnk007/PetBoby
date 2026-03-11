package com.sonnk.order.infrastructure.messaging;

import com.sonnk.order.application.event.saga.PaymentCompletedEvent;
import com.sonnk.order.application.event.saga.PaymentFailedEvent;
import com.sonnk.order.application.event.saga.PaymentRequestedEvent;
import com.sonnk.order.infrastructure.idempotency.IdempotentConsumerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Demo payment processor trong cùng service để hoàn thiện skeleton choreography.
 *
 * Idempotency: mỗi PaymentRequestedEvent.eventId chỉ được xử lý một lần cho consumer group này.
 * - Cùng eventId gửi lại (Kafka retry / at-least-once) → bị block tại idempotency check.
 *
 * Demo logic:
 * - branchCode chứa "FAIL-PAY" → payment failed.
 * - Còn lại → payment completed.
 *
 * Lưu ý: không phải payment gateway thật, không có giao dịch tài chính thật.
 */
@Component
public class SagaPaymentProcessorListener {

    private static final Logger log = LoggerFactory.getLogger(SagaPaymentProcessorListener.class);

    static final String CONSUMER_GROUP = "order-payment-processor-demo";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final IdempotentConsumerHelper idempotentConsumerHelper;

    public SagaPaymentProcessorListener(KafkaTemplate<String, Object> kafkaTemplate,
                                        IdempotentConsumerHelper idempotentConsumerHelper) {
        this.kafkaTemplate = kafkaTemplate;
        this.idempotentConsumerHelper = idempotentConsumerHelper;
    }

    @Transactional
    @KafkaListener(
            topics = SagaTopics.SAGA_PAYMENT_REQUESTED,
            groupId = CONSUMER_GROUP,
            properties = "spring.json.value.default.type=com.sonnk.order.application.event.saga.PaymentRequestedEvent"
    )
    public void onPaymentRequested(PaymentRequestedEvent event) {
        if (event == null) return;
        if (!idempotentConsumerHelper.tryMarkProcessed(event.eventId(), CONSUMER_GROUP, SagaTopics.SAGA_PAYMENT_REQUESTED)) {
            return;
        }

        boolean shouldFail = event.branchCode() != null && event.branchCode().contains("FAIL-PAY");
        if (shouldFail) {
            log.warn("[SAGA][PAYMENT] Simulate failed orderId={} eventId={} branchCode={}",
                    event.orderId(), event.eventId(), event.branchCode());
            kafkaTemplate.send(
                    SagaTopics.SAGA_PAYMENT_FAILED,
                    event.orderId().toString(),
                    new PaymentFailedEvent(UUID.randomUUID().toString(), event.orderId(), event.orderCode(), "Simulated payment failure")
            );
            return;
        }

        String txRef = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[SAGA][PAYMENT] Simulate completed orderId={} eventId={} txRef={}",
                event.orderId(), event.eventId(), txRef);
        kafkaTemplate.send(
                SagaTopics.SAGA_PAYMENT_COMPLETED,
                event.orderId().toString(),
                new PaymentCompletedEvent(UUID.randomUUID().toString(), event.orderId(), event.orderCode(), txRef)
        );
    }
}
