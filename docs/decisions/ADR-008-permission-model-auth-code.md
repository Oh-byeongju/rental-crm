# ADR-008 — 권한 모델: 메뉴 R/W/D 폐기, AUTH 키 단위 채택

- **작성일**: 2026-05-11
- **상태**: 확정
- **연관**:
  - `docs/decisions/ADR-001-erd-cm-block-revision.md` (선결정 — 후속 보강)
  - `docs/06. ERD 및 테이블 정의서.md` §CM 블록 갱신
  - `docs/04. 기능 명세서.md` §1-3 권한 관리 UX 갱신
  - `docs/07. API 명세서.md` §4 권한/메뉴 API 갱신
  - `infra/init-scripts/oracle/01-create-schema.sql` DDL 갱신
  - `infra/init-scripts/oracle/02-create-indexes.sql` 인덱스 갱신

---

## 1. Context

CM 블록의 권한 모델을 ADR-001 에서 **메뉴 × R/W/D 매트릭스** (`CM_ROLE_MENU`) 로 확정했다.
구현 단계 진입 직전 다음 한계가 드러남:

1. **R/W/D 로 매핑이 모호한 액션 다수**
   - 관리자 잠금 해제 / 비밀번호 재설정
   - 월청구 일괄 생성 배치 실행
   - 통계 엑셀 다운로드
   - 계약 일시정지 / 해지
   - 연체 수동 해제

   위 액션을 모두 W 로 묶으면 "수정" 권한 보유자가 배치 실행·잠금 해제도 가능 → 권한 분리 의도 상실.

2. **운영 단계 권한 추가 시 배포 부담**
   - 새 액션 / 새 화면 도입 시 R/W/D 컬럼은 고정 → 우회 (W 묶음·메뉴 분리·코드 분기) 필요
   - 운영자(비개발자) 가 DB 만으로 권한 정책 조정 불가

3. **참고 프로젝트 (ref-project) 검토**
   - `sy_pgm_info` 에 27 컬럼 (표준 7 + etc 10 + spcl 10) 미리 박는 SI 전통 패턴 발견
   - 효과 (DB 데이터로 권한 관리) 는 유사하나 **컬럼 비대 + 의미 모호** (`etc3_yn` → desc 컬럼 봐야 의미 확인) + 슬롯 한도 27개

---

## 2. Decision

**AUTH 키를 행 단위로 정의하는 모델로 전환.**

### 2-1. 신규 테이블

#### `CM_AUTH` (권한 키 마스터)

| 컬럼 | 타입 | NN | PK/FK | 설명 |
|---|---|---|---|---|
| AUTH_CODE | VARCHAR2(50) | Y | PK | 권한 키 (예: `CUSTOMER_EXPORT`, `BILLING_BATCH_RUN`, `USER_UNLOCK`) |
| AUTH_NAME | VARCHAR2(100) | Y | | 한글명 |
| MENU_ID | NUMBER | | FK | `CM_MENU.MENU_ID`. **NULL 허용** — 메뉴 무관 글로벌 권한 |
| AUTH_TYPE | VARCHAR2(20) | Y | | `VIEW` / `CREATE` / `UPDATE` / `DELETE` / `EXECUTE` (검색·필터링용 분류) |
| SORT_ORDER | NUMBER | Y | | 화면 표시 순서, 기본값 0 |
| USE_YN | CHAR(1) | Y | | 기본값 Y |

+ 공통 9컬럼 / `INDEX IDX_CM_AUTH_MENU (MENU_ID, SORT_ORDER)`

#### `CM_ROLE_AUTH` (역할-권한 매핑)

| 컬럼 | 타입 | NN | PK/FK | 설명 |
|---|---|---|---|---|
| ROLE_AUTH_ID | NUMBER | Y | PK | 시퀀스 |
| ROLE_ID | NUMBER | Y | FK | `CM_ROLE.ROLE_ID` |
| AUTH_CODE | VARCHAR2(50) | Y | FK | `CM_AUTH.AUTH_CODE` |

+ `UNIQUE (ROLE_ID, AUTH_CODE)` + 공통 9컬럼

### 2-2. 폐기

`CM_ROLE_MENU` 완전 삭제. 메뉴 진입은 **"해당 메뉴의 `*_VIEW` 권한 보유 여부"** 로 판정 (단일 source of truth).
시퀀스 `SEQ_CM_ROLE_MENU` 삭제 → `SEQ_CM_ROLE_AUTH` 추가.
(`AUTH_CODE` 는 VARCHAR PK 라 별도 시퀀스 불필요)

### 2-3. AUTH_CODE 명명 컨벤션

