# BILLING_CREATE 측정 리포트 — R5 (UNDO 폭주 + 거짓 COMPLETED 버그)

측정일: 2026-05-17 (ADR-014 Step 7-C-3)
환경: Windows 10 Pro / Docker Desktop / Oracle 21c XE / Spring Boot 3.5 / Java 21
대상: 활성 계약 50,000 건 → 청구 50,000 건 INSERT (single-save)

> 선행 리포트 [`2026-05-13-billing-create-r1-r2-r3-r4.md`](2026-05-13-billing-create-r1-r2-r3-r4.md) §4 "R5 — UNDO 폭주 재현" 의 실행.
> R5 의 본래 목적(작은 UNDO 에서 R1 실패 vs R4 정상)을 측정하던 중 **BL_BATCH_LOG 거짓 COMPLETED 버그**를 발견·수정하고, 수정을 런타임 재현으로 검증했다.

---

## 1. 결과 요약

| 항목 | 값 |
|---|---|
| UNDO 강제 | `undo_small` 2M, AUTOEXTEND OFF, XEPDB1 `undo_tablespace` 전환 (LOCAL_UNDO=TRUE) |
| single-save 결과 (수정 후) | **FAILED + ERROR_MSG(ORA-30036)**, BL_BILLING 0행 (정상 rollback) |
| 발견 버그 | single-save 가 ORA-30036 으로 rollback 됐는데 BL_BATCH_LOG 가 `COMPLETED / PROCESS=50000` 으로 거짓 기록 |
| 대조군 | chunk-commit(R4) 은 첫 chunk commit 에서 즉시 ORA-30036 포착 → 정확히 FAILED (버그 없음) |

---

## 2. 버그 증상

작은 UNDO + single-save 실행 시 (수정 전):

- batch 로그: `R1 done — target=50000 success=50000` 직후 `ORA-30036` → `JpaSystemException: could not execute batch [insert into bl_billing ...]` → 트랜잭션 rollback → 실제 BL_BILLING **0행**
- 그런데 BL_BATCH_LOG 행은 `BATCH_STATUS=COMPLETED, PROCESS_COUNT=50000, ERROR_MSG=null` 로 **거짓 기록**
- chunk-commit(R4) 은 동일 조건에서 BL_BATCH_LOG=FAILED 로 정확히 기록 → single-save 경로 한정 버그

핵심 단서: 로그 `R1 done — success=50000` 은 `BillingCreateService` 의 `complete()` **이후** 줄이다. 이 줄이 ORA-30036 **이전**에 찍힌다는 것은, `complete()`(COMPLETED commit) 가 BL_BILLING flush 보다 먼저 끝났다는 뜻.

---

## 3. 근본 원인

`BatchRunnerService.run()` 이 `@Transactional(propagation = REQUIRES_NEW)` 였다 (Step 5 에서 DUMMY 경로용으로 추가, Step 7-C-2 의 "외부 tx 없음" 설계와 미조정).

| 단계 | 동작 (수정 전) |
|---|---|
| run() | `@Transactional(REQUIRES_NEW)` → 외부 tx(TX_OUTER) 열림 (batch-runner 스레드) |
| BatchLogManager.start() | REQUIRES_NEW → BL_BATCH_LOG RUNNING INSERT, **자체 commit** |
| findAllActive() | TX_OUTER 합류 (read) |
| markTarget() | REQUIRES_NEW → target_count UPDATE, **자체 commit** |
| SingleSaveStrategy.execute() | `@Transactional`(기본 REQUIRED) → **TX_OUTER 에 합류** (자체 tx 아님). 5만 save() 큐잉, flush 안 됨, 50000 반환 |
| BatchLogManager.complete() | REQUIRES_NEW → BL_BATCH_LOG **COMPLETED 먼저 commit** ← 거짓 기록 |
| createMonthly() 리턴 | 정상 리턴 (예외 없음) |
| **run() proxy commit** | TX_OUTER commit → Hibernate flush → 5만 INSERT executeBatch → **ORA-30036** → TX_OUTER rollback → BL_BILLING 0행 |

`JpaSystemException` 은 **run() 의 commit 경계**(= `createMonthly()` 리턴 후, `complete()` commit 후)에서 발생 → `createMonthly` 의 `catch (RuntimeException e) → fail()` 이 **도달 불가**.

즉 프롬프트의 초기 추정("strategy 트랜잭션의 commit 경계")은 부정확했다. **이 경로에서 strategy 는 자체 tx 가 없다** — `run()` 의 REQUIRES_NEW tx 에 합류한다. 실패하는 commit 경계는 `run()` 의 것이다. 이것이 try/catch 가 못 잡는 이유이며, `BillingCreateService` Javadoc("R1/R2/R3 strategy = 각자 클래스 level @Transactional — 한 번 호출 = 한 tx") 과 ADR-014 §3-1("BillingCreateService @Transactional 제거 / 외부 tx 없음") 의 의도를 `run()` 의 `@Transactional` 이 조용히 위반한 상태였다.

chunk-commit(R4) 무사한 이유: `ChunkCommitStrategy` 는 `@Transactional` 없이 `TransactionTemplate(REQUIRES_NEW)` 로 chunk 마다 별 tx commit → 첫 chunk commit 에서 ORA-30036 이 `execute()` **안에서** 터져 `createMonthly` catch 로 잡힘.

---

## 4. 수정

### 4-1. 1차 (표면 surgical) — 검토 후 폐기

`SingleSaveStrategy` 만 `@Transactional(REQUIRES_NEW)` 로. strategy 가 자체 tx 를 commit/실패하게 해 ORA-30036 이 `execute()` 경계 안에서 터지도록. 런타임 검증 통과(BL_BATCH_LOG id=52 FAILED+ORA-30036). 그러나 R2/R3 와 불균일 + 근본 원인(run() 외부 tx) 잔존.

