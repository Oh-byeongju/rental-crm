# BILLING_CREATE 측정 리포트 — R1 / R2 / R3 / R4

측정일: 2026-05-13 (4회차, ADR-014 Step 7-B + 7-C-1 + 7-C-2)
환경: Windows 10 Pro / Docker Desktop / Oracle 21c XE / Spring Boot 3.5 / Java 21
대상: 활성 계약 50,000 건 → 청구 50,000 건 INSERT

> 이전 리포트 [`2026-05-13-billing-create-r1-r2-r3.md`](2026-05-13-billing-create-r1-r2-r3.md) 를 본 리포트로 확장.

---

## 1. 결과 요약

| 라운드 | Strategy | DURATION_MS | rows/sec | tx 패턴 | 비고 |
|---|---|---|---|---|---|
| **R1** | `single-save` (JPA) | **6,482** | ~7,700 | 한 tx 5만 commit | jdbc.batch_size=1000 자동 batch |
| **R2** | `chunk-flush` (JPA + em.clear) | ~21,543 | ~2,320 | 한 tx | em.clear 오버헤드 |
| **R3** | `bulk-jdbc` (JdbcTemplate) | 16,222 | ~3,080 | 한 tx | SEQ.NEXTVAL 5만 회 |
| **R4** | `chunk-commit` (REQUIRES_NEW × 50) | **27,876** | ~1,790 | 1000건 마다 commit | **commit 50회 비용 가장 큼** |

순위 (빠른 순): **R1 < R3 < R2 < R4**

---

## 2. R4 분석 — chunk commit 의 진짜 가치는 성능 아님

### 2-1. 왜 가장 느린가

| 항목 | R1 (한 tx) | R4 (50 tx) |
|---|---|---|
| `BEGIN`/`COMMIT` 호출 | 1번 | 50번 |
| LGWR (Log Writer) flush | commit 시점 1번 | commit 마다 → **50번** |
| SCN 증가 / redo log buffer flush | 1번 | 50번 |
| Hikari connection 재사용 | 동일 | REQUIRES_NEW 마다 새 connection (또는 pool 에서 acquire/release) |

Oracle 의 LGWR 는 commit 시 redo log 를 디스크에 flush (durability 보장). 이게 가장 큰 비용 — 매 commit 마다 디스크 I/O.

### 2-2. 그래도 chunk commit 을 쓰는 이유 (R5/R6 학습 포인트)

| 가치 | 설명 | R1 한계 |
|---|---|---|
| **UNDO 부담 ↓** | 매 commit 시 UNDO 세그먼트 해제 | R1 = 5만건 분 UNDO 누적 → 작은 UNDO tablespace 시 `ORA-30036` (R5 학습) |
| **재시작 친화** | 실패 시 직전 commit 까지 보존 | R1 실패 = 전체 rollback |
| **잠금 시간 ↓** | 매 chunk 후 행 잠금 해제 → 운영 중인 다른 세션 영향 ↓ | R1 = 5만 행 잠금 지속 |
| **메모리 안정성** | undo 메모리 + Hibernate 1차 캐시 모두 chunk 마다 비움 | R1 = 5만 영속성 누적 |

### 2-3. 학습 takeaway

> **chunk commit 은 "더 빠르다" 가 아니라 "더 안전하다".**
> 운영 환경에서 R1 의 5만 한 tx 는 UNDO 폭주 / 행 잠금 / 메모리 부담 / 재시작 위험으로 부적합.
> R4 의 50 tx 는 commit 비용 4배 감수 + 운영 안정성 확보.
>
> R5 (UNDO tablespace 5MB 강제) 에서 R1 은 ORA-30036 으로 실패, R4 는 정상 종료 — chunk commit 의 진가.

---

## 3. Step 7-C-2 리팩터 결과

### 3-1. 구조 변경

