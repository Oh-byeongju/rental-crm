package com.rental.domain.visit.event;

/**
 * 방문 배정 완료 이벤트 — 토픽 {@code rental.visit.assigned}.
 *
 * <p>페이로드 정본: {@code docs/04_기능 명세서.md §0-3}. 와이어 JSON = 본 record 필드.
 * 멱등키(메시지 key) = {@code visitId}.
 * Spring {@code ApplicationEvent} 이자 Kafka payload 로 그대로 사용 (계약 룰
 * {@code docs/global-rules/kafka-event-contract.md}).
 */
public record VisitAssignedEvent(Long visitId, Long engineerId, Long contractId) {}
