# BILLING_CREATE 측정 리포트 — R1 / R2 / R3

측정일: 2026-05-13 (4회차, ADR-014 Step 7-B + 7-C-1)
환경: Windows 10 Pro / Docker Desktop / Oracle 21c XE / Spring Boot 3.5 / Java 21
대상: 활성 계약 50,000 건 → 청구 50,000 건 INSERT

> 이전 리포트 [`2026-05-13-billing-create-r1-vs-r2.md`](2026-05-13-billing-create-r1-vs-r2.md) 를 본 리포트로 확장.

---

## 1. 결과 요약

| 라운드 | Strategy | DURATION_MS | rows/sec | BL_BATCH_LOG status | 비고 |
|---|---|---|---|---|---|
| **R1** | `single-save` (JPA 건별 `save()`) | **6,482 ms** | ~7,700 | COMPLETED ✅ | jdbc.batch_size=1000 으로 commit 시 자동 batch |
| **R2** | `chunk-flush` (1000건 chunk + flush+clear) | **21,543 ms** | ~2,320 | **RUNNING ❌** | em.clear 가 batchLog 도 detach (7-C-1 fix 적용 — 다음 측정부터 정상) |
| **R3** | `bulk-jdbc` (JdbcTemplate `batchUpdate`, JPA 우회) | **16,222 ms** | ~3,080 | COMPLETED ✅ | SEQ.NEXTVAL 5만회 호출이 비용 |

> R1 < R3 < R2 — **JPA 가 가장 빠름.** 의외의 두 번째 결과.

---

## 2. 핵심 분석

### 2-1. R2 가 R1 보다 느린 이유 (Step 7-B 발견 재기록)

`jdbc.batch_size=1000` 설정 ([application.yml](../../batch/src/main/resources/application.yml)) 으로 R1 의 commit 시점에 Hibernate 가 자동 batch INSERT. chunk flush 의 "INSERT batching" 가치 사라짐. R2 의 매 chunk `em.clear()` 오버헤드만 추가.

**chunk flush 의 진짜 가치 = 메모리 안정성** (1000만 행 같은 큰 규모). 5만 정도는 의미 X.

### 2-2. R3 가 R1 보다 느린 이유 — `SEQ.NEXTVAL` 호출 횟수

**핵심 발견.** [SEQ_BL_BILLING](../../infra/init-scripts/oracle/01-create-schema.sql) 정의:

```sql
INCREMENT_BY=50  CACHE_SIZE=0 (NOCACHE)
```

| 라운드 | NEXTVAL 호출 횟수 (5만건 기준) | 비용 |
|---|---|---|
| R1 (JPA, allocationSize=50) | 1,000 회 | 50건마다 1번. Hibernate 가 prefetch. |
| R3 (JdbcTemplate, SQL 안 NEXTVAL) | **50,000 회** | 매 INSERT 마다. |

- INCREMENT_BY=50 = Hibernate 의 `allocationSize=50` 호환 위해 설정 (한 번 NEXTVAL 시 50 번호 빌려옴)
- **CACHE_SIZE=0 (NOCACHE)** = NEXTVAL 호출 시 매번 disk write (`SYS.SEQ$` 갱신). cache 가 있으면 메모리 — 훨씬 빠름.
- R3 가 NOCACHE 시퀀스를 5만회 호출하면 그게 가장 큰 비용 — INSERT 자체보다 NEXTVAL round-trip 이 비쌈.

### 2-3. 학습 takeaway

> **"JPA 우회 = 무조건 빠르다" 는 거짓.** Hibernate 의 SEQUENCE allocation prefetch + jdbc.batch_size 결합이 사실 매우 강력. JPA 의 "추상화 오버헤드" 는 batch 시나리오에서 거의 없음.
>
> 진짜 빠른 bulk INSERT 패턴은:
> 1. **CACHE 가 충분히 큰 sequence** (CACHE 1000+)
> 2. **NEXTVAL 우회** (애플리케이션 측에서 ID 생성 — `IDENTITY` 컬럼 또는 UUID)
> 3. **INSERT INTO ... SELECT** 단일 SQL (Oracle direct path INSERT `/*+ APPEND */`)

