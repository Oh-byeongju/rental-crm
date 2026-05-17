# ADR-002 — ERD CT_* 블록 1차 검토 결과 반영

- **작성일**: 2026-05-11
- **상태**: 확정
- **연관 산출물**: `docs/06_ERD 및 테이블 정의서.md`
- **선행 ADR**: ADR-001 (공통 9컬럼·SEQUENCE·명명 규칙 — 본 ADR 에서도 동일 적용)

---

## 1. Context

CT_* 6개 테이블 (CT_CUSTOMER / CT_EQUIPMENT / CT_PRODUCT / CT_CONTRACT / CT_ENGINEER / CT_VISIT) 검토.
ADR-001 에서 확정한 공통 정책(9컬럼, SEQUENCE, 대문자, 풀네임) 동일 적용.

---

## 2. Decisions

### 2-1. 사람이 읽는 식별자 컬럼 추가

각 도메인 마스터에 `*_CODE` 또는 `*_NO` 컬럼 신규:

| 테이블 | 컬럼 | 형식 예시 |
|---|---|---|
| CT_CUSTOMER | `CUSTOMER_NO` | `CUST-20260511-00001` |
| CT_EQUIPMENT | `EQUIPMENT_CODE` | `EQ-AC-0001` |
| CT_PRODUCT | `PRODUCT_CODE` | `PROD-AC-0001` |
| CT_ENGINEER | `ENGINEER_CODE` | `ENG-00001` |

**근거**:
- PK(NUMBER 시퀀스)는 시스템 식별자 — 사용자가 직접 보지 않음
- 외부 노출 / 검색 / 콜센터 상담 / 종이 출력물에는 사람이 읽는 코드 필요 (ERP 표준)
- 코드 리뷰 시 "왜 PK 두고 별도 NO 컬럼?" 꼬리질문 정면 대응

### 2-2. CT_CUSTOMER 보강

- `BIRTH_DATE` — 본인 인증 보조 (선택)
- `ADDRESS_ZIP` — 우편번호 분리
- `TERMS_AGREE_YN` — 약관 동의 (회원가입 필수). 마케팅/개인정보 동의 분리는 학습 범위 외 — 단일 컬럼으로 단순화
- `LAST_LOGIN_AT` — CM_USER 와 컨벤션 일치 (잠금 정책 ADR-001 §2-5 적용)
- `EMAIL VARCHAR2(200)` → `VARCHAR2(254)` (RFC 5321 표준 최대값)

### 2-3. CT_CONTRACT 보강 — 일시정지 추적 명시화

기존엔 `CONTRACT_STATUS = SUSPENDED` 만 있고 일시정지 사유/시점 추적 불가.

추가 컬럼:
- `SUSPENDED_AT` (일시정지 시작 일시)
- `SUSPEND_REASON` (사유)
- `RESUMED_AT` (재개 일시)

**근거**: 04 기능명세서 §0-2 에 "ACTIVE ↔ SUSPENDED" 양방향 전이 명시되어 있는데 추적 컬럼이 없으면 운영 시 "왜 이 계약이 한때 정지됐는지" 추적 불가.

### 2-4. CT_VISIT 보강 — 취소 사유 추적

`CANCEL_REASON` 추가. `VISIT_STATUS = CANCELLED` 시 사유 기록 필수.

### 2-5. 인덱스 추가 (조회 패턴 기반)

