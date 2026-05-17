# ADR-003 — ERD BL_* 블록 1차 검토 결과 반영

- **작성일**: 2026-05-11
- **상태**: 확정
- **연관 산출물**: `docs/06_ERD 및 테이블 정의서.md`
- **선행 ADR**: ADR-001 (공통 9컬럼·SEQUENCE·명명 규칙), ADR-002 (CT_* 패턴)

---

## 1. Context

BL_* 4개 테이블 (BL_BILLING / BL_PAYMENT / BL_OVERDUE / BL_BATCH_LOG) 검토.
**본 블록은 본 프로젝트 3대 학습 시나리오(Ch.1 bulk INSERT / Ch.2 쿼리 튜닝 / Ch.3 Kafka)의 핵심 도메인** — 인덱스/멱등성/추적성 가장 빡세게 챙김.

---

## 2. Decisions

### 2-1. 청구·수납 사람-식별자

- BL_BILLING: `BILLING_NO` (예: `BILL-202605-000001`) UNIQUE
- BL_PAYMENT: `PAYMENT_NO` (예: `PAY-202605-000001`) UNIQUE

**근거**: 고객 문의 / 환불 처리 / 콜센터 응대 시 사람이 읽는 번호 필수. PK 시퀀스값을 그대로 노출하면 정보 누출 (총 청구 건수 추정 가능).

### 2-2. BL_BILLING ↔ BL_BATCH_LOG 추적

`BATCH_LOG_ID` FK 추가 — 어느 배치 실행에서 이 청구가 생성됐는지 직접 추적.

**근거**:
- 04 §6-1 멱등성 위반 시(같은 청구월 2회 실행 등) 누가 어느 배치에서 생성됐는지 추적 가능
- Ch.1 bulk INSERT 학습 중 트러블슈팅에 직결
- 실무 ERP 표준 (배치 출처 추적)

### 2-3. BL_BILLING 추가 컬럼

- `ISSUE_DATE` — 청구 발행일. 통상 청구월 1일이지만 명시적 컬럼으로 분리
- `PAID_AT` — 수납 완료 일시. BL_PAYMENT 와의 JOIN 비용 절감 (역정규화)

**근거**: 청구 조회 화면(04 §6) 에서 "발행일 / 납기일 / 수납일" 3가지 일자를 표시. 매번 JOIN 회피.

### 2-4. BL_PAYMENT 보강 — 결제 취소 추적

`PAYMENT_STATUS` / `CANCELLED_AT` / `CANCEL_REASON` 신규.

**근거**:
- 04 §13-1 (카드 납부) 에 "백엔드 검증 실패 시 Toss 결제 취소 API 호출" 명시
- 결제 취소 = 새 레코드가 아니라 기존 레코드의 상태 변경
- `PAYMENT_STATUS = REFUNDED` 도 환불 시나리오 추후 대응

**TOSS_ORDER_ID UNIQUE 제약** 추가: Toss 결제 중복 방지. NULL 허용 (비-Toss 수납은 NULL).

### 2-5. BL_OVERDUE 단일 레코드 정책

`UNIQUE (BILLING_ID)` 추가 — 한 청구당 한 연체 레코드.

**근거**:
- 연체 재발생(수납 후 다시 미납)은 일반적이지 않음 (이미 한 번 PAID 되면 종료)
- 만약 비즈니스 변경되어 재발생을 추적해야 하면 ADR-N 으로 정책 변경 + UNIQUE 제거

`OVERDUE_AMOUNT` 추가 — 연체 발생 시점의 청구금액 스냅샷. BL_BILLING JOIN 회피.

### 2-6. BL_BATCH_LOG — 성능 측정 데이터 필드

| 컬럼 | 용도 |
|---|---|
| `TARGET_COUNT` | 처리 대상 건수 (계약 5만 건 등) — 배치 시작 시 산정 |
| `PROCESS_COUNT` | 실제 처리 시도 건수 |
| `SUCCESS_COUNT` | 성공 건수 |
| `FAIL_COUNT` | 실패 건수 |
| `DURATION_MS` | 실행 시간 (밀리초) |

