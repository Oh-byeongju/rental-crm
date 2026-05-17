# 미납 현황 리포트 쿼리 튜닝 — 스칼라 서브쿼리 vs JOIN (Ch.2)

측정일: 2026-05-17 (Ch.2 — 미납 현황 엑셀 / 쿼리 최적화)
환경: Windows 10 Pro / Docker Desktop / Oracle 21c XE / Spring Boot 3.5 / Java 21
대상: 미수납 청구(`BILLING_STATUS IN ('UNPAID','OVERDUE')`) 50,000건 + 고객/계약/연체 조인

> 04 기능명세 §11-2 "미납 현황 엑셀 — 서브쿼리 vs JOIN 전환 실행계획 비교 기록" 의 실행.
> 운영 코드는 **JOIN 형만** 채택. naive 스칼라 서브쿼리 형은 본 리포트에 학습 자료로만 보존.

---

## 0. 측정 시나리오

"미납 현황" = 미수납 청구 + 고객·계약 정보. 같은 결과를 두 방식으로:

| 형태 | 설명 |
|---|---|
| **NAIVE** | `BL_BILLING` 풀스캔 + 행마다 스칼라 서브쿼리 5개 (고객명/번호/연락처 ×3 → `CT_CUSTOMER`, 계약번호 → `CT_CONTRACT`, 연체일수 → `BL_OVERDUE`) |
| **JOIN** (채택) | `BL_BILLING` ⋈ `CT_CUSTOMER` ⋈ `CT_CONTRACT` + LEFT JOIN `BL_OVERDUE` |

데이터 (stats gather 후):

| 테이블 | 행수 |
|---|---|
| `BL_BILLING` (UNPAID+OVERDUE) | 50,000 (전부 UNPAID — 2026-06 R6 시드분) |
| `CT_CUSTOMER` | 10,000 |
| `CT_CONTRACT` | 50,000 |
| `BL_OVERDUE` | 0 (연체 배치 미실행) |

---

## 1. 결과 요약

| 지표 | NAIVE (스칼라 서브쿼리) | JOIN | 비율 |
|---|---|---|---|
| **EXPLAIN cost** (옵티마이저 추정) | **378,000** | **2,763** | **≈137×** |
| **실제 buffer gets** (캐시 무관 작업량) | **118,000** | **2,702** | **≈44×** |
| 실제 A-Time (warm SGA) | 110 ms | 50 ms | ≈2.2× |
| Plan hash (EXPLAIN) | 1990557123 | 929490101 | |

**세 지표가 서로 다른 이야기를 한다 — 이게 Ch.2 의 핵심 학습.**

---

## 2. EXPLAIN PLAN (옵티마이저 추정)

### 2-1. NAIVE — cost 378K

```
| Id | Operation                     | Name               | Rows  | Cost (%CPU)|
|  0 | SELECT STATEMENT              |                    | 50000 |  378K  (1) |
|  1 |  TABLE ACCESS BY INDEX ROWID  | CT_CUSTOMER        |     1 |    2   (0) |
|* 2 |   INDEX UNIQUE SCAN           | PK_CT_CUSTOMER     |     1 |    1   (0) |   ← 고객 서브쿼리 ①
|  3 |  TABLE ACCESS BY INDEX ROWID  | CT_CUSTOMER        |     1 |    2   (0) |
|* 4 |   INDEX UNIQUE SCAN           | PK_CT_CUSTOMER     |     1 |    1   (0) |   ← 고객 서브쿼리 ② (동일 테이블 재조회)
|  5 |  TABLE ACCESS BY INDEX ROWID  | CT_CUSTOMER        |     1 |    2   (0) |
|* 6 |   INDEX UNIQUE SCAN           | PK_CT_CUSTOMER     |     1 |    1   (0) |   ← 고객 서브쿼리 ③
|  7 |  TABLE ACCESS BY INDEX ROWID  | CT_CONTRACT        |     1 |    2   (0) |
|* 8 |   INDEX UNIQUE SCAN           | PK_CT_CONTRACT     |     1 |    1   (0) |
|  9 |  TABLE ACCESS BY INDEX ROWID  | BL_OVERDUE         |     1 |    0   (0) |
|*10 |   INDEX UNIQUE SCAN           | UK_BL_OVERDUE_BILLING |  1 |    0   (0) |
| 11 |  SORT ORDER BY                |                    | 50000 |  378K  (1) |
|*12 |   TABLE ACCESS FULL           | BL_BILLING         | 50000 |  308   (1) |
```

