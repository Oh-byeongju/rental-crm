# BILLING_CREATE 측정 리포트 — R1 vs R2

측정일: 2026-05-13 (4회차, ADR-014 Step 7-B)
환경: Windows 10 Pro / Docker Desktop / Oracle 21c XE / Spring Boot 3.5 / Java 21
대상: 활성 계약 50,000 건 → 청구 50,000 건 INSERT

---

## 1. 결과 요약

| 라운드 | Strategy | DURATION_MS | rows/sec (대략) | BL_BATCH_LOG status | BL_BILLING INSERT 결과 |
|---|---|---|---|---|---|
| **R1** | `single-save` (건별 `save()`, flush/clear 안 함) | **6,482 ms** | ~7,700 | COMPLETED ✅ | 50,000 |
| **R2** | `chunk-flush` (1000건마다 `em.flush()+clear()`) | **21,543 ms** | ~2,320 | **RUNNING ❌** (버그) | 50,000 |

> R2 의 21.5초는 batch 모듈 로그 (`BillingCreateService` `[billing-create] R2 done duration=21543ms`) 가 진실값.
> DB `BL_BATCH_LOG` 의 R2 행은 INSERT 시점의 초기 상태 (RUNNING, PROCESS=0, DURATION=NULL) 로 남음 — §3 버그 참조.

---

## 2. 의외의 결과 — R2 가 R1 보다 3.3배 느림

### 2-1. 가설 (측정 전)

| | 예상 |
|---|---|
| R1 | 5만 청구 1차 캐시 누적 → 메모리 부담 ↑, 마지막 commit 시점에 flush — 가장 느릴 것 |
| R2 | 1000건 단위 flush + clear → 메모리 안정 + batch INSERT → R1 보다 빠를 것 |

### 2-2. 실제

R2 가 R1 의 **3.3배 시간**. 가설 반대.

### 2-3. 원인 분석

핵심 = **`spring.jpa.properties.hibernate.jdbc.batch_size=1000` + `order_inserts=true` 설정** ([backoffice/batch application.yml](../../batch/src/main/resources/application.yml)).

- R1: 5만건 `save()` 시 INSERT SQL 즉시 발생 X. 트랜잭션 commit 시점에 Hibernate 가 1차 캐시의 5만 청구를 batch INSERT (1000건씩 50회 batch). **JDBC batch 라 single-save 도 자동 bulk**.
- R2: 매 1000 건마다 flush. Hibernate batching = chunk 크기와 일치 (1000). round-trip 횟수도 R1 의 commit-time batching 과 동일 (50회).
- 그럼 R2 가 왜 3.3배 느린가?
  - `em.clear()` 호출 50회 — 1차 캐시 전체 비우기 비용
  - flush() 마다 SQL 생성/dispatch — R1 의 commit 시점 한방 처리보다 overhead ↑
  - JDBC 트랜잭션 동기화 — flush 시 connection 상태 동기화 비용

### 2-4. 학습 takeaway

> **`jdbc.batch_size` 가 켜져 있으면 chunk flush 의 성능 이득이 사라진다.**
> chunk flush 의 진짜 가치는 **메모리** (5만 청구 객체 누적 회피). 5만 정도는 JVM heap 에 영향 X → R1 이 더 빠름.
> 메모리가 진짜 문제되는 규모 (수십~수백만 행) 부터 chunk flush 의미가 있음.

R3 (진짜 bulk INSERT — JDBC `PreparedStatement.addBatch()` + `executeBatch()` 직접) 또는 R4 (chunk commit — 트랜잭션 분할) 가 의미 있는 비교 대상.

---

## 3. 버그 발견 — `em.clear()` 가 batchLog 도 detach

### 3-1. 증상

R2 실행 후 BL_BATCH_LOG:
```
BATCH_LOG_ID  ROUND_NO  BATCH_STATUS  TARGET_COUNT  PROCESS_COUNT  DURATION_MS
3             1         COMPLETED     50000         50000          6482       <- R1 OK
4             2         RUNNING       50000         0              (null)     <- R2 status 미갱신
```

근데 BL_BILLING 의 BILLING_MONTH='2026-02' 행은 정확히 50,000 건 INSERT 됨. **데이터는 정상이지만 메타만 안 박힘**.

