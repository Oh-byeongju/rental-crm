package com.rental.backoffice.common.kafka;

import com.rental.domain.common.kafka.KafkaTopics;
import com.rental.domain.payment.event.PaymentCompletedEvent;
import com.rental.domain.visit.event.VisitAssignedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 도메인 이벤트 → Kafka 발행 (backoffice 발행분: 수납 완료 / 방문 배정).
 *
 * <p>계약 룰 {@code docs/global-rules/kafka-event-contract.md} §4:
 * Service 가 도메인 변경과 같은 tx 안에서 {@code ApplicationEventPublisher} 로 발행,
 * 본 컴포넌트가 {@code AFTER_COMMIT} 에서만 브로커로 전송 (롤백 시 phantom 없음).
 * 메시지 key = 멱등키(payload 내재) → 동일 key 동일 파티션 직렬 처리.
 *
 * <p>⚠️ commit 후 send 전 crash 시 유실 gap — transactional outbox 는 LATER (ADR-015).
 * billing.created 는 batch 모듈이 발행 (여기 없음).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentCompleted(PaymentCompletedEvent e) {
        send(KafkaTopics.PAYMENT_COMPLETED, String.valueOf(e.billingId()), e);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVisitAssigned(VisitAssignedEvent e) {
        send(KafkaTopics.VISIT_ASSIGNED, String.valueOf(e.visitId()), e);
    }

    private void send(String topic, String key, Object payload) {
        kafkaTemplate.send(topic, key, payload).whenComplete((res, ex) -> {
            if (ex != null) {
                // 유실 gap (ADR-015) — 학습 단계는 로깅. 운영은 outbox/재시도 LATER.
                log.error("[kafka-pub] FAILED topic={} key={} payload={}", topic, key, payload, ex);
            } else {
                log.info("[kafka-pub] sent topic={} key={} offset={}",
                        topic, key, res.getRecordMetadata().offset());
            }
        });
    }
}
