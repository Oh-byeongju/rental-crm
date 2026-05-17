package com.rental.domain.common.kafka;

/**
 * Kafka 토픽 상수 — backoffice(producer+consumer) / batch(producer) 공유 단일 출처.
 *
 * <p>토픽 추가는 {@code infra/init-scripts/kafka/create-topics.sh} +
 * 계약 룰 {@code docs/global-rules/kafka-event-contract.md} 동기. 정본 페이로드 = 04 §0-3.
 */
public final class KafkaTopics {

    private KafkaTopics() {}

    public static final String BILLING_CREATED   = "rental.billing.created";
    public static final String PAYMENT_COMPLETED = "rental.payment.completed";
    public static final String PAYMENT_OVERDUE   = "rental.payment.overdue";
    public static final String VISIT_ASSIGNED    = "rental.visit.assigned";
}
