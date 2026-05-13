# TODO

> 3단 체계: NOW (현재) → NEXT (이후) → LATER (언젠가).
> 항목은 자유롭게 단계 사이로 옮긴다.
>
> **99 업무현황** 은 *완료 보고용*, **본 TODO** 는 *작업 예정 추적용*. 역할 분리.

---

## 1. NOW — 현재 사이클 (4회차 — 배치 모듈 분리)

> 2026-05-13 시작. ADR-014 채택 — `rental-batch` 별도 Spring Boot 앱 + `domain` 공유 모듈 + Gradle 멀티 모듈.
> 2026-05-12 LATER §"배치 모듈 분리" 결정 폐기 (학습 단계와 최종 구조 동일하게).

### 1-1. 배치 분리 인프라 (Step 0~5)

- [x] **Step 0** — ADR-014 작성 + TODO/99 정합 갱신
- [x] **Step 1** — 디렉토리 리네이밍 + 신규 모듈 골격 (`backend/` → `backoffice/`, `batch/` `domain/` 신규)
- [x] **Step 2** — Gradle 멀티 모듈 빌드 (root `settings.gradle` + 모듈별 `build.gradle` × 3)
- [x] **Step 3** — `domain` 모듈 추출 (Entity 19 + Repository 19 + BaseAuditEntity + AuditContext + ApiResponse/PageResponse + BusinessException/ErrorCode)
- [x] **Step 4** — batch 앱 스켈레톤 (`BatchApplication` + 포트 9093 + `application.yml`)
- [x] **Step 5** — backoffice → batch REST 트리거 (RestClient + X-Internal-Token + 더미 시나리오 2종) ← **다음 세션 시작 지점은 Step 6**

### 1-2. 배치 학습 본체 (Step 6~10)

- [x] **Step 6** — 배치 시나리오 정의 (`docs/100. 배치 시나리오 정의.md` — 7 시나리오 + Ch.1 6 라운드 계획)
- [ ] **Step 7** — **Ch.1 청구 배치 6 라운드 측정** (본 학습 본체 — bulk INSERT / chunk commit / UNDO / 메모리 / 재시작)
  - [x] **7-A** — 결정 + 시드 (`04-seed-perf-data.sql`) + DDL 갱신 (CHECK 제거, ROUND_NO/BATCH_PARAMS 추가). Step 5 CHECK 위반 발견·수정.
  - [x] **7-B 코드** — Strategy 패턴 (`BillingInsertStrategy` interface + `SingleSaveStrategy` R1 + `ChunkFlushClearStrategy` R2) + `BillingCreateService` (5만 contract 로드 → 전략 실행) + batch runner 분기 + 화면 BILLING_CREATE 카드/모달 (month/round 입력)
  - [x] **7-B 측정** — Docker 재기동 + 시드 + bootRun + 실 호출 → **R1 6,482ms / R2 21,543ms** (R2 가 R1 의 3.3배). 리포트: [`docs/perf-reports/2026-05-13-billing-create-r1-vs-r2.md`](perf-reports/2026-05-13-billing-create-r1-vs-r2.md). **버그 발견**: `em.clear()` 가 batchLog 도 detach → R2 STATUS 미갱신 (BL_BILLING 5만 행은 정상 INSERT).
  - [ ] **7-C** — R3~R6 + 버그 fix ← **다음 세션 시작 지점**
    - **선행** — detached batchLog 버그 fix (리포트 §3-3 A 안: strategy 후 `findById` 로 fresh reload, 또는 D 안: marking service REQUIRES_NEW 분리)
    - R3 real bulk INSERT (JdbcTemplate batchUpdate)
    - R4 chunk commit (트랜잭션 분할)
    - R5 UNDO 폭주 (Oracle UNDO tablespace 5MB 강제)
    - R6 멱등성 (UNIQUE 위반 처리 — SKIP / MERGE / catch+continue 비교)
