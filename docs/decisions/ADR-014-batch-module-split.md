# ADR-014 — 배치 모듈 분리 (rental-batch)

상태: **Accepted** (2026-05-13)
영향 범위: 모노레포 디렉토리 구조 / Gradle 빌드 / 패키지 루트 / 포트 매핑 / Phase 1 학습 시나리오

---

## 컨텍스트

2026-05-12 1차 결정 (`TODO.md` LATER §"배치 모듈 분리"):

> 학습 단계는 단일 앱 유지, 운영 이행 시점 ADR 작성 후 분리

근거였음 — "도메인 코드 공유 / 통신 방식 / 멀티 모듈 빌드 결정 부담이 Phase 1 학습 본질(bulk INSERT 측정) 을 흐림".

**2026-05-13 결정 번복.** 사용자 인사이트:

- 결국 두 웹앱(backoffice + batch) 이 떠야 하는 게 명확해짐
- Phase 1 학습 본체가 청구 배치 1개에서 **배치 학습 플랫폼** 으로 확장 — 도메인별 다양 시나리오(월 청구 / 1분 회계 갱신 / 새벽 고객 동기 등) 를 메뉴로 구성
- 분리 자체가 Phase 1 학습의 일부 (멀티 모듈 / 도메인 공유 / REST·Kafka 통신 / 스케줄러)
- 학습 단계와 최종 구조를 동일하게 가는 게 더 명료

---

## 검토한 대안

### S0. 단일 앱 유지 (어제 결정) ❌ 폐기

장점: 학습 진척 빠름, Ch.1 본질에 집중
단점: 최종 구조와 학습 구조 불일치, 분리 사이클이 Phase 1 마무리 뒤로 밀림 — 그 시점에는 다시 마이그레이션 비용이 더 큼

### S1. Spring Batch 단일 사용 (배치 분리 X) ❌ 미채택

장점: Spring Batch 추상화(chunk / restart / skip) 학습
단점: 두 웹앱 운영 / 멀티 모듈 / 통신 패턴 학습 미충족. Spring Batch 는 LATER 후보로 둠.

### S2. 별도 Spring Boot 앱 (배치 분리) ✅ 채택

장점: 멀티 모듈 + 도메인 공유 + 통신 + 스케줄러 학습 + 운영 격리 패턴
단점: Phase 1 시작 전 인프라 작업 1~2회차 소요 (감수)

### S3. 별도 레포 (마이크로서비스 풀) ❌ 미채택

장점: 진짜 독립 배포 단위
단점: 모노레포 정체성 깨짐, 학습 범위 초과 (Phase 1 에는 과함)

---

## 결정

### 모노레포 구조

```
rental-crm/
├── infra/                      (Oracle / Kafka / Redis)
├── domain/             ★신규   라이브러리 모듈 — Entity / Repository / BaseEntity / 감사 컬럼 / 공통 응답·예외
├── backoffice/         ★개명   (옛 backend/) 사용자·장비·청구·수납 등 운영 화면
├── batch/              ★신규   배치 전용 Spring Boot 앱 — 강제실행 API + 자체 스케줄 + 도메인별 시나리오
├── customer-portal/            (Phase 2, 그대로)
└── docs/
```

### 패키지 루트

| 모듈 | 패키지 |
|---|---|
| domain | `com.rental.domain` |
| backoffice | `com.rental.backoffice` |
| batch | `com.rental.batch` |

근거: group 분리가 모듈 경계를 더 명시적으로 드러냄 (학습 목적과 정합). `com.rental.crm.*` 단일 root 도 정당하지만, 도메인 라이브러리 모듈 의도 강조 위해 group 분리.

### 포트 매핑

| 서비스 | 포트 | 비고 |
|---|---|---|
| backoffice | **9091** | 기존 |
| batch | **9093** | 신규 — 9092 는 Kafka broker 와 충돌 |

### 빌드

- Gradle 멀티 모듈: `settings.gradle` 에 4개 모듈 (domain, backoffice, batch, customer-portal)
- 각 모듈 `build.gradle` 독립. 공통 설정은 root `build.gradle` 로 추출.
- 의존: `backoffice → domain`, `batch → domain`. `backoffice ↔ batch` 직접 의존 없음 (통신은 REST/Kafka).

### 통신

| 시나리오 | 1차 (Ch.1) | 후순위 (Ch.3 Kafka 학습 후) |
|---|---|---|
| backoffice → batch 강제 실행 | REST `POST /batch/run` (비동기 fire-and-forget) | Kafka 요청 토픽 |
| batch → backoffice 진행/결과 | DB `BL_BATCH_LOG` 폴링 (backoffice 가 조회) | Kafka 응답 토픽 |
| batch 자체 스케줄 | Spring `@Scheduled` | (분산 시 ShedLock 검토) |

### 배치 시나리오 (도메인별 메뉴 — 가상 도메인명)

