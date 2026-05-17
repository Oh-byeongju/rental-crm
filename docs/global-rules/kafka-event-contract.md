---
description: "Kafka Producer / Consumer 작성·확장 시 — 토픽 명명·페이로드 계약·멱등키·발행 시점·offset 정책 (backoffice·batch·domain 공유 계약)"
---

# Kafka 이벤트 계약 룰 (전역)

> backoffice(producer+consumer) / batch(producer) / domain(payload record) 가 **공유하는 이벤트 계약**.
> 토픽 추가·페이로드 변경·Consumer 작성 시 본 룰을 따른다. 위반 = 모듈 간 직렬화/멱등 깨짐.
> 페이로드 **정본은 `docs/04_기능 명세서.md §0-3`** — 본 룰의 표는 구현 편의 사본, 충돌 시 04 우선.

---

## 1. 토픽 명명 — `rental.{domain}.{event}`

| 토픽 | 발행 시점 | 페이로드 | Consumer 처리 |
|---|---|---|---|
| `rental.billing.created` | 월 청구 배치 완료 (1 run = 1 event) | `{ billingMonth, count }` | 알림 INSERT |
| `rental.payment.completed` | 수납 처리 완료 | `{ billingId, customerId, amount }` | 연체 자동해제 + 알림 INSERT |
| `rental.payment.overdue` | 납기 초과 배치 | `{ billingId, customerId, overdueDays }` | 연체 상태변경 + 알림 INSERT |
| `rental.visit.assigned` | 방문 배정 완료 | `{ visitId, engineerId, contractId }` | 알림 INSERT |

- 토픽은 **자동 생성 금지** (`KAFKA_AUTO_CREATE_TOPICS_ENABLE=false`). 신규 토픽은 `infra/init-scripts/kafka/create-topics.sh` 에 `create_topic` 추가 후 컨테이너 재기동.
- 파티션 3 / replication 1 (개발). 운영 이행 시 재검토.

---

## 2. 페이로드 = `domain` 모듈 record

- 페이로드 타입은 `com.rental.domain.{domain}.event.*Event` **record** 로 둔다 (backoffice·batch 공유).
- JSON 직렬화 (`JsonSerializer`/`JsonDeserializer`). Consumer 의 `spring.json.trusted.packages` 에 payload 패키지(`com.rental.domain.*`) 가 **반드시 포함**돼야 역직렬화된다.
- record 는 Jackson 역직렬화를 위해 단일 canonical 생성자로 충분 (Spring Kafka `JsonDeserializer` 는 타입 헤더 기반 — `DashboardSummary` 의 `@JsonCreator` 케이스와 다름).

---

## 3. 멱등키 = 메시지 key (파티션 라우팅 단위)

`KafkaTemplate.send(topic, key, payload)` 의 **key = 멱등키**. 동일 key → 동일 파티션 → 단일 Consumer 스레드 직렬 처리 → check-then-act race 제거.

| 토픽 | 멱등키 (= 메시지 key, 모두 payload 내재) |
|---|---|
| `rental.payment.completed` | `billingId` (1 billing : 1 payment — 진짜 멱등 단위, payload 내재) |
| `rental.visit.assigned` | `visitId` |
| `rental.billing.created` | `billingMonth` |
| `rental.payment.overdue` | `billingId` |

> 멱등키는 **payload 에서 도출** (별도 이벤트 타입 불필요, 와이어 payload = 04 §0-3 정확 일치).
> `paymentNo` 가 아닌 `billingId` 인 이유: payload(`{billingId,customerId,amount}`)에 paymentNo 없음 +
> 한 청구는 1회만 수납(PaymentService 중복 차단) → billingId 가 멱등 단위. Consumer 도 billingId 로 dedup.

---

## 4. Producer 발행 시점 — 트랜잭션 커밋 후

- 도메인 변경과 같은 tx 안에서 `ApplicationEventPublisher` 로 **도메인 이벤트** 발행.
- `@TransactionalEventListener(phase = AFTER_COMMIT)` 가 받아 `KafkaTemplate` 발행.
- 근거: tx 롤백 시 phantom 이벤트 없음. `enable.idempotence=true` + `acks=all` (producer 중복/유실 방지).

```java
// Service — 도메인 변경과 동일 tx
events.publishEvent(new PaymentCompletedEvent(billingId, customerId, amount));

// 별도 컴포넌트 — 커밋 확정 후에만 브로커로
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void on(PaymentCompletedEvent e) {
    kafkaTemplate.send("rental.payment.completed", String.valueOf(e.billingId()), e);
}
```

> ⚠️ **알려진 gap**: commit 성공 후 send 전 프로세스 crash 시 이벤트 유실. 학습 단계 허용.
> 완전 보장은 **transactional outbox** — LATER (점진적 추상화, 별도 사이클).

---

## 5. Consumer — manual ack + 자연 멱등

- `enable-auto-commit: false` / `ack-mode: manual_immediate` (이미 `backoffice/application.yml`).
- 처리 **성공 후에만** `Acknowledgment.acknowledge()`. 실패 시 ack 안 함 → 재전송.
- 재전송 안전성은 **처리 효과의 자연 멱등성**으로 보장 (별도 inbox 테이블 없음):
  - 상태 전이형(연체 해제 등): DB 현재 상태 재조회 후 이미 처리됐으면 no-op (api-safety §2-1).
  - INSERT 형(알림): `existsBy(notificationType, refType, refId)` 가드 후 INSERT. **가드+INSERT 는 동일 `@Transactional`** (Oracle READ COMMITTED — commit 된 row 가시, api-safety §2-3/§5).
- `@KafkaListener` 비즈니스 예외는 삼키지 말 것(ack 안 되고 재전송돼야 함). 단 **독성 메시지 무한 재전송** 주의 — 학습 단계는 로깅 후 ack(skip) 허용, 운영은 DLT 검토(LATER).

```java
@KafkaListener(topics = "rental.payment.completed")
public void on(PaymentCompletedEvent e, Acknowledgment ack) {
    overdueService.resolveByPayment(e.billingId());                 // 자연 멱등 (no-op on repeat)
    notificationService.publishIfAbsent(PAYMENT_COMPLETED, ...);    // existsBy 가드 inside tx
    ack.acknowledge();                                              // 성공 후에만 offset commit
}
```

---

## 6. 새 이벤트 추가 절차

1. `docs/04_기능 명세서.md §0-3` 표에 행 추가 (정본).
2. `infra/init-scripts/kafka/create-topics.sh` 에 `create_topic` 추가 → 컨테이너 재기동.
3. `domain` 에 payload record 추가 (`com.rental.domain.{domain}.event`).
4. 본 룰 §1·§3 표 동기화.
5. Producer(§4 패턴) + Consumer(§5 패턴) 작성. Consumer 멱등키·자연 멱등 처리 명시.

---

## 관련 룰

- 서버측 검증 (상태 재조회·existsBy·tx 경계): [`backoffice/guide/conventions/api-safety.md`](../../backoffice/guide/conventions/api-safety.md)
- 페이로드 정본: `docs/04_기능 명세서.md §0-3`
- 통신 방식 결정 (REST→Kafka): [`docs/decisions/ADR-014`](../decisions/ADR-014-batch-module-split.md), `ADR-015`

---

## 변경 이력

- 2026-05-17: 신규 작성 — Ch.3 Kafka 도입 (ADR-015). 토픽 4 / AFTER_COMMIT 발행 / 자연 멱등 / manual ack.
