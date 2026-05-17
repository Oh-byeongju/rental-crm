# BILLING_CREATE 측정 리포트 — R6 (멱등성 3안)

측정일: 2026-05-17 (ADR-014 Step 7-C-3)
환경: Windows 10 Pro / Docker Desktop / Oracle 21c XE / Spring Boot 3.5 / Java 21
대상: 활성 계약 50,000 건 → 청구 50,000 건이 **이미 존재**하는 month(2026-06) 재실행

> 선행 리포트 [`2026-05-13-billing-create-r1-r2-r3-r4.md`](2026-05-13-billing-create-r1-r2-r3-r4.md) §4 "R6 — 멱등성" 의 실행.
> R5(UNDO 폭주)는 [`2026-05-17-billing-create-r5-undo.md`](2026-05-17-billing-create-r5-undo.md) 에서 별도 측정.

---

## 0. 측정 시나리오

"같은 month 재실행" = 멱등성. 깨끗한 비교를 위해 **full overlap** 설계:

1. R1 `single-save` 로 month=2026-06 에 50,000 건 시드 (baseline)
2. R6-A / R6-B / R6-C 를 **같은 2026-06** 에 순차 실행 — 50,000 건 전부 `UK_BL_BILLING_CONTRACT_MONTH (CONTRACT_ID, BILLING_MONTH)` 충돌
3. 각 실행 후 `SELECT COUNT(*) WHERE BILLING_MONTH='2026-06'` 가 **정확히 50000 유지**인지로 멱등성 판정
   (= 신규 INSERT 0, 중복 0, 에러 0, BL_BATCH_LOG COMPLETED)

`BillingFactory` 의 `BILLING_NO = BL-{YYYYMM}-{CONTRACT_ID:010d}` 는 (CONTRACT_ID, BILLING_MONTH) 와
자연 1:1 → `UK_BL_BILLING_NO` 도 동시 충돌. 어느 쪽이 먼저 터지든 ORA-00001.

---

## 1. 결과 요약

| 라운드 | Strategy | DURATION_MS | 라운드트립 | 멱등 결과 | BL_BATCH_LOG |
|---|---|---|---|---|---|
| (시드) | `single-save` | 45,048 | 50,000 | 신규 50,000 INSERT | id=152 COMPLETED |
| **R6-A** | `catch-continue` | **296,352** | 50,000 | 신규 0 / 스킵 50,000 | id=153 COMPLETED 50000/0 |
| **R6-B** | `select-insert` | **306,313** | 50,000 | 신규 0 / 스킵 50,000 | id=154 COMPLETED 50000/0 |
| **R6-C** | `merge` | **8,967** | ~50 (batch 1000) | 신규 0 (WHEN NOT MATCHED) | id=155 COMPLETED 50000/0 |

- **3안 모두 멱등성 정확** — 매 실행 후 `COUNT(2026-06)=50000` 불변, STATUS=COMPLETED, success=50000 fail=0.
- 성능: **C(MERGE) 가 A/B 대비 ~33배 빠름** (8.9초 vs 296·306초).
- A ≈ B (둘 다 ~300초) — 예외 처리 유무가 사실상 무차이.

---

## 2. 핵심 발견 — 비용은 "충돌 처리 방식" 이 아니라 "라운드트립 수"

직관적 예상은 "A(예외 5만 회)가 B(사전 SELECT, 예외 0)보다 한참 느리다" 였다. **틀렸다.**

| | R6-A catch-continue | R6-B select-insert |
|---|---|---|
| per-row 동작 | INSERT 시도 → ORA-00001 → `DuplicateKeyException` catch | `existsByContractIdAndBillingMonth` 인덱스 조회 → 존재 → 스킵 |
| 예외 생성·변환 | 5만 회 | 0 |
| DB 라운드트립 | 5만 | 5만 |
| 결과 | **296,352 ms** | **306,313 ms** |