- [ ] **Step 8** — UNDO 폭주 재현 환경 (Docker oracle 작은 UNDO tablespace 강제)
- [ ] **Step 9** — Ch.3 Kafka 통신 도입 (일부 토픽 비동기화 + Producer/Consumer 학습)
- [ ] **Step 10** — 도메인별 배치 메뉴 확장 (에너지 고객 동기 / 회계 정보 갱신 / 통계 집계)

---

## 2. NEXT — 다음 사이클

### 2-1. 자동화 도구 2차 도입 (ref-project 패턴 일부)

> ref-project 의 `/cache-refresh` 흐름 그대로 옮김. 도메인 작업 시 즉시 가치 발생.

- [x] **DB 스키마 캐시** (`docs/cache/{table,column,index}.txt`) — Oracle 메타테이블 (`USER_TAB_COMMENTS` / `USER_TAB_COLUMNS` / `USER_INDEXES`) 에서 자동 생성
- [x] **캐시 갱신 스크립트** `infra/cache/refresh.py` — `docker exec rental-oracle sqlplus` 호출 + 텍스트 파싱
- [x] **`/cache-refresh` 슬래시 커맨드**:
  - `.claude/commands/cache-refresh.md` (얇은 진입점)
  - `docs/commands/cache-refresh-spec.md` (두꺼운 spec)
- [x] 캐시를 사용하는 룰 신설 — `docs/global-rules/db-cache-pattern.md` (description: "DB 스키마 조회 시 캐시 우선")

### 2-2. 잔여 도메인 (단순 CRUD 묶음 — Phase 1)

- [x] 장비 (`CT_EQUIPMENT`) — 단순 CRUD + EQUIPMENT_TYPE selectbox **+ 재고 모델 STOCK_QTY (동적 계산)**
- [x] 상품 (`CT_PRODUCT`) — 장비 FK + 4 NUMBER 검증 (Positive/Min)
- [x] 계약 (`CT_CONTRACT`) — 자동 채번 + 재고 3중 검증 + 상태 전이 4종 + **별도 상세 페이지**
- [x] 기사 (`CT_ENGINEER`) — 단순 CRUD + INTERNAL/EXTERNAL + 지역 검색
- [x] 방문 이력 (`CT_VISIT`) — 기사별 일정 5건 초과 차단 + complete/cancel 상태 전이

### 2-3. 학습 핵심 — Ch.2 (Ch.1/Ch.3 은 NOW Step 7/9 로 이동)

- [ ] **Ch.2 통계 미납 엑셀** — 쿼리 튜닝 (서브쿼리 → JOIN + 인덱스 활용) + Oracle EXPLAIN PLAN 전/후 비교 + Apache POI SXSSF 스트리밍. 배치 모듈 학습 본체 (NOW Step 7~9) 후 진행.

### 2-4. 대시보드 + 알림

- [ ] 대시보드 — `@Scheduled` 집계 + Redis 캐시 (미납/연체/매출 위젯)
- [ ] 알림 목록 화면 (`CM_NOTIFICATION` 조회)

---

## 3. LATER — 언젠가 / 조건부

### 자동화 도구 3차 — 도메인 패턴 정형화 후

> 단순 CRUD 도메인 5개 (장비/상품/계약/기사/방문) + Kafka·배치 도메인 2개 (청구·수납) + 통계 1개 = **8개 도메인 만든 후** 진행. 그 시점에 패턴이 굳어져 spec 정확도 ↑.
> 추측 spec 으로 미리 작성 시 후속 수정 부담 큼.

- [ ] `/domain-spec {도메인}` — 도메인 작업지시서 생성 (코드 X, 명세만)
  - `.claude/commands/domain-spec.md` (얇은 진입점)
  - `docs/commands/domain-spec-spec.md` (두꺼운 spec — Entity/Repo/Service/Controller/DTO/Template/JS 패턴 그대로 박음)
- [ ] `/domain-build {도메인}` — 작업지시서 → 코드 일괄 생성
- [ ] `/domain-test {도메인}` — 검증 리포트 (코드 수정 X, 정적 분석 + SQL 실행 검증)
- [ ] **ADR-011** — 자동화 워크플로 결정 (도메인 단위 / Customer 패턴 표준 / Kafka·배치 도메인 sub-spec 분기)