- 옵티마이저는 스칼라 서브쿼리를 **행당 5회 × 50,000행** 으로 모델링 → 비용 폭증.
- **동일 `CT_CUSTOMER` 를 3번 따로 조회** (SEL$2/3/4). Oracle 은 텍스트가 다른 스칼라 서브쿼리를 병합하지 않는다.

### 2-2. JOIN — cost 2,763

```
| Id | Operation              | Name               | Rows  | Cost (%CPU)|
|  0 | SELECT STATEMENT       |                    | 50000 | 2763   (1) |
|  1 |  SORT ORDER BY         |                    | 50000 | 2763   (1) |
|* 2 |   HASH JOIN            |                    | 50000 | 1012   (1) |
|  3 |    TABLE ACCESS FULL   | CT_CUSTOMER        | 10000 |  102   (0) |
|  4 |    NESTED LOOPS OUTER  |                    | 50000 |  909   (1) |
|* 5 |     HASH JOIN          |                    | 50000 |  908   (1) |
|  6 |      TABLE ACCESS FULL | CT_CONTRACT        | 50000 |  341   (0) |
|* 7 |      TABLE ACCESS FULL | BL_BILLING         | 50000 |  308   (1) |
|  8 |     TABLE ACCESS BY INDEX ROWID | BL_OVERDUE |    1 |    0   (0) |
|* 9 |      INDEX UNIQUE SCAN | UK_BL_OVERDUE_BILLING | 1 |    0   (0) |
```

- 풀스캔 3개를 해시조인 1패스 + 연체는 NL OUTER(인덱스). 250,000 단건 조회 → 해시 빌드/프로브.

---

## 3. 실제 실행 통계 (`gather_plan_statistics`, `ALLSTATS LAST`)

`SUM(LENGTH(...))` 래핑으로 50k 행 전송 없이 서버측 작업만 강제(§4 함정 회피).

### 3-1. NAIVE — Buffers 118K / 110ms (hash 3695161144)

```
| Id | Operation                     | Name           | Starts | A-Rows | A-Time     | Buffers |
|  1 |  TABLE ACCESS BY INDEX ROWID  | CT_CUSTOMER    |  11527 |  11527 |00:00:00.01 |   18085 |
|* 2 |   INDEX UNIQUE SCAN           | PK_CT_CUSTOMER |  11527 |  11527 |00:00:00.01 |    6558 |
|  3 |  TABLE ACCESS BY INDEX ROWID  | CT_CUSTOMER    |  11599 |  11599 |00:00:00.01 |   18179 |
|  5 |  TABLE ACCESS BY INDEX ROWID  | CT_CUSTOMER    |  11527 |  11527 |00:00:00.01 |   18103 |
|  7 |  TABLE ACCESS BY INDEX ROWID  | CT_CONTRACT    |  50000 |  50000 |00:00:00.03 |   63183 |
|* 8 |   INDEX UNIQUE SCAN           | PK_CT_CONTRACT |  50000 |  50000 |00:00:00.01 |   13183 |
|  9 |  TABLE ACCESS BY INDEX ROWID  | BL_OVERDUE     |  50000 |      0 |00:00:00.01 |       0 |
|*12 |   TABLE ACCESS FULL           | BL_BILLING     |      1 |  50000 |00:00:00.01 |    1074 |
|  0 | SELECT STATEMENT              |                |      1 |      1 |00:00:00.11 |     118K|
```