**근거**: Ch.1 학습 목표 "건별 INSERT vs bulk INSERT 수치 비교" 의 직접 측정 컬럼. 별도 측정 도구 없이 BL_BATCH_LOG 만으로 포트폴리오 자료 확보 가능.

### 2-7. 인덱스 — 학습 시나리오 매핑

| 인덱스 | 용도 | 학습 시나리오 |
|---|---|---|
| `IDX_BL_BILLING_CUSTOMER` (CUSTOMER_ID, BILLING_MONTH DESC) | 고객 청구 이력 (최근 12건) | Ch.2 — 청구 이력 조회 |
| `IDX_BL_BILLING_STATUS_DUE` (BILLING_STATUS, DUE_DATE) | 연체 배치 대상 | Ch.1 — 연체 일괄 처리 |
| `IDX_BL_BILLING_MONTH` (BILLING_MONTH, BILLING_STATUS) | 월별 통계 / 대시보드 | 대시보드 §9 |
| `IDX_BL_PAYMENT_BILLING` (BILLING_ID, PAYMENT_STATUS) | 청구별 수납 | 일반 |
| `IDX_BL_PAYMENT_CUSTOMER_DATE` (CUSTOMER_ID, PAYMENT_DATE DESC) | 고객별 납부 이력 (최신순) | 일반 |
| `IDX_BL_PAYMENT_DATE_METHOD` (PAYMENT_DATE, PAYMENT_METHOD) | 일별/수단별 통계 | 대시보드 |
| `IDX_BL_OVERDUE_CUSTOMER_RESOLVED` (CUSTOMER_ID, RESOLVED_AT) | 미해결 연체 | Ch.2 — 미납 현황 엑셀 |
| `IDX_BL_OVERDUE_DAYS` (OVERDUE_DAYS DESC) | 장기 연체자 통계 | 대시보드 |
| `IDX_BL_BATCH_LOG_STARTED` (STARTED_AT DESC) | 최근 배치 이력 | 운영 |
| `IDX_BL_BATCH_LOG_TYPE_STATUS` (BATCH_TYPE, BATCH_STATUS) | 실패 배치 재실행 | 운영 |

---

## 3. 의도적 비채택

| 항목 | 사유 |
|---|---|
| BL_BILLING 에 `LATE_FEE` (연체수수료) | 04 기능명세에 연체수수료 계산 없음 |
| BL_PAYMENT 에 `INSTALLMENT_MONTHS` (할부) | 일시불만 학습 범위 |
| BL_PAYMENT 에 `RECEIPT_URL` (영수증) | 파일 출력 시나리오 없음 |
| BL_OVERDUE 재발생 별도 레코드 | 한 청구당 단일 레코드. 재발생은 RESOLVED_AT 갱신 |
| BL_BATCH_LOG `EXECUTOR_USER_ID` | 감사 컬럼 `FIRS_REG_USER_ID` 로 갈음 |

---

## 4. Consequences

### 긍정
- Ch.1 (bulk INSERT) 성능 측정이 BL_BATCH_LOG 컬럼만으로 자족적
- Ch.2 (쿼리 튜닝) 인덱스 사전 설계 — 인덱스 유/무 실행계획 비교 학습 가능
- 결제 취소 시나리오(04 §13-1) 의 데이터 흐름이 BL_PAYMENT 상태 전이로 명확
- BL_BILLING ↔ BL_BATCH_LOG 추적 — 운영 트러블슈팅 즉시 가능

### 부정 / 비용
- BL_BILLING 의 `PAID_AT` 역정규화 — 수납 시 양쪽 UPDATE 필요 (트랜잭션 일관성 필수)
- BL_OVERDUE 의 `OVERDUE_AMOUNT` 역정규화 — 동일 (단 청구 금액은 거의 변경 X 라 위험 낮음)
- 인덱스 10개 추가 — INSERT 비용. Ch.1 측정 시 베이스라인 영향

---

## 5. 다음

- [ ] 17개 테이블 전체 DDL 생성
- [ ] 시퀀스 17개 DDL
- [ ] 인덱스 30+개 DDL
- [ ] 공통코드 시드 데이터
- [ ] 감사 컬럼 자동 주입 메커니즘 설계 (별도 ADR — 코드 진입 시)
