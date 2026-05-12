# ADR-013 — 코드 테이블 구조 강화 (S1+S2)

상태: **Accepted** (2026-05-12)
영향 범위: `CM_CODE_GROUP` + `CM_CODE` 테이블 자체 + 모든 도메인의 코드 사용 패턴

---

## 컨텍스트

ref-project (GDI) 의 `sy_code_dtl` / `sy_code_mst` 구조 검토 시 rental-crm 의 `CM_CODE_GROUP` / `CM_CODE` 와 비교. 마스터/디테일 분리 자체는 동일하지만 컬럼 구성 차이:

| 항목 | ref-project | rental-crm 초기 |
|---|---|---|
| 그룹 마스터 시스템 마킹 | `sys_yn` 존재 | 없음 |
| 코드 디테일 설명 | `comm_cd_desc` | 없음 (그룹만 DESCRIPTION) |
| 코드 디테일 확장 속성 | `cd_prop_val1~5` (5개) | 없음 |
| 연관 코드 참조 | `rel_wrk_tp_cd / rel_cd_tp_cd / rel_comm_cd` | 없음 |
| 변경 이력 | `sy_code_dtl_ud_log` 별도 테이블 | 없음 |

확장 결정 — 어디까지 도입할지.

---

## 검토한 대안

### S0. 변경 없음

마스터/디테일 분리 동일 → 구조 동일성 인정. 추가 컬럼 X.

### S1. 핵심 보강 ✅ 부분 채택

- `CM_CODE_GROUP.SYSTEM_YN` — 시스템 예약 그룹 (변경/삭제 차단)
- `CM_CODE.DESCRIPTION` — 코드값 부연 설명

### S2. 확장 속성 추가 ✅ 부분 채택 (3개)

- `CM_CODE.PROP_VAL1~3` — 도메인별 자유 사용 (단축어 / 표시색 / 외부키 등)
- ref-project 의 5개 대신 3개 (YAGNI)

### S3. 연관 코드 참조 ❌ 채택 안 함

- ref-project 의 `rel_*` 컬럼 — 정산 도메인 특수 요구. rental-crm 활용처 없음.

### S4. 변경 이력 테이블 ❌ 채택 안 함

- `CM_CODE_HISTORY` 별도 테이블 — 감사 로직 학습 가치는 있으나 학습 시나리오 (Ch.1-3) 와 별개.

---

## 결정

**S1 + S2 채택 (3개 속성).**

### 추가 컬럼

| 테이블 | 추가 컬럼 | 정책 |
|---|---|---|
| `CM_CODE_GROUP` | `SYSTEM_YN VARCHAR2(1) DEFAULT 'N' NOT NULL` | 시스템 그룹 변경/삭제/하위 코드 추가 전체 차단 |
| `CM_CODE` | `DESCRIPTION VARCHAR2(200)` | 모호한 코드값 부연 (NULL 허용) |
| `CM_CODE` | `PROP_VAL1~3 VARCHAR2(100)` | 도메인별 자유 사용 (NULL 허용) |

### 시스템 마킹 시드

| 그룹 | SYSTEM_YN | 이유 |
|---|---|---|
| `CONTRACT_STATUS` / `BILLING_STATUS` / `NOTIFICATION_TYPE` | **Y** | 도메인 로직 (allow-list / Kafka 알림) 직결 — 운영자 임의 변경 금지 |
| `EQUIPMENT_TYPE` / `PAYMENT_METHOD` / `VISIT_TYPE` | N | 운영 중 추가 가능 (신규 가전 카테고리 / 새 결제 수단 등) |

### 시스템 그룹 차단 정책 (옵션 A — strict)

- 그룹 메타 (이름/설명) 수정 ❌
- 그룹 삭제 ❌
- 하위 코드 신규 추가 ❌
- 하위 코드 수정/삭제 ❌
- 즉 시스템 그룹 = 일체 변경 불가

운영 중 시스템 그룹 추가 필요 시 → 개발자가 시드 SQL 수정 + 배포 (정상 워크플로).

---

## 근거

1. **YAGNI**: ref-project `cd_prop_val1~5` 의 5개 → rental-crm 단순 도메인 (상태 / 유형 / 방법) 에는 5개 활용처 명확 X. 3개로 충분 (사후 평가에서 운영 중 부족하면 확장).
2. **시스템 마킹 가치**: `CONTRACT_STATUS` 같은 도메인 로직 직결 코드를 운영자가 실수 변경 시 시스템 전체 영향. 차단 가치 명확.
3. **변경 이력 미채택 이유**: 감사 로직 학습은 학습 시나리오 Ch.1-3 와 별개 + 변경 사례 적음 (시스템 코드는 변경 차단).
4. **연관 코드 미채택 이유**: 정산 도메인 (ref-project) 특수 요구. rental-crm 도메인 (장비 렌탈) 에 활용처 없음.

---

## 사후 평가 (2026-05-12 — 적용 완료)

- DDL / 시드 22 CM_CODE + 6 CM_CODE_GROUP 일괄 갱신 (Python 스크립트)
- Entity / DTO 6 파일 변경
- CodeService 6 위치 변경 (시스템 그룹 검증 헬퍼 + 4 메서드 + register/update Builder 매개변수)
- 화면 4 파일 (그룹 그리드 시스템 badge + 그룹 모달 차단 + 코드 모달 입력 필드 + payload)
- ref-project 구조 차용 시 **YAGNI 적용 + 도메인 적합성 검증** 패턴 수립

---

## 후속 검토 사항

- PROP_VAL 활용 사례 누적 후 ADR 보강 (어떤 도메인이 어떻게 활용했는지)
- 시스템 그룹 추가 시 본 ADR 갱신 (시드 마킹 표)
- 운영 중 변경 이력 필요성 발견 시 별도 ADR (S4) 추진
