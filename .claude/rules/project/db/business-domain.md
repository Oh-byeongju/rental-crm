---
description: "신규 테이블 / 시퀀스 / 인덱스 설계 시 — 업무 도메인 prefix → 의미 매핑"
---

# 업무 도메인 매핑 — rental-crm

> 자연어 업무명을 어느 도메인 prefix 로 분류할지 결정.
> 신규 테이블 / 시퀀스 / 인덱스 / 자바 패키지 설계 시 참조.

---

## 도메인 prefix 표

| prefix | 의미 | 예시 테이블 |
|---|---|---|
| `CM` | 공통 / 권한 / 알림 | `CM_USER`, `CM_ROLE`, `CM_AUTH`, `CM_ROLE_AUTH`, `CM_USER_AUTH`, `CM_MENU`, `CM_CODE_GROUP`, `CM_CODE`, `CM_NOTIFICATION` |
| `CT` | 계약 / 장비 / 고객 / 방문 | `CT_CUSTOMER`, `CT_EQUIPMENT`, `CT_PRODUCT`, `CT_CONTRACT`, `CT_ENGINEER`, `CT_VISIT` |
| `BL` | 청구 / 수납 / 배치 | `BL_BILLING`, `BL_PAYMENT`, `BL_OVERDUE`, `BL_BATCH_LOG` |

---

## 사용 규칙

- **테이블 prefix**: 대문자 + 언더스코어 (예: `CT_EQUIPMENT`, `BL_BILLING`)
- **시퀀스 명명**: `SEQ_<테이블명>` (예: `SEQ_CT_EQUIPMENT`, `SEQ_BL_BILLING`)
- **인덱스 명명**: `IDX_<테이블>_<용도>` (예: `IDX_CT_EQUIPMENT_TYPE`, `IDX_BL_BILLING_STATUS_DUE`)
- **자바 패키지**: `com.rental.crm.{도메인}` — prefix 의 의미 단위로 분리 (`admin`, `auth`, `code`, `customer`, `equipment`, `menu` 등). 테이블 prefix 와 자바 패키지가 1:1 매칭은 아님 (CM_* 는 admin/auth/code/menu 로 분리).
- **자연어에서 도메인 prefix 가 명확하지 않으면** 사용자에게 확인. 위 3개 (CM/CT/BL) 외 새 도메인 추정 금지.

---

## 신규 도메인 추가 절차

새 prefix 가 필요한 비즈니스 도메인이 등장하면:

1. 사용자에게 prefix 후보 (2글자 대문자) 확인
2. 본 표에 행 추가
3. ERD 정의서 [`docs/06_ERD 및 테이블 정의서.md`](../../../../docs/06_ERD%20및%20테이블%20정의서.md) §명명 규칙 갱신
4. 시스템 정보 [`@.claude/rules/project/general/system-info.md`](../general/system-info.md) 테이블 접두사 줄 갱신
5. 매핑 표 [`@.claude/rules/project/README.md`](../README.md) 갱신

---

## 관련 룰

- ERD / 테이블 정의: [`docs/06_ERD 및 테이블 정의서.md`](../../../../docs/06_ERD%20및%20테이블%20정의서.md)
- DB 컨벤션: [`docs/global-rules/db-conventions.md`](../../../../docs/global-rules/db-conventions.md)
- SQL 작성: [`@.claude/rules/frame/db/sql-query.md`](../../frame/db/sql-query.md)
- 시스템 정보 (스택·환경): [`@.claude/rules/project/general/system-info.md`](../general/system-info.md)

---

> 출처: 참고 프로젝트의 `guide/03. coding-rules/project/db/business-domain.md` (rental-crm 도메인에 맞춰 8개 prefix → 3개 prefix 로 교체).
