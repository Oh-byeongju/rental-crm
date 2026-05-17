package com.rental.batch.common.kafka;

import com.rental.domain.billing.event.BillingCreatedEvent;
import com.rental.domain.common.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * batch 발행분 — {@code rental.billing.created} (월 청구 배치 완료, 1 run = 1 event).
 *
 * <p>backoffice 의 {@code KafkaEventPublisher} 와 달리 {@code @TransactionalEventListener}
 * 미사용: batch 는 외부 tx 없음(ADR-014 R5). 호출 측이 strategy + BatchLogManager(REQUIRES_NEW)
 * commit 이 끝난 시점(= batch durable 완료)에만 호출하므로 phantom 없음. 실패 시 미발행.
 * 멱등키 = billingMonth (kafka-event-contract.md §3).
 *
 * <p>⚠️ complete 후 send 전 crash 시 유실 gap — outbox LATER (ADR-015).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillingEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishBillingCreated(String billingMonth, long count) {
        kafkaTemplate.send(KafkaTopics.BILLING_CREATED, billingMonth,
                        new BillingCreatedEvent(billingMonth, count))
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        log.error("[kafka-pub] FAILED topic={} key={} count={}",
                                KafkaTopics.BILLING_CREATED, billingMonth, count, ex);
                    } else {
                        log.info("[kafka-pub] sent topic={} key={} count={} offset={}",
                                KafkaTopics.BILLING_CREATED, billingMonth, count,
                                res.getRecordMetadata().offset());
                    }
                });
    }
}
