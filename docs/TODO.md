# TODO

> 3단 체계: NOW (현재) → NEXT (이후) → LATER (언젠가).
> 항목은 자유롭게 단계 사이로 옮긴다.
>
> **99 업무현황** 은 *완료 보고용*, **본 TODO** 는 *작업 예정 추적용*. 역할 분리.

---

## 1. NOW — 현재 사이클

> 지금 손대고 있거나 즉시 시작할 작업.

(현재 NOW 비어 있음 — 다음 작업 NEXT 에서 끌어올림.)

---

## 2. NEXT — 다음 사이클

### 2-1. 자동화 도구 2차 도입 (ref-project 패턴 일부)

> ref-project 의 `/cache-refresh` 흐름 그대로 옮김. 도메인 작업 시 즉시 가치 발생.

- [ ] **DB 스키마 캐시** (`docs/cache/{table,column,index}.txt`) — Oracle 메타테이블 (`USER_TAB_COMMENTS` / `USER_TAB_COLUMNS` / `USER_INDEXES`) 에서 자동 생성
- [ ] **캐시 갱신 스크립트** `infra/cache/refresh.py` — `docker exec rental-oracle sqlplus` 호출 + 텍스트 파싱
- [ ] **`/cache-refresh` 슬래시 커맨드**:
  - `.claude/commands/cache-refresh.md` (얇은 진입점)
  - `docs/commands/cache-refresh-spec.md` (두꺼운 spec)
- [ ] 캐시를 사용하는 룰 신설 — `docs/global-rules/db-cache-pattern.md` (description: "DB 스키마 조회 시 캐시 우선")

### 2-2. 잔여 도메인 (단순 CRUD 묶음 — Phase 1)

- [ ] 장비 (`CT_EQUIPMENT`) — 단순 CRUD + EQUIPMENT_TYPE 코드 selectbox
- [ ] 상품 (`CT_PRODUCT`) — 장비 FK + 금액 컬럼
- [ ] 계약 (`CT_CONTRACT`) — 일시정지/해지 `EXECUTE` 권한 + 상태 전이
- [ ] 기사 (`CT_ENGINEER`) — 지역별 가용 기사 조회 (`IDX_CT_ENGINEER_AREA`)
- [ ] 방문 이력 (`CT_VISIT`) — 방문 배정/완료 + 기사별 일정 5건 초과 검증 (04 §5-1)

### 2-3. 학습 핵심 3챕터

- [ ] **Ch.1 청구** — 월 청구 일괄 생성 배치 + 건별 INSERT vs bulk INSERT 성능 측정 → `BL_BATCH_LOG.DURATION_MS` 기록 → README `📈 성능 측정 결과` 채우기
- [ ] **Ch.2 통계 미납 엑셀** — 쿼리 튜닝 (서브쿼리 → JOIN + 인덱스 활용) + Oracle EXPLAIN PLAN 전/후 비교 + Apache POI SXSSF 스트리밍
- [ ] **Ch.3 수납 + 연체 Kafka** — Producer/Consumer 양방향 토픽 + 멱등성 (Idempotent Consumer + offset commit 정책) + `CM_NOTIFICATION` INSERT

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

### 정책 검토

- [ ] **CM_USER_AUTH 화면 자체 권한 분리** — 현재 `USER_UPDATE` 로 묶음 (ADR-009 §2-4). 운영 시 `USER_AUTH_MANAGE` 별도 키 분리 검토
- [ ] **사용자 다역할 (M:N)** — 현재 `CM_USER.ROLE_ID` 단일 FK (ADR-009 §2-4). 필요 시 `CM_USER_ROLE` 테이블 추가

---

## 메모

- ref-project 자동화 시스템 분석 후 **점진적 도입** 결정 (2026-05-12). 풀세트 (1+2+3차) 대신 1차 → 패턴 정형화 → 3차 순서.
- 본 TODO 신설 자체가 1차 도입의 일부 (TODO + retro/ + claude-tuning/ 폴더).
- 2차 도구 (DB 캐시 + `/cache-refresh`) 가 가치 가장 큼 — 다음 도메인 작업 시 즉시 활용.
