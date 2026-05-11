# 컬럼 접미어 → 타입/길이 표

> 신규 컬럼 명명 시 접미어로 타입/길이를 결정. 참고 프로젝트(GDI) 의 `domain_suffix.txt` 에서 rental-crm 에 필요한 항목만 추출 + 도메인 확장.

---

## 기본 접미어 (ERP 공통)

| 접미어 | 의미 | 타입 | 기본 길이 | 예시 |
|---|---|---|---|---|
| `_ID` | 식별자 (PK/FK) | NUMBER 또는 VARCHAR2(20) | - | `USER_ID`, `BILLING_ID` |
| `_NO` | 번호 (사람이 읽는 식별자) | VARCHAR2(20) | 20 | `CONTRACT_NO` (CT-20250401-00001) |
| `_NAME` / `_NM` | 명칭 | VARCHAR2(100) | 100 | `USER_NAME`, `PRODUCT_NAME` |
| `_CODE` | 코드값 | VARCHAR2(50) | 50 | `ROLE_CODE`, `GROUP_CODE` |
| `_DESC` / `_DESCRIPTION` | 설명 | VARCHAR2(500) | 500 | `DESCRIPTION` |
| `_REMARK` / `_RMK` | 비고 | VARCHAR2(100) | 100 | `WRK_RMK` |
| `_MEMO` | 메모 (긴 자유 텍스트) | VARCHAR2(1000) | 1000 | `MEMO` |
| `_AT` / `_DTS` | 일시 (TIMESTAMP) | TIMESTAMP(0) | - | `CREATED_AT`, `FIRS_REG_DTS` |
| `_DATE` | 날짜만 | DATE | - | `START_DATE`, `DUE_DATE` |
| `_MONTH` | 년월 | VARCHAR2(7) | 7 | `BILLING_MONTH` (YYYY-MM) |
| `_AMOUNT` / `_AMT` | 금액 | NUMBER | - | `BILLING_AMOUNT`, `PAYMENT_AMOUNT` |
| `_COUNT` / `_CNT` | 건수/횟수 | NUMBER | - | `LOGIN_FAIL_CNT`, `PROCESS_COUNT` |
| `_YN` | 플래그 (Y/N) | CHAR(1) | 1 | `USE_YN`, `READ_YN` |
| `_TYPE` | 타입/유형 | VARCHAR2(20-50) | 상황별 | `MENU_TYPE`, `VISIT_TYPE` |
| `_STATUS` | 상태 | VARCHAR2(20) | 20 | `BILLING_STATUS`, `CONTRACT_STATUS` |
| `_URL` | URL | VARCHAR2(200) | 200 | `MENU_URL` |
| `_EMAIL` | 이메일 | VARCHAR2(200) | 200 | `EMAIL` |
| `_PHONE` | 전화번호 | VARCHAR2(20) | 20 | `PHONE` |
| `_ADDRESS` / `_ADDR` | 주소 | VARCHAR2(500) | 500 | `ADDRESS`, `INSTALL_ADDRESS` |
| `_IP` | IP 주소 | VARCHAR2(50) | 50 | `FIRS_REG_IP` |
| `_PGM_ID` | 프로그램 ID | VARCHAR2(50) | 50 | `FIRS_REG_PGM_ID` |
| `_PASSWORD` / `_PWD` | 비밀번호 (BCrypt 해시) | VARCHAR2(255) | 255 | `PASSWORD` |
| `_KEY` | 키 (외부 시스템 ID 등) | VARCHAR2(200) | 200 | `TOSS_PAYMENT_KEY` |
| `_ORDER` | 정렬 순서 | NUMBER | - | `SORT_ORDER` |
| `_DEPTH` | 깊이 (계층) | NUMBER | - | `MENU_DEPTH` |
| `_DAYS` | 일수 | NUMBER | - | `OVERDUE_DAYS` |
| `_MSG` / `_MESSAGE` | 메시지 | VARCHAR2(1000) | 1000 | `MESSAGE`, `ERROR_MSG` |
| `_REASON` | 사유 | VARCHAR2(1000) | 1000 | `TERMINATE_REASON`, `RESOLVE_REASON` |
| `_CLASS` | CSS 클래스 등 | VARCHAR2(50) | 50 | `ICON_CLASS` |

---

## rental-crm 도메인 특화 접미어

| 접미어 | 의미 | 타입 | 길이 | 예시 |
|---|---|---|---|---|
| `_FEE` | 요금 (금액과 동일) | NUMBER | - | `MONTHLY_FEE` |
| `_MONTHS` | 개월수 | NUMBER | - | `CONTRACT_MONTHS` |

---

## 길이가 명시되지 않은 경우

- `NUMBER` / `DATE` / `TIMESTAMP` → 길이 불필요
- `VARCHAR2` 인데 본 표에 없는 접미어 → ADR 로 합의 후 본 표에 추가
- 임의로 길이 결정 금지 (가독성·일관성 깨짐)

---

## 변경 이력

- 2026-05-11: 신규 작성 (GDI `domain_suffix.txt` 에서 rental-crm 에 필요한 항목 추출)