> **발견 — Oracle 스칼라 서브쿼리 캐싱**: `CT_CUSTOMER` 3개 서브쿼리의 Starts 가 50,000 이 아니라
> **~11,527 / 11,599 / 11,527**. 50,000 청구가 10,000 고객을 가리켜(NDV 낮음) 입력값 반복 →
> Oracle 이 스칼라 서브쿼리 결과를 세션 해시로 캐시. 반면 `CT_CONTRACT` 는 Starts **50,000**
> (계약 NDV = 50,000, 캐시 이득 0). 즉 옵티마이저 cost(378K)는 캐싱을 반영 안 한 과대추정,
> 실제는 캐싱 덕에 일부 완화됐지만 여전히 contract 차원이 50k 단건조회로 76k buffers 소모.

### 3-2. JOIN — Buffers 2,702 / 50ms (hash 2939673710)

```
| Id | Operation              | Name           | Starts | A-Rows | A-Time     | Buffers | Used-Mem |
|* 2 |   HASH JOIN            |                |      1 |  50000 |00:00:00.06 |    2702 | 1844K(0) |
|  3 |    TABLE ACCESS FULL   | CT_CUSTOMER    |      1 |  10000 |00:00:00.01 |     373 |          |
|* 5 |     HASH JOIN          |                |      1 |  50000 |00:00:00.03 |    2329 | 4588K(0) |
|  6 |      TABLE ACCESS FULL | CT_CONTRACT    |      1 |  50000 |00:00:00.01 |    1256 |          |
|* 7 |      TABLE ACCESS FULL | BL_BILLING     |      1 |  50000 |00:00:00.01 |    1073 |          |
|  8 |     TABLE ACCESS BY INDEX ROWID | BL_OVERDUE |  50000 |  0 |00:00:00.01 |       0 |          |
|  0 | SELECT STATEMENT       |                |      1 |      1 |00:00:00.05 |    2702 |          |
```

- 풀스캔 3개(373+1256+1073) 한 번씩 + 해시조인 in-memory(`(0)` = 스필 없음). Buffers 2,702.

---

## 4. 측정 방법론 함정 (정직 고지)

1. **첫 actual 측정 오류 — FK+COUNT join elimination**: `COUNT(col)` 로 래핑하니 JOIN 형이
   Buffers 1,073 으로 찍힘. `BILLING.CUSTOMER_ID/CONTRACT_ID` 가 NOT NULL FK → PK 라서
   `COUNT(c.CUSTOMER_NO)` = 행수와 동치 → 옵티마이저가 `CT_CUSTOMER/CT_CONTRACT` 조인을
   **통째로 제거**. 공정 비교 불가 → `SUM(LENGTH(...))` 로 값 실체화 강제해 재측정(§3 = 2,702).
2. **warm cache 압축**: 50k 가 전부 SGA 적재 → 118k 논리적 읽기도 메모리 CPU 라 110ms.
   wall-clock(2.2×)만 보면 차이가 작아 보임. **cold / 대용량 / 동시성** 에서 buffer gets(44×)가
   래치·CPU·물리 I/O 로 현실화. → 절대 시간 아닌 **buffer gets** 가 정직한 지표.
3. **stats 선행 필수**: WSL2 재구성 직후 통계 없으면 동적 샘플링 → 비현실 plan.
   측정 전 4개 테이블 `DBMS_STATS.GATHER_TABLE_STATS` 수행.

---

## 5. 학습 takeaway