### 4-2. 2차 (근본 원인 — 채택)

ADR-014 §3-1 의도대로 **run() 의 외부 tx 자체를 제거**.

| 파일 | 변경 |
|---|---|
| `BatchRunnerService` | `@Transactional(REQUIRES_NEW)` 제거 (`@Async` 만 유지). run() 무 tx 진입 → strategy `@Transactional`(REQUIRED) 이 합류할 외부 tx 없음 → 각자 자체 tx. run() Javadoc 에 "절대 @Transactional 금지" 회귀 가드 명시 |
| `DummyBatchService` (신규) | DUMMY 경로는 save() 후 dirty-checking flush 에 자체 tx 필요. private 메서드 self-invocation 으로 `@Transactional` 이 안 먹으므로 별도 `@Service @Transactional` bean 으로 분리 |
| `SingleSaveStrategy` | 1차의 REQUIRES_NEW 를 plain `@Transactional` 로 **원복**. 외부 tx 가 없어졌으므로 REQUIRED 가 곧 자체 tx → R1/R2/R3 균일, `BillingCreateService` Javadoc 과 정확 일치 |

회귀 가드는 strategy 가 아니라 **진짜 위험 지점인 `run()`** 으로 이동.

---

## 5. 런타임 검증

수정 소스로 `:batch:bootJar` 빌드 → 별도 포트 9099 기동 (기존 9093 = 미수정 코드, 미간섭) → 검증 → 환경 원복.

| 검증 | 트리거 | BL_BATCH_LOG 결과 | 판정 |
|---|---|---|---|
| 1차 수정 single-save (작은 UNDO, 2026-09) | id=52 | `FAILED / PROC=0 / SUCC=0 / ERROR_MSG=ORA-30036('UNDO_SMALL')` , BL_BILLING 0행 | ✅ |
| **2차 수정 single-save** (작은 UNDO, 2026-10) | id=104 | `RUNNING(tgt=0)→RUNNING(tgt=50000)→FAILED / PROC=0 / SUCC=0 / ERROR_MSG=ORA-30036` , BL_BILLING 0행 | ✅ |
| DUMMY 회귀 (DummyBatchService tx) | id=102 / id=103 | `DUMMY_SUCCESS→COMPLETED(1/1/0)` / `DUMMY_FAIL→FAILED(1/1/0, 의도된 실패 msg)` | ✅ |

- `complete()` 미호출 증거 = PROCESS/SUCCESS_COUNT 0 (markTarget 의 TARGET_COUNT=50000 은 정상 — 대상은 실제 5만).
- DUMMY 행이 RUNNING/0 이 아니라 COMPLETED/FAILED + 카운터 = `DummyBatchService` 의 `@Transactional` + dirty-checking 정상 동작 (분리해도 회귀 없음).
- 검증 후 `undo_tablespace=UNDOTBS1` 복귀, `undo_small` DROP, 9099 종료, 임시 로그 삭제까지 완료. BL_BILLING 잔존 0.

---

## 6. 측정 메타

| 항목 | 값 |
|---|---|
| UNDO tablespace | `undo_small` `/opt/oracle/oradata/XE/XEPDB1/undo_small.dbf` SIZE 2M AUTOEXTEND OFF |
| LOCAL_UNDO_ENABLED | TRUE (XEPDB1 자체 undo — 전환이 PDB 한정) |
| 트리거 | `POST :9099/internal/batch/run/BILLING_CREATE` `{billingMonth, roundNo:5, strategy:single-save}` (fire-and-forget 202) |
| 대상 계약 수 | 50,000 (CT_CONTRACT ACTIVE) |
| 빈 청구월 | 2026-09 (1차), 2026-10 (2차) — UNIQUE 노이즈 회피 |
| DB 부수효과 | BL_BATCH_LOG 측정행 id 52·102·103·104 추가 (ADR-014 §측정 리포트 — DB 영속 정책). BL_BILLING 0행 |

---

## 7. 학습 takeaway

> **거짓 COMPLETED 의 본질은 "마킹 commit 과 데이터 commit 의 순서·경계 불일치".**
> marking service 를 REQUIRES_NEW 로 분리(Step 7-C-2)하면 실패 흔적 보존이라는 이득이 있지만,
> 데이터 commit 경계가 marking commit **뒤**로 밀리면 "데이터 rollback + 상태 COMPLETED" 가 성립한다.
> 마킹은 데이터 tx 가 **durable commit 된 뒤**에만 COMPLETED 여야 한다.
>
> 트랜잭션 경계는 어노테이션 한 줄(`run()` 의 `@Transactional`)로 의도와 정반대가 될 수 있다.
> propagation REQUIRED 는 "외부 tx 가 있으면 합류" — 외부 tx 의 **존재 자체**가 설계 가정을 깬다.
> 회귀 가드는 보정 지점(strategy)이 아니라 **위험을 만드는 지점(run())** 에 둔다.

---

## 8. 후속

- R5 본래 목적(작은 UNDO 에서 R1 실패 vs R4 정상 비교)은 버그 수정·검증으로 대체 측정됨 — single-save = ORA-30036 FAILED 확인. chunk-commit 정상 동작은 Step 7-C-2 에서 이미 확인.
- R6 멱등성(같은 month 재실행 UNIQUE 위반 3안 비교) 은 차기 sub-cycle.
- ADR-014 §후속 검토 사항에 "run() 비-tx 강제 = 청구 배치 tx 격리 전제" 반영 검토 (선택).
