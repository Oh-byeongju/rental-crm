package com.rental.domain.billing.event;

/**
 * 월 청구 배치 완료 이벤트 — 토픽 {@code rental.billing.created}.
 *
 * <p>페이로드 정본: {@code docs/04_기능 명세서.md §0-3}. 와이어 JSON = 본 record 필드.
 * 1 배치 run = 1 이벤트 (건별 아님 — 5만 건이라도 단일 이벤트).
 * 멱등키(메시지 key) = {@code billingMonth}.
 * batch 모듈이 발행, backoffice Consumer 가 알림 INSERT (계약 룰
 * {@code docs/global-rules/kafka-event-contract.md}).
 */
public record BillingCreatedEvent(String billingMonth, long count) {}
