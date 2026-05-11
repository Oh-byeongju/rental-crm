---
description: "신규 테이블 / 컬럼 / 시퀀스 / 인덱스 설계 시 — DB 메타 룰 (타입·명명·기본값)"
---

# DB 컨벤션 룰 (전역)

> 모든 도메인의 테이블/컬럼/인덱스 작성에 적용. 참고 프로젝트(GDI) 기반 + rental-crm 도메인 추가.
> ERD 산출물(`docs/06`) + DDL(`infra/init-scripts/oracle/`) + JPA 엔티티 매핑 모두 본 룰을 따른다.

---

## 1. `*_YN` 플래그 컬럼

### 1-1. 타입 — **VARCHAR2(1)**

```sql
-- ✅ 정답
USE_YN          VARCHAR2(1)  NOT NULL,
TERMS_AGREE_YN  VARCHAR2(1)  NOT NULL,
READ_YN         VARCHAR2(1)  DEFAULT 'N' NOT NULL,

-- ❌ 잘못 — CHAR(1) 미사용
USE_YN          CHAR(1)  NOT NULL,   -- 사용 금지
```

### 1-2. 허용 값 — **`'Y'` / `'N'` 만**

`true`/`false` / `1`/`0` / `'T'`/`'F'` / `NULL` 모두 **금지**.

### 1-3. CHECK 제약 (선택)

엄격 검증이 필요한 경우:
```sql
USE_YN VARCHAR2(1) DEFAULT 'Y' NOT NULL,
CONSTRAINT CK_{TABLE}_USE_YN CHECK (USE_YN IN ('Y', 'N'))
```

학습 프로젝트 단순화를 위해 모든 `*_YN`에 CHECK 강제하지는 않음. 다만 비즈니스 중요 플래그(예: `TERMS_AGREE_YN`)는 권장.

### 1-4. JPA 매핑

VARCHAR2(1) 은 JPA `String` 기본 매핑과 일치. **별도 어노테이션 불필요**.

```java
// ✅ 정답
@Column(name = "USE_YN", length = 1, nullable = false)
private String useYn;

// ❌ 잘못 — CHAR 매핑 어노테이션 불필요
@JdbcTypeCode(Types.CHAR)
@Column(name = "USE_YN", length = 1)
private String useYn;
```

### 1-5. 근거 (참고 프로젝트)

GDI 의 `*_yn` 패턴 (`use_yn`, `del_yn`, `end_yn`) 은 모두 VARCHAR(1). 참고 프로젝트와 일관성 유지 + Oracle 권장 (Trailing space padding 회피).

---

## 2. 기타 데이터 타입 컨벤션

| 의미 | 타입 | 비고 |
|---|---|---|
| PK / FK ID (시퀀스) | `NUMBER` | 시퀀스로 자동 생성 |
| 사람이 읽는 식별자 (예: `CUST-YYYYMMDD-NNNNN`) | `VARCHAR2(20~50)` | 도메인별 |
| 명칭 / 짧은 이름 | `VARCHAR2(100)` | |
| 긴 텍스트 / 설명 | `VARCHAR2(500~1000)` | |
| 메모 / 자유 비고 | `VARCHAR2(100)` (예: `WRK_RMK`) ~ `VARCHAR2(1000)` (예: `MEMO`) | |
| 이메일 | `VARCHAR2(254)` | RFC 5321 |
| 연락처 | `VARCHAR2(20)` | 숫자만 저장 권장 |
| 코드값 | `VARCHAR2(50)` | CM_CODE 의 CODE_VALUE 등 |
| 상태값 | `VARCHAR2(20)` | `ACTIVE`/`SUSPENDED`/`TERMINATED` |
| 일시 (초 단위) | `TIMESTAMP(0)` | |
| 날짜만 | `DATE` | |
| 금액 | `NUMBER` | 원 단위 정수 |
| 개수/일수/카운트 | `NUMBER` | |
| 비밀번호 해시 (BCrypt) | `VARCHAR2(255)` | |
| IP 주소 | `VARCHAR2(50)` | IPv4 + IPv6 + XFF 헤더 대응 |
| 우편번호 | `VARCHAR2(10)` | |
| URL | `VARCHAR2(200~500)` | |

상세 도메인 단어 사전: [`docs/domain-terms/suffix.md`](../domain-terms/suffix.md) / [`docs/domain-terms/term.md`](../domain-terms/term.md)

---

## 3. 컬럼명 — 대문자 + 언더스코어

```
CUSTOMER_ID, CUSTOMER_NO, CUSTOMER_NAME, BILLING_AMOUNT, FIRS_REG_DTS
```

소문자 / camelCase / kebab-case 금지. 영문 풀네임 + 도메인 약어 (FIRS / FINA / RMK 등).

---

## 4. 테이블명 — `{도메인접두사}_{명사}` 대문자

| 접두사 | 의미 |
|---|---|
| `CM_` | 공통/권한/알림 (Common) |
| `CT_` | 계약/장비/고객 (Contract domain) |
| `BL_` | 청구/수납/배치 (Billing) |

향후 도메인 추가 시 본 표 갱신.

---

## 5. 시퀀스 명명 — `SEQ_{TABLE_NAME}`

```sql
CREATE SEQUENCE SEQ_CT_CUSTOMER  START WITH 1 INCREMENT BY 50 NOCACHE NOCYCLE;
```

- INCREMENT BY 50 — JPA `@SequenceGenerator(allocationSize = 50)` 와 정합 (ADR-001 §2-2)
- 테이블당 1 시퀀스 1:1 매핑
- VARCHAR PK 인 테이블(예: `CM_CODE_GROUP.GROUP_CODE`) 은 시퀀스 없음

---

## 6. 인덱스 명명 — `IDX_{TABLE}_{용도}`

```sql
CREATE INDEX IDX_CT_CUSTOMER_NAME ON CT_CUSTOMER (CUSTOMER_NAME);
CREATE INDEX IDX_BL_BILLING_STATUS_DUE ON BL_BILLING (BILLING_STATUS, DUE_DATE);
```

- PK / UNIQUE 인덱스는 자동 생성 — 명시 X
- 조회 패턴 분석 후 보조 인덱스 명시 (ADR-001 ~ 003 참조)
- 컬럼 순서: 카디널리티 높은 컬럼 → 낮은 컬럼

---

## 7. 공통 9컬럼 (모든 테이블 필수)

상세: [ADR-001 §2-1](../decisions/ADR-001-erd-cm-block-revision.md)

요약:
- `WRK_RMK VARCHAR2(100)` — 업무비고
- 감사 8개 — `FIRS_REG_*` / `FINA_REG_*` (PGM_ID / DTS / USER_ID / IP)

---

## 8. CHECK 제약 — 상태/금액 검증

```sql
CONSTRAINT CK_{TABLE}_STATUS CHECK (CONTRACT_STATUS IN ('ACTIVE', 'SUSPENDED', 'TERMINATED')),
CONSTRAINT CK_{TABLE}_AMOUNT CHECK (BILLING_AMOUNT > 0),
CONSTRAINT CK_{TABLE}_PERIOD CHECK (END_DATE > START_DATE)
```

상태 enum 컬럼 + 금액/일자 비교는 DB 차원에서 강제. JPA Validation 과 별개.

---

## 9. 변경 이력

- 2026-05-11: 신규 작성 — `*_YN VARCHAR2(1)` 룰 명시 (참고 프로젝트 컨벤션 차용)
