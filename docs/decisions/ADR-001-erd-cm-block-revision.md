# ADR-001 — ERD CM_* 블록 1차 검토 결과 반영

- **작성일**: 2026-05-11
- **상태**: 확정 (CM_* 블록만. CT_*, BL_* 는 추후 동일 절차 진행 예정)
- **연관 산출물**: `docs/06. ERD 및 테이블 정의서.md`

---

## 1. Context (배경)

기획 단계 완료 후 ERD 산출물(06번) 초안에 대한 1차 검토.
대상: CM_* 7개 테이블 (CM_CODE_GROUP, CM_CODE, CM_USER, CM_ROLE, CM_MENU, CM_ROLE_MENU, CM_NOTIFICATION).

검토 동기:
- 백오피스 ERP 표준 컬럼(감사·업무비고) 누락
- PK 생성 전략 모호 (SEQUENCE vs IDENTITY)
- 일부 테이블 누락 컬럼·인덱스
- 명명 규칙 미명시

---

## 2. Decision (결정)

### 2-1. 공통 9컬럼 도입 (참고 프로젝트 GDI 차용)

모든 테이블에 다음 9컬럼 필수 적용:
- `WRK_RMK` (업무비고) ×1
- 감사 컬럼 (`FIRS_REG_*` / `FINA_REG_*` 각 4컬럼) ×8

근거:
- 백오피스 ERP 표준. 누가·언제·어디서·어떤 프로그램으로 등록/수정했는지 추적 필수
- 학습 / 코드 리뷰 시 "감사 컬럼 어떻게 설계했냐" 꼬리질문 정면 대응
- 참고 프로젝트(GDI) 의 검증된 패턴 차용 — 새로 발명할 필요 없음

대안 검토:
- JPA 표준 4컬럼 (`@CreatedDate`, `@CreatedBy` 등) → 어노테이션 편의는 있으나 pgm_id·ip 추적 불가. **기각**
- 시간만 2컬럼 (`CREATED_AT`, `UPDATED_AT`) → 사용자 추적 불가. **기각**

### 2-2. PK 생성 전략 — Oracle SEQUENCE

- 테이블별 SEQUENCE 1:1 매핑 (예: `SEQ_CM_USER`)
- JPA `@SequenceGenerator(allocationSize = 50)` 적용

근거:
- `GENERATED ALWAYS AS IDENTITY` 는 Hibernate batch insert 비활성화
- 본 프로젝트 Ch.1 학습 목표(5만 건 bulk INSERT)와 정면 충돌
- SEQUENCE + allocationSize=50 → 시퀀스 round-trip 1/50 로 감소

### 2-3. 명명 규칙

- 케이스: 대문자 + 언더스코어 (산출물 기존 표기 유지)
- 비즈니스 컬럼: 풀네임 (`BILLING_AMOUNT`) — GDI 약어 패턴(`bill_amt`) 미적용
- 근거: 학습 프로젝트 가독성 우선. 약어 학습은 회사 입사 후 자연 습득

### 2-4. 테이블별 변경

| 테이블 | 변경 |
|---|---|
| CM_CODE_GROUP | `CREATED_AT`/`UPDATED_AT` 제거 (공통 9컬럼 대체) |
| CM_CODE | 동상 + 인덱스 `IDX_CM_CODE_GROUP` (GROUP_CODE, USE_YN, SORT_ORDER) 추가 |
| CM_USER | 잠금 해제 정책 명시 (LOCKED_AT + 30분 자동 해제) |
| CM_ROLE | `USE_YN` 추가 |
| CM_MENU | `MENU_DEPTH`, `MENU_TYPE`, `ICON_CLASS` 추가 |
| CM_ROLE_MENU | 공통 9컬럼 적용 |
| CM_NOTIFICATION | `RECIPIENT_USER_ID`, `REF_TYPE` 추가 + 인덱스 `IDX_CM_NOTIFICATION_UNREAD` (READ_YN, CREATED_AT DESC) |

### 2-5. 의도적 비채택

| 항목 | 결정 | 사유 |
|---|---|---|
| CM_ROLE_MENU 에 `EXCEL_DOWNLOAD_YN` | 미도입 | 학습 프로젝트 범위 초과. READ 권한으로 흡수 |
| CM_USER 에 `PASSWORD_CHANGED_AT` | 미도입 | 90일 변경 강제 정책은 학습 범위 외 |
| CM_USER 에 `LAST_LOGIN_IP` | 미도입 | 감사 컬럼 IP 로 갈음 |
| CM_NOTIFICATION 의 9컬럼 적용 | 도입 (단 주의) | 시계열 로그에 과한 측면 있음. 운영 부담 시 ADR-N 으로 예외 처리 검토 |

---

## 3. Consequences (영향)

### 긍정
- 모든 데이터 변경 이력 추적 가능 (감사·컴플라이언스 대응)
- bulk INSERT 학습 시나리오와 정합 (SEQUENCE allocationSize)
- 참고 프로젝트와 컨벤션 일치 → ERP 도메인 학습 효율 ↑
- 코드 리뷰 시 "왜 이렇게 설계했냐" 모든 질문에 ADR 로 답변 가능

### 부정 / 비용
- 모든 테이블에 9컬럼 추가 → 데이터 부피 증가 (테이블당 약 250 bytes/row 증가)
- INSERT/UPDATE 로직마다 9컬럼 채우는 보일러플레이트 — 단, JPA `@PrePersist`/`@PreUpdate` 또는 인터셉터로 자동화 가능 (별도 ADR 예정)
- CM_NOTIFICATION 같은 시계열 로그에 과적용 우려

---

## 4. 다음 액션

- [ ] CT_* 블록(6개 테이블) 동일 검토 절차 — ADR-002
- [ ] BL_* 블록(4개 테이블) 동일 검토 절차 — ADR-003
- [ ] 감사 컬럼 자동 주입 메커니즘 설계 — backoffice/guide/decisions/ 에 별도 ADR
- [ ] 도메인 용어집 초기 골격 작성 (`docs/domain-terms/`)

---

## 5. 후속 보강

### 5-1. ADR-008 (2026-05-11) — 권한 모델 전환

본 ADR 에서 정의한 **`CM_ROLE_MENU` (메뉴 × R/W/D 매트릭스)** 는 [ADR-008](ADR-008-permission-model-auth-code.md) 에서 **폐기**됨.

폐기 사유:
- R/W/D 로 매핑이 모호한 액션 다수 (잠금해제·배치실행·엑셀다운로드 등)
- 운영 단계 권한 추가 시 매번 코드 배포 부담

대체 모델:
- `CM_AUTH` (권한 키 마스터, 행 단위 `AUTH_CODE`)
- `CM_ROLE_AUTH` (역할-권한 매핑)
- 메뉴 진입은 "해당 메뉴의 `*_VIEW` 권한 보유" 로 판정

자세한 근거·DDL·후속 작업은 [ADR-008](ADR-008-permission-model-auth-code.md) 참조.