- 형식: `{모듈}_{액션}`
- 영문 대문자 + 언더스코어
- 예시:
  - `CUSTOMER_VIEW` / `CUSTOMER_CREATE` / `CUSTOMER_UPDATE` / `CUSTOMER_DELETE` / `CUSTOMER_EXPORT`
  - `BILLING_VIEW` / `BILLING_BATCH_RUN`
  - `USER_VIEW` / `USER_CREATE` / `USER_UNLOCK` / `USER_RESET_PASSWORD`

### 2-4. 사용자 ↔ 역할 관계는 M:1 유지

`CM_USER.ROLE_ID` 단일 FK 그대로 유지. ref-project 의 M:N (`sy_grp_user_info`) 은 비채택 — 학습 단순화. 필요 시 후속 ADR.

---

## 3. Consequences

### 3-1. Positive

- **운영 단계 권한 추가 = DB INSERT 만**. 코드 / 재배포 불필요 (단, 새 액션이 화면 버튼으로 노출되려면 화면 코드 자체는 갱신 필요)
- **권한 키 의미가 자기 설명적** (`CUSTOMER_EXPORT` vs `etc3_yn`)
- **권한 확장 무제한** (행 단위 — 컬럼 한도 없음)
- Spring Security `@PreAuthorize("hasAuthority('CUSTOMER_EXPORT')")` 자연스러운 매핑
- Thymeleaf `${perms.has('CUSTOMER_EXPORT')}` 단순 표현

### 3-2. Negative

- **AUTH_CODE 명명 관리 부담** — 사람이 일관된 컨벤션으로 등록 (자동 생성 아님)
- **신규 화면 추가 시** 메뉴 1행 + 화면별 AUTH 코드 다수 등록 + 역할별 매핑 필요
- 권한 캐싱 필요 — 매 요청 DB 조회 시 부담. Redis 권한 캐시 패턴 도입 (Refresh Token 옆 `permissions:{userId}` 키, Access Token TTL 동기화)

---

## 4. 비채택 대안

| 대안 | 비채택 사유 |
|---|---|
| 메뉴 × R/W/D 유지 (ADR-001 원안) | 모호 액션 다수 + 운영 부담 — 이 ADR 의 동기 |
| ref-project 컬럼 슬롯 (27 컬럼) | SI 전통이나 컬럼 비대 + `etc{N}_yn` 의미 모호 + 한도 27개 |
| 사용자 직접 권한 매핑 (`sy_user_auth_info` 식) | 단순화 우선 — 모든 권한은 역할을 통해서만. 필요시 후속 ADR |

---

## 5. 후속 작업 (이번 사이클 내 완료)

- [x] `docs/06 ERD` — mermaid + 테이블 목록 + CM_AUTH/CM_ROLE_AUTH 컬럼 정의 + CM_ROLE_MENU 삭제
- [x] `infra/init-scripts/oracle/01-create-schema.sql` — 시퀀스/DDL 갱신
- [x] `infra/init-scripts/oracle/02-create-indexes.sql` — CM_AUTH 인덱스 추가
- [ ] `docs/04 기능 명세서` §1-3 권한 관리 UX 갱신
- [ ] `docs/07 API 명세서` §4 권한/메뉴 API 갱신
- [ ] `docs/decisions/ADR-001` 본문에 후속 보강 노트 추가
- [ ] `README.md` ADR 표에 ADR-008 행 추가
- [ ] `infra/init-scripts/oracle/03-seed-codes.sql` — AUTH 코드 시드 + 역할별 권한 시드 (작업 사이클 2단계)

---

## 6. 후속 보강

### 6-1. ADR-009 (2026-05-11) — 사용자 단위 확장

본 ADR 은 권한을 **역할 단위로만** 부여 (`CM_USER.ROLE_ID` 단일 FK). 사용자별 미세 차이 (예: 같은 ADMIN 역할인데 한 명만 `CUSTOMER_EXPORT` 추가) 처리 방안:

| 옵션 | 처리 | 한계 |
|---|---|---|
| A. 사용자 전용 역할 신설 | `CM_ROLE` 행 추가 + `ROLE_ID` 변경 | 역할 폭증 — 50명이면 역할 50개 |
| B. 사용자 직접 권한 매핑 | `CM_USER_AUTH` 행 추가 (GRANT 또는 REVOKE) | 권한 판정 3단계 |

옵션 B 채택 → [ADR-009](ADR-009-user-direct-auth-grant-revoke.md) 에서 `CM_USER_AUTH` 테이블 신설.

최종 권한 판정:
```
(역할 권한) ∪ (사용자 GRANT) − (사용자 REVOKE)
```

본 ADR (행 단위 AUTH 키) 의 "DB 만으로 권한 제어" 정신이 사용자 단위까지 확장됨.