예외 변환 비용(SQLException → Spring DataAccessException, 스택트레이스 생성)이 5만 회나 있는 A 가
오히려 B 보다 근소하게 빨랐다. 즉 **두 경로 다 5만 회 동기 라운드트립 벽에 막혀** 있고,
예외 vs 사전조회 차이는 그 벽 위의 노이즈다.

C(MERGE)는 `JdbcTemplate.batchUpdate` + `jdbc.batch_size=1000` 로 라운드트립을 5만 → ~50 으로
접어 9초에 끝났다. R3(bulk-jdbc)에서 얻은 교훈("배칭이 본질")의 재확인이며, 멱등성 ON 절 매칭
오버헤드는 set-based 앞에서 무시할 수준임을 보여준다.

> **멱등성의 정확성은 3안 모두 동일. 차이는 전부 라운드트립 수에서 온다.**
> "예외 잡고 계속" vs "넣기 전에 확인" 은 5만 왕복이라는 같은 감옥. 탈출구는 set-based(MERGE) 뿐.

---

## 3. 구현 노트 — R6-A 가 JPA 가 아니라 JDBC 인 이유 (rollback-only 함정)

R6-A 의 가장 큰 학습 포인트는 측정값이 아니라 **naive JPA catch+continue 가 동작하지 않는다**는 사실:

- JPA/Hibernate 는 제약 위반이 한 번 터지면 영속성 컨텍스트 트랜잭션을 **rollback-only** 마킹.
  같은 tx 안에서 그 뒤 `save()` 를 try/catch 로 감싸도 flush/commit 단계에서 전체가 터진다.
  "건너뛰고 계속" 자체가 성립 안 함.
- raw JDBC 는 Oracle **statement-level rollback** — 실패한 INSERT 한 문장만 무효, 같은 tx 의
  직전 성공분/이후 문장은 생존. catch 한 예외가 `@Transactional` proxy 경계를 안 넘으므로
  Spring 이 tx 를 rollback-only 로 마킹하지도 않는다(Hibernate 세션 미접촉).
- → **단일 tx 안에서 per-row try/catch 가 진짜 동작**. R6-A 가 COMPLETED·count 50000 불변으로
  끝난 것이 이 설계의 런타임 검증이다(만약 rollback-only 였다면 commit 단계에서 터져 FAILED).

`CatchContinueStrategy` Javadoc 에 근거를 박았다. SEQ_BL_BILLING.NEXTVAL 은 실패한 INSERT 에서도
소비(시퀀스 비트랜잭셔널) → R3 처럼 5만 회 NEXTVAL.

---

## 4. 측정 한계 — 절대값은 환경 노이즈로 ~7배 부풀려짐 (정직 고지)

이 측정의 **절대 시간은 신뢰하지 말 것.** 상대 순위·배율과 멱등성 정확성만 유효.

| 근거 | 값 |
|---|---|
| R1 single-save 시드 | **45,048 ms** (이번) vs **6,482 ms** (2026-05-13 R1) → **~7배 느림** |
| 원인 추정 | WSL2 초기화 후 Docker 볼륨 재구성(ADR-004 복구) 직후 cold Oracle SGA/buffer cache + Docker Desktop on Windows I/O + 동시 실행 중인 gradle daemon·bootRun JVM |
| 측정 방법 오염 | 폴링이 `docker exec rental-oracle sqlplus` 를 ~3초마다 호출 → 측정 대상 컨테이너에 부하 가중 |

그럼에도 결론이 견고한 이유:
- **A vs B vs C 는 같은 세션·같은 warm 상태에서 연속 측정** → 상대 비교 내적 일관성 유지.
- **C 가 A/B 대비 ~33배** 차이는 7배 노이즈로 뒤집히지 않는 큰 격차.
- A≈B(예외 무관) 발견은 배율이 아니라 동률이라 노이즈에 강건.

