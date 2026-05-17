package com.rental.domain.payment.event;

/**
 * 수납 완료 이벤트 — 토픽 {@code rental.payment.completed}.
 *
 * <p>페이로드 정본: {@code docs/04_기능 명세서.md §0-3}. 와이어 JSON = 본 record 필드.
 * 멱등키(메시지 key) = {@code billingId} (1 청구 : 1 수납 — Consumer dedup 단위).
 * Spring {@code ApplicationEvent} 이자 Kafka payload 로 그대로 사용 (계약 룰
 * {@code docs/global-rules/kafka-event-contract.md}).
 */
public record PaymentCompletedEvent(Long billingId, Long customerId, Long amount) {}