> **"서브쿼리 vs JOIN" 은 단순히 'JOIN 이 빠르다' 가 아니다. 세 층위가 다른 진실을 말한다.**
>
> - **옵티마이저 cost(137×)** = 모델 추정. 스칼라 서브쿼리를 행당 실행으로 보고 폭증.
>   스칼라 서브쿼리 캐싱을 cost 에 안 넣어 과대추정.
> - **실제 buffer gets(44×)** = 캐시 무관 진짜 작업량. 가장 신뢰할 지표.
>   NAIVE 는 차원 테이블을 행마다 단건 조회 → 논리적 읽기 증폭.
> - **warm wall-clock(2.2×)** = 소규모·캐시 적중 시 차이 압축. 여기에 속으면 안 됨.
> - **스칼라 서브쿼리 캐싱**: NDV 낮은 차원(고객 10k)은 50k→~11.5k 로 자동 완화,
>   NDV 높은 차원(계약 50k)은 완화 0. naive 의 위험은 **고NDV 상관 차원**에서 폭발.
> - **동일 테이블 다중 스칼라 서브쿼리는 병합 안 됨** — 고객 3컬럼을 3번 조회. JOIN 은 1프로브.
>
> 결론: 리포트성 다중 차원 조인은 **JOIN 형**. naive 스칼라 서브쿼리는 고NDV·cold·대용량·동시성에서
> 선형 폭발. EXPLAIN cost 로 위험 신호를 잡고, buffer gets 로 실제를 검증한다.

R3·R6(배칭이 본질) 과 동형 교훈: "왕복/단건 반복 vs 집합 연산". Ch.1 은 INSERT, Ch.2 는 SELECT.

---

## 6. 운영 코드 반영 + 기능 검증

- **채택**: JOIN 형만 `OverdueReportRepository` (네이티브 + 인터페이스 프로젝션). naive 미탑재(불필요한 느린 코드 X — 본 리포트가 baseline 보존).
- **스트리밍 엑셀** (04 §11-1): `SXSSFWorkbook(window=100)` + repository `Stream<>` 커서(fetchSize 500) + readOnly tx + 0건 사전 가드.
- **스모크 검증** (backoffice 9091):
  | 항목 | 결과 |
  |---|---|
  | `GET /admin/reports/overdue` | HTTP 200, 화면 렌더 |
  | `GET /api/reports/overdue?billingMonth=2026-06&size=3` | `SUCCESS`, totalElements 50000, 프로젝션 매핑 정상 |
  | `GET /api/reports/overdue/excel?billingMonth=2026-06` | xlsx 2.13 MB, **50,001행**(헤더+5만), Content-Type 정상 |
  | `GET .../excel?billingMonth=2099-01` (0건) | **HTTP 400 JSON** `INVALID_REQUEST`, partial file 없음 |

---

## 7. 측정 메타

| 항목 | 값 |
|---|---|
| EXPLAIN | `EXPLAIN PLAN FOR` + `DBMS_XPLAN.DISPLAY(...,'ALL')` (실 SELECT, ORDER BY 포함) |
| 실제통계 | `/*+ gather_plan_statistics */` + `DBMS_XPLAN.DISPLAY_CURSOR(...,'ALLSTATS LAST')`, SYSDBA `CURRENT_SCHEMA=RENTAL` (rental 유저 PLUSTRACE 미보유) |
| 작업강제 | `SUM(LENGTH(col)+...)` 래핑 — 50k 전송/스칼라제거/조인제거 모두 회피 |
| stats | 측정 직전 `DBMS_STATS.GATHER_TABLE_STATS` × 4 테이블 |
| 데이터 | 2026-06 BL_BILLING 50,000 (전부 UNPAID), BL_OVERDUE 0 |
| ⚠️ 환경 | cold post-WSL2 + Docker Desktop on Windows. 절대 시간 부정확, **buffer gets·cost·plan 구조**만 유효 |

---

## 8. 후속

- Ch.2 학습 본체(미납 현황 엑셀 + 쿼리튜닝 + SXSSF) **완료**. 화면 풀스택 + 리포트.
- 04 §1-11 잔여 리포트(청구 현황 엑셀 / 월별 수납 통계)는 학습 본체 아님 — 필요 시 별도.
- BL_OVERDUE 가 비어 LEFT JOIN 분기 미측정. 연체 배치(OVERDUE_UPDATE) 도입 후 연체 섞인 분포 재측정 가능(선택).
