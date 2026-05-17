# ADR-015 — Kafka 이벤트 드리븐 도입 (Ch.3 / Step 9)

상태: **Accepted** (2026-05-17)
영향 범위: backoffice(producer+consumer) / batch(producer) / domain(event payload) / infra 토픽 / 학습 시나리오 Ch.3

---

## 컨텍스트

ADR-014 §통신: "1차 REST fire-and-forget → Ch.3 Kafka 학습 후 일부 토픽". ADR-014 시나리오 표:
"수납·연체 Kafka (이벤트 기반) — Producer/Consumer + 멱등성 — Ch.3".

권위 스펙은 **이미 존재** — `docs/04_기능 명세서.md §0-3` (토픽 4 / 발행시점 / 페이로드 /
Consumer 처리), `infra/init-scripts/kafka/create-topics.sh` (토픽 4 기생성), 코드 breadcrumb
(`PaymentService` / `NotificationService` / `OverdueService`). 본 ADR 은 그 스펙의 **구현 방식**
(발행 신뢰성·멱등 메커니즘·offset·범위) 을 확정한다 (ADR-014 하위 결정).

---

## 검토한 대안

### 범위 — 무엇을 Kafka 화?

| 안 | 내용 | 판정 |
|---|---|---|
| A. REST fire-and-forget 유지 | 통신 변경 없음 | ❌ Ch.3 학습 본체가 Kafka |
| B. 배치 트리거 요청/응답 토픽화 | ADR-014 §통신의 backoffice↔batch 트리거를 Kafka 로 | ❌ ADR-014 §통신 "후순위" — Ch.3 학습 본체(이벤트 드리븐)와 다른 축. LATER |
| **C. 04 §0-3 도메인 이벤트 드리븐** ✅ | 4 토픽 중 산출지 준비된 3흐름 구현 | ✅ 채택 — 스펙·breadcrumb 정합, 학습 4요소 충족 |

채택 범위 (C):
- `rental.payment.completed` — backoffice 발행(수납 완료) → 연체 자동해제 + 알림
- `rental.visit.assigned` — backoffice 발행(방문 배정) → 알림
- `rental.billing.created` — **batch** 발행(월 청구 배치 완료) → backoffice 알림 (교차 모듈/양방향)
- `rental.payment.overdue` — **제외**. 산출 배치 OVERDUE_UPDATE 미구현 (ADR-014 "Ch.1 후속"
  별도 시나리오). producer·consumer 동반 작업이 옳음 — listener 단독 와이어링은 speculative
  dead code (CLAUDE.md §2). 계약 룰 §1 표가 forward-doc.
- `rental.payment.cancelled` — **계약 외**. 04 §0-3 에 없는 토픽(create-topics.sh 4개에 없음).
  `PaymentService.cancel` 의 미결 "청구 상태 복원 정책" 과 묶인 별도 사항. 코드 TODO 유지.

### 발행 신뢰성 — phantom/유실

| 안 | 판정 |
|---|---|
| tx 안에서 직접 send | ❌ 롤백 시 phantom 이벤트 |
| **도메인 이벤트 + @TransactionalEventListener(AFTER_COMMIT)** ✅ | ✅ 롤백 시 미발행. batch 는 외부 tx 없음 → durable complete() 후 직접 발행(실패 시 미발행) |
| transactional outbox | ⏳ LATER — commit→send 사이 crash 유실 gap 완전 차단. 점진적 추상화(별도 사이클) |

### 멱등 Consumer — 재전송(at-least-once) 안전

| 안 | 판정 |
|---|---|
| **자연 멱등 (신규 DDL 0)** ✅ | ✅ `resolveByPayment` no-op-on-repeat + `publishIfAbsent` existsBy(type,refType,refId) 가드(동일 tx, Oracle READ COMMITTED — api-safety §2-3). 메시지 key=멱등키(payload 내재) → 동일키 동일 파티션 직렬 → check-then-act race 제거. simplicity first + 취약 시드 Oracle DDL 회피 |
| 명시적 inbox 테이블 | ❌ 신규 DDL → ADR-004 수동 재적용 + R6 시드 5만행 리스크. 학습 명료성 이점은 자연 멱등 + 본 ADR 문서화로 대체 |
| Redis SETNX | ❌ Redis 휘발 → 영속 보장 약함(권한캐시처럼 보조용). inbox 보다 비정석 |

### offset 정책

