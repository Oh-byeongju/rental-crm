# ADR-009 — 사용자 직접 권한 매핑 (역할 폭증 회피)

- **작성일**: 2026-05-11
- **상태**: 확정
- **연관**:
  - `docs/decisions/ADR-008-permission-model-auth-code.md` (선결정 — 사용자 단위까지 확장)
  - `docs/06. ERD 및 테이블 정의서.md` §CM 블록 갱신 (테이블 19개)
  - `docs/04. 기능 명세서.md` §1-2 관리자 계정 관리 UX 확장
  - `docs/07. API 명세서.md` §3 관리자 관련 API 확장
  - `infra/init-scripts/oracle/01-create-schema.sql` DDL 갱신
  - `infra/init-scripts/oracle/02-create-indexes.sql` 인덱스 갱신

---

## 1. Context

ADR-008 에서 AUTH 키 단위로 권한 모델을 전환했으나, **권한 부여 단위는 여전히 역할만** (`CM_USER.ROLE_ID` 단일 FK).

검토 시나리오 — 오병주(ADMIN 역할) 가 한 가지 권한만 차이가 나는 경우:

| 옵션 | 처리 | 한계 |
|---|---|---|
| A. 사용자 전용 역할 신설 (`OBJ_CUSTOM`) | `CM_ROLE` INSERT + `CM_USER.ROLE_ID` UPDATE + `CM_ROLE_AUTH` 매핑 | 사용자마다 미세 차이 → 역할 폭증. 50명이면 역할 50개 |
| **B. 사용자 직접 권한 매핑 (이번 결정)** | `CM_USER_AUTH` 행 추가 (GRANT 또는 REVOKE) | 권한 판정 로직 복잡 (3단계) |

참고 프로젝트(ref-project)도 `sy_user_auth_info` 로 사용자 직접 권한 매핑을 별도 운영. 역할 매핑(`sy_grp_auth_info`)과 병행.

---

## 2. Decision

**`CM_USER_AUTH` 테이블 신설** — 사용자가 역할에서 받은 권한을 GRANT/REVOKE 단위로 미세 조정.

### 2-1. 신규 테이블

#### `CM_USER_AUTH` (사용자 직접 권한 매핑)

| 컬럼 | 타입 | NN | PK/FK | 설명 |
|---|---|---|---|---|
| USER_AUTH_ID | NUMBER | Y | PK | 매핑 ID (시퀀스) |
| USER_ID | NUMBER | Y | FK | `CM_USER.USER_ID` |
| AUTH_CODE | VARCHAR2(50) | Y | FK | `CM_AUTH.AUTH_CODE` |
| GRANT_TYPE | VARCHAR2(10) | Y | | `GRANT` (역할 권한에 추가) / `REVOKE` (역할 권한에서 제외) |

+ `UNIQUE (USER_ID, AUTH_CODE)`
+ `CHECK (GRANT_TYPE IN ('GRANT', 'REVOKE'))`
+ `INDEX IDX_CM_USER_AUTH_USER (USER_ID)`
+ 공통 9컬럼

### 2-2. 권한 판정 로직

```
최종 권한 = (역할 권한) ∪ (사용자 GRANT) − (사용자 REVOKE)

= CM_ROLE_AUTH   WHERE ROLE_ID = user.role_id
  UNION
  CM_USER_AUTH   WHERE USER_ID = user.id AND GRANT_TYPE = 'GRANT'
  MINUS
  CM_USER_AUTH   WHERE USER_ID = user.id AND GRANT_TYPE = 'REVOKE'
```

`PermissionService` 안에서 SQL 한 번에 처리 (UNION ALL + MINUS) 또는 코드 SetOperations 결합.

### 2-3. 화면 UX

관리자 상세 화면에 "권한 미세 조정" 섹션 추가 — AG Grid:

| AUTH 코드 | 한글명 | 역할 권한 | 개인 조정 | 최종 |
|---|---|---|---|---|
| `CUSTOMER_VIEW` | 고객 조회 | ✅ | — | ✅ |
| `CUSTOMER_EXPORT` | 고객 엑셀 | ❌ | GRANT | ✅ |
| `CONTRACT_TERMINATE` | 계약 해지 | ✅ | REVOKE | ❌ |

"개인 조정" 셀: 드롭다운 `없음` / `GRANT` / `REVOKE`. 저장 시 변경분만 INSERT/DELETE.

### 2-4. 화면 자체의 권한

권한 조정 화면을 누가 조작하는가? — **`USER_UPDATE` 권한 보유자**로 단순화. 별도 `USER_AUTH_MANAGE` 신설은 비채택 (필요 시 후속 ADR).

### 2-5. 시드 데이터

`CM_USER_AUTH` 는 **시드 없이 비워둠**. 실제 운영 시 관리자 상세 화면에서 등록.

---

## 3. Consequences

### 3-1. Positive

- 역할 폭증 회피 — 영업팀 25명이 ADMIN 역할 그대로, 미세 차이만 `CM_USER_AUTH` 행으로
- ADR-008 의 "DB 만으로 권한 제어" 정신을 **사용자 단위까지** 확장
- 권한 변경 이력 자동 기록 (공통 9컬럼) — 누가 언제 GRANT/REVOKE 했는지 추적 무료
- ref-project 의 패턴 (`sy_user_auth_info`) 학습 가치

### 3-2. Negative

- **권한 판정 로직 복잡** — 3단계 (역할 ∪ GRANT − REVOKE)
- Redis 권한 캐시 키도 사용자별 (역할만 캐싱 → 사용자별 캐싱 또는 미스시 합성)
- 5단계 (관리자 도메인) 작업 1.3배 — `CM_USER_AUTH` 화면 + API + 서비스 추가

---

## 4. 비채택 대안

| 대안 | 비채택 사유 |
|---|---|
| 옵션 A — 사용자별 전용 역할 신설 | 역할 폭증. ADR-008 의 핵심 가치 (운영 무중단) 약화 |
| 사용자 다역할 (M:N) | 한 사용자 ROLE_A + ROLE_B 동시 보유. 더 유연하나 OR/AND 결합 정책 부담. GRANT/REVOKE 가 더 명확 |

---

## 5. 무중단 가능 범위 (운영 관점)

| 작업 | DB만 | 코드 변경 |
|---|---|---|
| 사용자에게 AUTH 추가 부여 (GRANT) | ✅ | — |
| 사용자에게서 AUTH 제외 (REVOKE) | ✅ | — |
| 역할의 AUTH 변경 (`CM_ROLE_AUTH`) | ✅ | — |
| 신규 AUTH 키 등록 (`CM_AUTH`) | ✅ | — |
| **새 기능/버튼을 화면에 노출** | ❌ | Thymeleaf 의 `th:if="${perms.has('NEW_AUTH')}"` 박힌 버튼·API 신규 |

**권한 정책 변경 = 무중단**. **새 기능 노출 = 코드 변경**. 둘이 분리되는 게 본 모델의 핵심 가치.

---

## 6. 후속 작업 (이번 사이클 내 완료)

- [x] `docs/06 ERD` — CM_USER_AUTH 추가 (테이블 19개)
- [x] `infra/init-scripts/oracle/01-create-schema.sql` — 시퀀스 + DDL
- [x] `infra/init-scripts/oracle/02-create-indexes.sql` — `IDX_CM_USER_AUTH_USER` 추가
- [ ] `docs/04 기능 명세서` §1-2 — 권한 미세 조정 UX 추가
- [ ] `docs/07 API 명세서` §3 — `GET/PUT /api/users/{userId}/auths` 추가
- [ ] `docs/decisions/ADR-008` 후속 보강 (사용자 단위 확장 노트)
- [ ] `README.md` ADR 표에 ADR-009 추가
- [ ] 5단계 (관리자 도메인 구현) — `PermissionService` 의 UNION/MINUS 로직 + 화면