R4 (chunk commit), R5 (UNDO 폭주), R6 (멱등성) 에서 추가 학습.

---

## 3. Step 7-C-1 적용 fix

| 버그 | 위치 | fix |
|---|---|---|
| `em.clear()` 가 batchLog detach → R2 status 미갱신 | `BillingCreateService.createMonthly` | strategy 호출 후 `batchLog = batchLogRepository.findById(batchLogId).orElseThrow()` 로 fresh reload |
| JPA `save(batchLog)` 가 영속성 컨텍스트만, JdbcTemplate (R3) 가 FK 참조 시 DB 미반영 → ORA-02291 | 같음 | `save` → `saveAndFlush` 로 즉시 INSERT |

> 둘 다 R3 첫 시도에서 ORA-02291 으로 막혔던 부수 발견. fix 후 R3 정상 동작.

---

## 4. R2 의 BL_BATCH_LOG RUNNING 잔재

이전 리포트 §3 에서 명시한 그대로. fix 적용 전 측정이라 BL_BATCH_LOG R2 행이 RUNNING / PROCESS_COUNT=0 으로 남음 (실 INSERT 5만건은 BL_BILLING 에 정상). 학습 자료로 보존.

다음 측정 (R4) 이 정상 STATUS 동작하는지로 fix 검증 가능.

---

## 5. R4~R6 진행 시 보정점 (Step 7-C-2 / 7-C-3)

1. **NEXTVAL 부담 측정 분리** — R3 의 16초 중 INSERT 자체 vs SEQ.NEXTVAL 비중을 분리하려면 NOCACHE → CACHE 1000 으로 alter 후 재측정 가치. 단 SEQ_BL_BILLING 만 alter 하면 다른 측정 baseline 깨질 수 있어 별도 strategy 비교만.
2. **R4 chunk commit** — 트랜잭션 분할. ROUND_NO 별 5만건 → 1000건 chunk 마다 commit. UNDO 부담 ↓. R3 의 NOCACHE NEXTVAL 비용은 여전.
3. **R5 UNDO 폭주** — Oracle `UNDO_TABLESPACE` 5MB 강제 → R1 재실행 시 ORA-30036 발생 예상. R4 (chunk commit) 와 비교하면 UNDO 차이 명확.
4. **R6 멱등성** — 동일 month 재실행 시 UNIQUE(CONTRACT_ID, BILLING_MONTH) 위반. 3 안 비교:
   - SKIP (Java try-catch + continue)
   - MERGE (Oracle MERGE INTO)
   - INSERT ... ON DUPLICATE 같은 우회 패턴

---

## 6. 측정 메타

| 항목 | 값 |
|---|---|
| 측정 환경 | Windows 10 Pro / Docker Desktop / Oracle 21c XE (gvenzl/oracle-free) |
| Spring Boot | 3.5 |
| Java | 21 |
| Hikari pool | maximum-pool-size=10 |
| Hibernate jdbc.batch_size | 1000 |
| order_inserts / order_updates | true |
| SEQ_BL_BILLING | INCREMENT_BY=50, CACHE_SIZE=0 (NOCACHE) |
| 대상 계약 수 | 50,000 (CT-PERF-*) |
| 청구월 분리 | R1=2026-01 / R2=2026-02 / R3=2026-03 |
| 트랜잭션 | BatchRunnerService `REQUIRES_NEW` + BillingCreateService `REQUIRED` (합류) |

---

## 7. 결론

- Step 7-B (R1/R2) + 7-C-1 (R3 + 버그 fix) 완료.
- **3 라운드 중 R1 (JPA) 이 가장 빠름 (6.5초).** JPA 의 보이지 않는 최적화 (allocation prefetch + commit-time batch) 의 위력.
- 의미 있는 bulk INSERT 차이는 SEQUENCE CACHE 설정 / IDENTITY 컬럼 / direct path INSERT 같은 다른 차원에서 나옴.
- 다음 (7-C-2): R4 chunk commit — 트랜잭션 경계 학습. UNDO 부담 사전 비교.