### 3-2. 원인

[ChunkFlushClearStrategy](../../batch/src/main/java/com/rental/batch/billing/strategy/ChunkFlushClearStrategy.java) 가 매 chunk 후 `entityManager.clear()` 호출 → **영속성 컨텍스트의 모든 entity 가 detached**. `BillingCreateService` 가 strategy 호출 전에 `batchLogRepository.save(...)` 로 INSERT 한 batchLog 객체도 같이 detach.

이후 strategy 종료 후 `batchLog.markCompleted()` / `batchLog.setCounters(...)` 호출 — detached entity 에 대한 작업이라 Hibernate dirty checking 동작 X → 트랜잭션 commit 시점에 UPDATE SQL 생성 안 됨.

### 3-3. 해결안 (Step 7-C 진입 시 수정)

옵션 비교:

| 안 | 코드 | 트레이드오프 |
|---|---|---|
| **A** | strategy 호출 후 `batchLog = batchLogRepository.findById(batchLogId).orElseThrow()` 로 fresh 로드 | 가장 단순. 1 SELECT 추가. **추천** |
| B | strategy 호출 후 `batchLog = batchLogRepository.save(batchLog)` (merge) | findById 와 효과 동일하지만 의도 흐림 |
| C | Strategy 가 `em.clear()` 안 함, 청구만 selective detach | Hibernate 표준 API 없음 — 우회 복잡 |
| D | batchLog 갱신을 별도 service + REQUIRES_NEW tx 로 분리 | 트랜잭션 경계 학습 가치 ↑, 코드 복잡도 ↑. Step 7-C 의 marking service 분리와 합쳐서 진행 |

권장 = A (Step 7-C 시작 시점), 또는 D (marking service 패턴 + 실패 흔적 보존도 같이 해결)

---

## 4. 측정 메타

| 항목 | 값 |
|---|---|
| 측정 환경 | Windows 10 Pro / Docker Desktop / Oracle 21c XE (gvenzl/oracle-free) |
| Spring Boot | 3.5 |
| Java | 21 (D:/Dev/JDK/openjdk-21+35) |
| Hikari pool | maximum-pool-size=10, batch 모듈도 별도 pool |
| Hibernate jdbc.batch_size | 1000 |
| order_inserts / order_updates | true |
| 대상 계약 수 | 50,000 (CT-PERF-* / `04-seed-perf-data.sql`) |
| 청구 단위 | 1 contract → 1 billing (1:1, 같은 month) |
| 호출 경로 | 화면 →(RestClient)→ batch `/internal/batch/run/BILLING_CREATE` → @Async → BillingCreateService |
| 트랜잭션 | BatchRunnerService `REQUIRES_NEW` + BillingCreateService `REQUIRED` (합류) |

---

## 5. R3~R6 진행 시 보정점 (Step 7-C)

1. **buggy markCompleted 먼저 fix** (위 §3-3 A 안). 안 그러면 R3 도 status 안 박힘.
2. R3 (real bulk INSERT) — JPA save 대신 JdbcTemplate `batchUpdate()` 또는 `PreparedStatement.addBatch()` 직접. R1 의 6.5초 보다 빨라야 의미 있음.
3. R4 (chunk commit) — `@Transactional` 분리 + chunk size 별 트랜잭션. UNDO 부담 비교 (R5 의 사전 작업).
4. R5 (UNDO 폭주) — Oracle UNDO tablespace 작게 강제 (예: 5MB) → R3/R4 재실행 → ORA-30036 재현.
5. R6 (멱등성) — 같은 `billingMonth` 재실행 → UNIQUE (CONTRACT_ID, BILLING_MONTH) 위반 처리. SKIP / MERGE / catch+continue 3 안 비교.

---

## 6. 결론

- Step 7-B 측정 완료. R1 6.5초 / R2 21.5초.
- **chunk flush 가 단순히 빠르지 않다** — JDBC batch 와 결합 시 R1 도 자동 bulk. 메모리 안정성이 chunk flush 의 진짜 가치.
- detached batchLog 버그 발견 — Step 7-C 진입 시 §3-3 A 안으로 fix.
- 다음: Step 7-C R3 (real bulk) — chunk flush 의 의미 회수 또는 R5 (UNDO) 학습의 사전 작업.