> ⚠️ 정밀 절대값이 필요하면 **격리된 clean 환경**(콜드 캐시 워밍업 + 폴링 부하 제거 + 단독 실행)에서
> R1~R6 전체 재측정 필요. 본 리포트는 R6 의 *질적 결론*(3안 정확·라운드트립이 비용·MERGE 가 답)을 확정한다.
> R1~R4 의 옛 절대값 표([2026-05-13](2026-05-13-billing-create-r1-r2-r3-r4.md))와 본 R6 절대값을 **직접 비교 금지**.

---

## 5. 측정 메타

| 항목 | 값 |
|---|---|
| 트리거 | `POST :9093/internal/batch/run/BILLING_CREATE` (X-Internal-Token, fire-and-forget 202) |
| 페이로드 | `{billingMonth:"2026-06", roundNo:6, strategy:<catch-continue|select-insert|merge>}` |
| 대상 계약 수 | 50,000 (CT_CONTRACT ACTIVE) |
| baseline 시드 | R1 single-save → 2026-06 에 50,000 건 (id=152) |
| 충돌 제약 | `UK_BL_BILLING_CONTRACT_MONTH (CONTRACT_ID, BILLING_MONTH)` + `UK_BL_BILLING_NO` |
| jdbc.batch_size | 1000 (C 의 batchUpdate 가 활용) |
| SEQ_BL_BILLING | INCREMENT_BY=50 NOCACHE (A·C 는 NEXTVAL 5만 회 소비) |
| tx | 3안 모두 strategy 메서드 단일 `@Transactional` (BatchRunnerService.run() 무 tx — R5 회귀가드 유지) |
| BL_BATCH_LOG | 측정행 id 152·153·154·155 (2026-06). BL_BILLING 2026-06 = 50000 (불변) |
| DB 부수효과 | BL_BILLING 에 2026-06 50,000 행 영속(시드분). 다른 month 무영향 |

---

## 6. 학습 takeaway

> **멱등성은 "어떻게 충돌을 처리하느냐" 가 아니라 "왕복을 몇 번 하느냐" 의 문제다.**
>
> - 정확성: catch+continue / SELECT-then-INSERT / MERGE 3안 모두 멱등(중복 0·에러 0). 선택 기준은 정확성이 아님.
> - 성능: per-row(A·B)는 5만 동기 왕복이 지배 → 예외 처리 유무는 무차이(296s≈306s).
>   set-based MERGE 는 왕복을 ~50 으로 접어 33배 빠름(9s).
> - 운영 함의: 멱등 재처리·복구 배치를 per-row 가드로 짜면 충돌률이 높을수록 선형 폭발.
>   **DB 엔진에 위임(MERGE/UPSERT)** 이 정석. 애플리케이션 멱등 가드는 충돌이 드물 때만 정당.
> - 함정: JPA naive try/catch 는 rollback-only 로 "건너뛰고 계속" 이 원천 불가.
>   per-row catch 가 꼭 필요하면 JDBC statement-level rollback 에 기대야 한다.

R3(배칭이 본질) → R6(멱등성도 결국 배칭) 으로 일관. "JPA 우회가 빠르다"(R3 반증)와
"예외가 비싸다"(R6-A≈B 반증) 두 직관 모두 측정으로 깨졌다 — 진짜 변수는 라운드트립 수.

---

## 7. 후속

- Ch.1 6 라운드(R1~R6) 측정 **완료**. 순수 성능 결론은 R1~R4 리포트, 멱등성은 본 리포트.
- 절대값 정밀화는 clean 환경 재측정 과제(§4) — 선택. 질적 결론은 확정이므로 학습 목적상 불필요할 수 있음.
- Step 8(UNDO 폭주 재현 환경) / Step 9(Ch.3 Kafka) 로 진행. R5 §4 의 "R1 실패 / R4 통과 UNDO 임계 탐색" 미완 항목은 99 §다음 작업에 보존됨.
