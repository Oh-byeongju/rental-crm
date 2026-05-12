# 시스템 정보 — rental-crm

> 빌드 명령·기술 스택·아키텍처·환경 프로파일. **매 세션 자동 로드**.

---

## 기술 스택

| 영역 | 기술 | 버전 |
|---|---|---|
| 언어 | Java | 21 (LTS, 프로젝트 단위 — 시스템 JAVA_HOME 은 17 유지) |
| 백엔드 | Spring Boot | 3.x |
| ORM | Spring Data JPA (Hibernate) | - |
| DB | Oracle | 21c (Docker `gvenzl/oracle-free`) |
| 메시지 브로커 | Apache Kafka | (Spring Kafka) |
| 캐시 | Redis | 7-alpine |
| 백오피스 템플릿 | Thymeleaf + **Tabler (Bootstrap 5)** | - |
| 백오피스 그리드 | **AG Grid Community** (MIT) | v32+ |
| 고객 포털 | React + **Tailwind CSS + shadcn/ui** | 18 / 최신 |
| 고객 포털 그리드 | **TanStack Table v8** (shadcn/ui `DataTable`) | v8 |
| 고객 포털 상태 | Zustand | - |
| 고객 포털 HTTP | Axios | - |
| 결제 | Toss Payments SDK | - |
| 엑셀 | Apache POI (SXSSF) | - |
| API 문서 | springdoc (Swagger) | - |
| 빌드 | Gradle | - |
| 컨테이너 | Docker + docker-compose | - |
| CI/CD | GitHub Actions | - |

---

## 디렉토리 구조

```
rental-crm/
├── .claude/rules/                  ← 헌법 (자동 로드)
├── docs/                           ← 설계 산출물 + 전역 룰
│   ├── 01~99. *.md                 ← 산출물
│   ├── decisions/                  ← 전역 ADR
│   ├── notes/
│   └── global-rules/               ← 전역 작업 시점 룰
├── infra/                          ← Oracle / Kafka / Zookeeper / Redis
│   ├── guide/                      ← infra 로컬 룰
│   └── docker-compose.yml
├── backend/                        ← Spring Boot (백오피스 Thymeleaf + REST API)
│   ├── guide/                      ← backend 로컬 룰
│   └── src/
└── customer-portal/                ← React 고객 포털
    ├── guide/                      ← portal 로컬 룰
    └── src/
```

---

## 빌드 / 실행 명령 (예정)

### 인프라 기동
```bash
cd infra
docker compose up -d
# Windows: infra/start.bat 더블클릭
```

### 백엔드
```bash
cd backend
.\gradlew bootRun
# 또는
.\gradlew build && java -jar build/libs/*.jar
```

> Java 21 경로 (사용자별 — 노트북 이주 시 갱신): `D:\Dev\JDK\openjdk-21+35_windows-x64_bin`
> 시스템 JAVA_HOME 은 17 유지. `backend/gradle.properties` 의
> `org.gradle.java.installations.paths` 가 21 경로를 가리킴.
> IntelliJ 도 동일 경로를 Gradle JDK 로 설정.

### 고객 포털
```bash
cd customer-portal
npm install
npm run dev
```

> ⚠️ 위 명령은 **예정**. 실제 스켈레톤 생성 후 명령이 확정되면 본 문서 갱신 필요.

---

## 환경 프로파일

| 프로파일 | 용도 | 파일 |
|---|---|---|
| `local` | 로컬 개발 (Docker 컨테이너 사용) | `application-local.yml` (Git 제외) |
| `dev` | 개발 서버 | `application-dev.yml` |
| `prod` | 운영 (Oracle Cloud Free Tier 검토 중) | `application-prod.yml` |

---

## 핵심 아키텍처 결정

| 결정 | 이유 | 산출물 |
|---|---|---|
| 모노레포 | 풀스택 변경 PR 한 번에 묶기 | - |
| 백오피스 + API = 단일 Spring Boot 앱 | 도메인 코드 공유, 멀티모듈 오버엔지니어링 회피 | - |
| 인프라 docker-compose 별도 | 앱 재빌드와 DB 라이프사이클 분리 | `infra/docker-compose.yml` |
| JPA 선택 (MyBatis 아님) | 객체 중심 ORM 학습 + 벌크 연산 패턴 학습 | `docs/03. 기술 스택 정의서.md` |
| `BL_BILLING.CUSTOMER_ID` 역정규화 | 청구 조회 시 JOIN 비용 절감 | `docs/06. ERD 및 테이블 정의서.md` |

---

## 학습 시나리오 (3챕터)

| 챕터 | 목표 | 실무 매핑 |
|---|---|---|
| Ch.1 | bulk INSERT 5만 건 성능 측정 (건별 vs 일괄, Oracle MERGE 멱등성) | 경남에너지 SAP EAI 매일 3~5만 건 INSERT |
| Ch.2 | 쿼리 튜닝 (서브쿼리 → JOIN, 실행계획 비교) + 엑셀 스트리밍 | 부산상수도 자산관리 수십만 건 조회 + 다운로드 |
| Ch.3 | Kafka Producer/Consumer 양방향 토픽 + 멱등성 | 경북연구원 A10 연계 + 경남에너지 SAP EAI |

---

## 참고 프로젝트 경로

> 다른 프로젝트의 룰·패턴·산출물을 참조할 때 사용. **rental-crm 과 ref-project 는 항상 같은 부모 폴더에 형제로 둔다** (PC 간 이동 시 본 섹션 갱신 불필요).

| 항목 | 값 |
|---|---|
| 참고 프로젝트 루트 (상대) | `../ref-project` (rental-crm 루트 기준) |

### 전제 구조

두 PC 모두 다음 형태를 유지:

```
{어딘가}/                     ← 부모 폴더 (드라이브·상위 경로는 PC마다 달라도 OK)
├── rental-crm/
└── ref-project/
```

### 사용 규칙

- 사용자가 "GDI 룰 봐바", "참고 프로젝트의 XX 봐줘" 같이 말하면 `{rental-crm 루트}/../ref-project` 하위에서 찾는다
- Claude 는 작업 디렉토리(워크트리 포함) 와 무관하게 **rental-crm 루트 기준** 으로 해석한다
- 거기서 가져올 만한 룰/패턴이 발견되면, **rental-crm 도메인에 맞춰 변형 후 복사** 하는 게 원칙 (단순 경로 참조는 깨짐 위험)
- 복사한 룰은 출처를 본문에 명시 (예: `> 출처: 참고 프로젝트의 audit-columns.md (rental-crm 도메인에 맞춰 변형)`)

### PC 이동 시 체크리스트

1. 새 PC 에서 `rental-crm` 옆에 `ref-project` 가 형제로 있는지 확인
2. 없으면 동일 구조로 배치 (본 문서 갱신 불필요)