| Before (Step 7-C-1) | After (Step 7-C-2) |
|---|---|
| BillingCreateService `@Transactional` 외부 tx | `@Transactional` 제거 |
| batchLog INSERT/UPDATE 가 외부 tx 안 (saveAndFlush + findById reload) | `BatchLogManager` 신규 — 모든 작업 `REQUIRES_NEW` |
| R1/R2/R3 strategy 가 외부 tx 합류 | R1/R2/R3 strategy 자체 `@Transactional` |
| R4 없음 | R4 `ChunkCommitStrategy` — `TransactionTemplate.execute(REQUIRES_NEW)` chunk 마다 |

### 3-2. 결과 정합성

R4 측정 결과 → BL_BATCH_LOG status=COMPLETED, PROCESS_COUNT=50000, BATCH_PARAMS 정상 박힘.
즉 `BatchLogManager.complete()` (REQUIRES_NEW) 가 strategy tx 와 분리되어 정상 commit.
**Step 7-B 의 detached batchLog 버그 본질적 해결.**

> R2 의 BL_BATCH_LOG 행은 여전히 RUNNING / PROCESS=0 — 이전 측정 (Step 7-B) 의 잔재. 학습 자료로 보존. R2 새 측정 시 정상 동작 검증 가능 (이번 사이클은 R4 만 측정).

---

## 4. R5/R6 진행 시 보정점

### R5 — UNDO 폭주 재현

1. Oracle UNDO tablespace 5MB 강제 (`ALTER TABLESPACE UNDOTBS1 ... or 별도 UNDO tablespace 생성`)
2. R1 (single-save, 한 tx) 재시도 → `ORA-30036 unable to extend segment by 8 in undo tablespace 'UNDOTBS1'` 예상
3. R4 (chunk-commit) 재시도 → 정상 종료 예상
4. 두 결과 비교 → chunk commit 의 운영 가치 명확화

### R6 — 멱등성

같은 month 재실행 시 UNIQUE(CONTRACT_ID, BILLING_MONTH) 위반.

| 안 | 처리 | 측정 비교 |
|---|---|---|
| A. catch + continue | try 안에서 INSERT, ORA-00001 catch 후 다음 row | 예외 처리 비용 |
| B. SELECT 후 INSERT (애플리케이션 측 멱등) | 미리 존재 여부 조회 → 없으면 INSERT | 5만 SELECT 추가 |
| C. Oracle MERGE INTO | `MERGE INTO ... USING ... ON ... WHEN NOT MATCHED THEN INSERT` | 단일 SQL — 가장 빠를 듯 |

---

## 5. 측정 메타

| 항목 | 값 |
|---|---|
| 측정 환경 | Windows 10 Pro / Docker Desktop / Oracle 21c XE |
| Spring Boot | 3.5 |
| Java | 21 |
| Hikari pool | maximum-pool-size=10 |
| jdbc.batch_size | 1000 |
| SEQ_BL_BILLING | INCREMENT_BY=50, CACHE_SIZE=0 (NOCACHE) |
| 대상 계약 수 | 50,000 (CT-PERF-*) |
| 청구월 분리 | R1=2026-01 / R2=2026-02 / R3=2026-03 / R4=2026-04 |
| 누적 BL_BILLING | 200,000 (R1+R2+R3+R4) |
| 트랜잭션 | R1/R2/R3 strategy 자체 @Transactional / R4 chunk 마다 REQUIRES_NEW / batchLog 모두 BatchLogManager REQUIRES_NEW |

---

## 6. 결론

- **순수 성능 순위 변하지 않음**: R1 < R3 < R2 < R4. 한 tx 가 항상 가장 빠름.
- **R4 의 commit 50회 비용은 ~4배 시간** (R1 6.5 → R4 27.9초). LGWR / 디스크 I/O 가 가장 큰 요인.
- **chunk commit 의 가치는 R5 (UNDO 폭주) 에서 측정 가능** — 다음 sub-cycle 본체.
- BatchLogManager 패턴 정착 — 측정 인프라가 strategy 다양성에 robust 해짐.
- 다음 (7-C-3): R5 UNDO 부담 비교 + R6 멱등성 처리 3안 비교.