| 시나리오 | 학습 포인트 | 차수 |
|---|---|---|
| 청구 일괄 생성 (월 1회 새벽) | bulk INSERT 6 라운드 + chunk commit + 멱등성 + UNDO 폭주 | Ch.1 |
| 연체 발생 처리 (일 단위) | UPDATE/INSERT 혼합 + 트랜잭션 경계 | Ch.1 후속 |
| 수납·연체 Kafka (이벤트 기반) | Producer/Consumer + 멱등성 | Ch.3 |
| 미납 통계 엑셀 다운로드 | 쿼리 튜닝 + EXPLAIN PLAN + POI SXSSF | Ch.2 |
| 에너지 고객 동기 (새벽 INSERT) | 외부 시스템 동기 + bulk INSERT + 멱등성 | 학습 확장 |
| 회계 정보 갱신 (1분 주기) | 짧은 주기 스케줄러 + 락 회피 | 학습 확장 |
| 통계 집계 (일/주/월) | 집계 쿼리 + Redis 캐시 갱신 | 학습 확장 |

### 측정 리포트

- 형식: markdown 파일 + DB `BL_BATCH_LOG` 두 곳에 영속화
- markdown 위치: `docs/perf-reports/{YYYY-MM-DD}-{시나리오}-round{N}.md`
- DB: `BL_BATCH_LOG` 에 라운드 비교용 컬럼(`ROUND_NO` 등) 추가 — 후속 ADR 또는 본 ADR 적용 단계에서 결정

---

## 근거

1. **결정 전제 변화**: 두 웹앱 분리가 최종 구조라는 인식 확정 → 학습 단계와 최종 구조 일치가 학습 가치 큼.
2. **배치 학습 플랫폼화**: Ch.1 청구 1개에서 도메인별 다양 시나리오로 확장 → 단일 앱에 다 박으면 패키지·책임 비대. 분리하면 batch 가 시나리오 학습 플랫폼이 됨.
3. **운영 격리 패턴 학습**: 배치 부하가 backoffice 응답에 영향 안 줌 (학습용이지만 운영 패턴 학습).
4. **멀티 모듈 학습 가치**: domain 공유 모듈 추출 / Gradle 멀티 모듈 빌드 / 모듈 경계 학습.
5. **통신 방식 학습**: 1차 REST → Ch.3 Kafka 점진 도입 = 실무 마이크로서비스 통신 패턴 그대로.

---

## 영향 (사전 — Step 1 이후 발생)

| 영역 | 변경 |
|---|---|
| 디렉토리 | `backend/` → `backoffice/`, `batch/`·`domain/` 신규 |
| 패키지 | `com.rental.crm.*` (전 클래스) → `com.rental.{backoffice,batch,domain}.*` |
| Gradle | 단일 → 멀티 모듈 (`settings.gradle` + 4 `build.gradle`) |
| 룰 | `backend/guide/` → `backoffice/guide/`, `batch/guide/` 신설 |
| 문서 | `TODO.md` / `99. 업무현황.md` / `.claude/rules/project/general/system-info.md` / `.claude/rules/project/README.md` / `CLAUDE.md` |
| 포트 | 9093 신규 (`infra/.env` 또는 batch `application.yml`) |
| 빌드 명령 | `./gradlew :backoffice:bootRun` / `:batch:bootRun` |

---

## 후속 검토 사항

- **domain 의 web/jackson 의존성 정리** (Step 2~4 적용 중 발견, 2026-05-13)
  - 현 상태: `domain/build.gradle` 이 `spring-web` + `jackson-annotations` 까지 잡아당김
  - 원인: `ErrorCode.java` 의 `HttpStatus`, `ApiResponse.java` 의 `@JsonInclude(NON_NULL)`
  - 정공법: (B) `ErrorCode` 에서 `HttpStatus` 제거 → 각 앱 `GlobalExceptionHandler` 에서 매핑 + (C) `ApiResponse` 를 각 앱으로 되돌림 (response wrapper = web layer 책임)
  - 학습 가치: 도메인 vs 인프라 책임 경계
  - 진행 시점: 별도 사이클 (현 사이클은 진척 우선 옵션 A 채택)
- `domain` 모듈의 Spring Boot 의존성 범위 최소화 (transitive 깔끔히 — JPA / Validation 정도만)
- backoffice·batch 가 같은 Oracle 인스턴스를 별도 DataSource 로 쓰는 안전성 검증 (커넥션 풀 사이즈 / 트랜잭션 격리)
- `BL_BATCH_LOG` 측정 비교용 컬럼 추가 (`ROUND_NO` 등) — 본 ADR 적용 단계에서 결정
- 누락 번호 `ADR-005` / `ADR-006` 별도 정리 사이클 (본 ADR 무관)
- Spring Batch 자체 도입 (chunk / restart / skip 추상화 학습) — LATER 후보