| 테이블 | 인덱스 | 용도 |
|---|---|---|
| CT_CUSTOMER | `IDX_CT_CUSTOMER_NAME` (CUSTOMER_NAME) | 고객명 LIKE 검색 |
| CT_CUSTOMER | `IDX_CT_CUSTOMER_PHONE` (PHONE) | 연락처 검색 |
| CT_EQUIPMENT | `IDX_CT_EQUIPMENT_TYPE` (EQUIPMENT_TYPE, USE_YN) | 유형별 사용중 장비 |
| CT_PRODUCT | `IDX_CT_PRODUCT_EQUIPMENT` (EQUIPMENT_ID, USE_YN) | 장비별 사용중 상품 |
| CT_CONTRACT | `IDX_CT_CONTRACT_CUSTOMER` (CUSTOMER_ID, CONTRACT_STATUS) | 고객별 계약 (상태 필터) |
| CT_CONTRACT | `IDX_CT_CONTRACT_STATUS` (CONTRACT_STATUS) | 월청구 배치 대상 (`ACTIVE` 만) |
| CT_CONTRACT | `IDX_CT_CONTRACT_END_DATE` (END_DATE, CONTRACT_STATUS) | 만료 임박 계약 |
| CT_ENGINEER | `IDX_CT_ENGINEER_AREA` (AREA, USE_YN) | 지역별 가용 기사 |
| CT_VISIT | `IDX_CT_VISIT_ENGINEER_DATE` (ENGINEER_ID, SCHEDULED_DATE) | 기사 일정 (5건 초과 검증 — 04 §5-1) |
| CT_VISIT | `IDX_CT_VISIT_CONTRACT` (CONTRACT_ID, VISIT_STATUS) | 계약별 방문 이력 |

**근거**: Ch.2 학습 목표(쿼리 튜닝, 실행계획 비교)의 출발점. 인덱스 없이 시작해서 추가하는 흐름도 가능하나, **누락 시 발생 가능한 인덱스 누락 안티패턴**을 처음부터 차단.

### 2-6. 추가 컬럼

- CT_EQUIPMENT: `RELEASE_DATE` (출시일 — 단종 관리 보조), `IMAGE_URL` (고객 포털 표시)
- CT_PRODUCT: `DEPOSIT_AMOUNT` (보증금, 기본 0), `INSTALL_FEE` (설치비, 기본 0)
- CT_ENGINEER: `ENGINEER_TYPE` (`INTERNAL` / `EXTERNAL` — 외주 비용 산정 시 분기)

---

## 3. 의도적 비채택

| 항목 | 사유 |
|---|---|
| CT_CUSTOMER 마케팅/개인정보 별도 동의 컬럼 | 단일 `TERMS_AGREE_YN` 으로 통합. 학습 범위 단순화 |
| CT_CUSTOMER `GENDER` | 학습 시나리오 무관 |
| CT_CONTRACT `BILLING_DAY` (고객별 청구일) | 04 §6-1 은 매월 1일 일괄. 다양화는 학습 범위 외 |
| CT_CONTRACT `AUTO_RENEW_YN` | 자동 갱신 로직은 학습 시나리오에 없음 |
| CT_PRODUCT `PENALTY_RATE` (중도해지 위약금) | 04 §4-2 해지 흐름엔 위약금 계산 없음 |
| CT_ENGINEER `CERTIFICATION` / `HIRED_DATE` | 인사 도메인 — 범위 외 |
| CT_VISIT `COMPLETION_PHOTO_URL` | 파일 업로드 시나리오 없음 |
| CT_VISIT `SCHEDULED_TIME` (시각) | 일자 단위로 충분 (04 §5-1) |

---

## 4. Consequences

### 긍정
- 인덱스 사전 설계로 Ch.2 쿼리 튜닝 학습 시 비교 기준점 확보 (인덱스 유/무 실행계획 비교 가능)
- 상태 전이(SUSPENDED, CANCELLED) 의 사유/시점 추적 — 실무 운영 시 "왜 이 데이터?" 답 가능
- 사람이 읽는 식별자(`*_CODE`/`*_NO`) — 포트폴리오 어필 + 콜센터 시나리오 호환
- ADR-001 의 공통 정책(9컬럼·SEQUENCE) 일관 적용

### 부정 / 비용
- 인덱스 10개 추가 → INSERT/UPDATE 비용 증가. Ch.1 bulk INSERT 측정 시 인덱스 영향 의식 필요
- 컬럼 추가로 테이블 부피 증가. 학습 데이터 50만 건 규모엔 무시 가능

---

## 5. 다음

- [ ] BL_* 블록 검토 — ADR-003
- [ ] 17개 테이블 전체 DDL 생성 → `init-scripts/oracle/01-create-schema.sql`