manual ack (`enable-auto-commit:false`, `ack-mode:manual_immediate`) — 처리 **성공 후에만**
`Acknowledgment.acknowledge()`. 실패 시 미ack → 재전송. at-least-once + 자연 멱등 결합.
독성 메시지 무한 재전송 차단(DLT) = LATER (학습 단계는 로깅).

---

## 결정

- 범위 C (3흐름 구현, payment.overdue/cancelled 제외).
- 발행 = 도메인 이벤트 + AFTER_COMMIT(backoffice) / durable 후 직접(batch). outbox LATER.
- 멱등 = 자연 멱등, 신규 DDL 0.
- offset = manual ack 성공 후.
- 공유 계약 = `domain` 모듈에 event record 3 + `KafkaTopics` 상수 (backoffice·batch 단일 출처).
- 신규 전역 룰 `docs/global-rules/kafka-event-contract.md` (rule-management §1-3 — backoffice·batch·domain 동시 영향 → global).

### 산출물

| 모듈 | 신규/변경 |
|---|---|
| domain | `payment/visit/billing.event.*Event` record 3 + `common.kafka.KafkaTopics` + `Notification.TYPE_*` + `NotificationRepository.existsBy...` |
| backoffice | `common.kafka.KafkaEventPublisher`(AFTER_COMMIT) / `KafkaEventConsumer`(@KafkaListener 3 + manual ack) / `PaymentService`·`VisitService` 발행 / `NotificationService.publishIfAbsent`(멱등) / `OverdueService.resolveByPayment`(자연 멱등) |
| batch | `build.gradle` +spring-kafka / `application.yml` +producer / `common.kafka.BillingEventPublisher` / `BillingCreateService` 발행 |
| infra | 변경 없음 (토픽 4 기생성) |

---

## 근거

1. 스펙 우선 — 04 §0-3 + breadcrumb 가 WHAT 을 못박음. ADR 은 HOW 만 확정 (발명 회피).
2. AFTER_COMMIT — 학습 단계 적정 비용으로 phantom 차단. outbox 의 유실 gap 완전차단은 LATER (YAGNI/점진).
3. 자연 멱등 — Kafka at-least-once 의 정석 대응(효과를 멱등 설계). DDL 0 으로 취약 시드 Oracle 불가침 + simplicity first.
4. 단일 계약 출처(domain) — producer(backoffice·batch)/consumer(backoffice) 토픽·페이로드 drift 차단.

---

## 검증 (런타임, 2026-05-17 — 5컨테이너 가동, bootRun ×2)

- **payment.completed**: 수납 등록(billing 151002, 34000) → `[kafka-pub] sent key=151002 off=0` → `[kafka-con] recv/done` → CM_NOTIFICATION 1행(PAYMENT_COMPLETED, BILLING/151002, "PerfUser34 님의 2026-06 수납이 완료되었습니다.") + billing 151002 PAID. 연체 0 → resolveByPayment no-op.
- **billing.created (교차 모듈)**: BILLING_CREATE merge 2026-06 → batch JVM `[kafka-pub] key=2026-06 count=50000` → backoffice JVM `[kafka-con] recv/done` → CM_NOTIFICATION +1(BILLING_CREATED, BILLING_MONTH/202606, "2026-06 청구서 50000건이 생성되었습니다.").
- **멱등 (더블 트리거)**: 동일 배치 재트리거(log 202→203 둘 다 COMPLETED 50000, R6 멱등) → billing.created 2회 수신(23:06, 23:12 둘 다 recv/done) → **BILLING_CREATED 알림 1건 불변** (publishIfAbsent skip). NOTIF_TOTAL=2 고정.
- **한계**: `visit.assigned` 는 코드 완성 + 컨슈머 subscribed/partitions assigned(기동 로그) 확인되나 **활성 기사 시드 0건**(CT_ENGINEER USE_YN=Y=0)으로 런타임 미실행. 컨슈머 경로는 billing.created 와, 프로듀서 경로는 payment.completed 와 구조 동일.

---

## 후속 검토 사항

- transactional outbox (발행 유실 gap 완전차단) — LATER.
- DLT / 재시도 백오프 (독성 메시지) — LATER.
- payment.overdue + OVERDUE_UPDATE 배치 (producer+consumer 동반) — ADR-014 "Ch.1 후속" 시나리오.
- payment.cancelled + 청구 상태 복원 정책 — 04 §0-3 미정의. 별도 결정 필요.
- visit.assigned 런타임 검증 — 활성 기사 시드 추가 시.
- backoffice↔batch 트리거 자체의 Kafka 요청/응답 토픽화 (ADR-014 §통신 후순위) — 별도.