### Phase 1 마무리 후 별도 사이클

- [ ] **JWT 인증 사이클** — Access/Refresh + JTI 블랙리스트 (강제 로그아웃) + `XUserIdAuthenticationFilter` 제거 (ADR-010 §2-4·§2-8)
- [ ] **Phase 2 — React 고객 포털** — 로그인 / 청구 조회 / Toss Payments 카드 납부 / 납부 내역 / 계약 현황
- [ ] **포털 권한 분리** — `/api/portal/**` 별도 인증 흐름 (SecurityConfig 의 TODO)

### 인프라 / 배포

- [ ] **Oracle Cloud Free Tier 배포** — Railway/Render 는 Oracle 미지원
- [ ] **CI/CD GitHub Actions** — 빌드/테스트 자동화
- [ ] **백오피스 v 포털 보안 분리** — SecurityConfig 의 TODO
- [ ] **배치 컨테이너화** — `rental-batch` Docker compose 추가 (학습 단계는 IDE 실행 OK, 운영 시점 컨테이너화)
- [ ] **Spring Batch 도입 검토** — chunk / restart / skip 추상화 학습 (배치 손구현 후 비교 — ADR-014 후속)

### 정책 검토

- [ ] **CM_USER_AUTH 화면 자체 권한 분리** — 현재 `USER_UPDATE` 로 묶음 (ADR-009 §2-4). 운영 시 `USER_AUTH_MANAGE` 별도 키 분리 검토
- [ ] **사용자 다역할 (M:N)** — 현재 `CM_USER.ROLE_ID` 단일 FK (ADR-009 §2-4). 필요 시 `CM_USER_ROLE` 테이블 추가

---

## 메모

- ref-project 자동화 시스템 분석 후 **점진적 도입** 결정 (2026-05-12). 풀세트 (1+2+3차) 대신 1차 → 패턴 정형화 → 3차 순서.
- 본 TODO 신설 자체가 1차 도입의 일부 (TODO + retro/ + claude-tuning/ 폴더).
- 2차 도구 (DB 캐시 + `/cache-refresh`) 가 가치 가장 큼 — 다음 도메인 작업 시 즉시 활용.
- **2026-05-12 2회차 — NEXT 2-2 5개 도메인 + 코드 구조 강화 (S1+S2) 완수.** 약 90+ 파일 변경.
  - ref-project 룰 4개 도입 (`command-creation` / `business-domain` / `api-safety` / `delete-defense`) — 실전 적용 (계약·방문 상태 전이 / 코드 시스템 그룹 차단)
  - `db-conventions §2-1` NUMBER NOT NULL DEFAULT 0 룰 신설
  - 코드 구조 강화: `CM_CODE_GROUP.SYSTEM_YN` + `CM_CODE.DESCRIPTION/PROP_VAL1~3` (ref-project sy_code_dtl 구조 차용)
  - **DDL/캐시 검증 미수행** (Docker 미가동) — 일과 끝 컨테이너 재생성 후 검증. 가정 시그니처 (CustomerRepository 등) 컴파일 오류 가능성.
- **2026-05-13 4회차 진입 — ADR-014 채택 (배치 모듈 분리).** 2026-05-12 LATER §"배치 모듈 분리" 결정 폐기. `rental-batch` 별도 Spring Boot 앱 + `domain` 공유 모듈 + Gradle 멀티 모듈. 학습 단계와 최종 구조 동일하게 가는 결정. 도메인별 배치 시나리오 학습 플랫폼화 (청구 / 연체 / 수납·연체 / 미납 통계 / 에너지 동기 / 회계 갱신 / 통계 집계).
- **2026-05-13 4회차 Step 5 완료** — backoffice ↔ batch 통신 뼈대. RestClient (Spring 6.1+) + X-Internal-Token 공유 시크릿 + @Async fire-and-forget. 더미 시나리오 (DUMMY_SUCCESS/FAIL) 로 success/failure 양 경로 검증. `/admin/batch-trigger` 페이지 + `BATCH_TRIGGER_*` 권한 시드. 컴파일 통과, 실 호출 검증은 다음 docker 재기동 후.
