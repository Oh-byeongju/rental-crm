# `.claude/rules/` — 항상 로드되는 룰 (헌법)

> 매 세션 자동 로드되는 **전역 필수 룰**. Claude 가 무조건 알아야 하는 메타·식별자·짧은 컨벤션.
> 작업 시점에만 필요한 룰은 [`docs/global-rules/`](../../docs/global-rules/) (전역) 또는 [`{backend,customer-portal,infra}/guide/`](../../) (로컬) 에 있다.

---

## 폴더 구조

```
.claude/rules/
├── _meta/        ← Claude 작업 메타 (모든 작업에 영향)
├── frame/        ← 짧은 코드 컨벤션 (이식 가능 — 다른 프로젝트에도 그대로 적용)
└── project/      ← 현재 프로젝트 식별자·시스템 정보 (이식 시 교체)
```

각 디렉토리는 **항상 로드**. 무겁거나 작업 시점에만 필요한 룰은 여기 두지 않는다.

## 각 영역

### `_meta/` — Claude 작업 메타룰

코딩이든 정리든 리팩터든 **모든 작업에 적용**되는 메타 룰. 자동 로드.
일부는 특정 작업 시점에만 적용 — `description` frontmatter 매칭으로 Read.

- `communication.md` — 대화 원칙 (객관성 우선·Yes맨 방지) ✅ 자동 로드
- `file-operations.md` — 파일·폴더 작업 시 영향 처리 ✅ 자동 로드
- `rule-management.md` — 룰을 다루는 룰 (위치 결정·등록 동기화·충돌 우선순위) ✅ 자동 로드
- `command-creation.md` — 새 슬래시 커맨드 + spec 자동화 체계 작성 시 ❌ 작업 시점 Read

### `frame/` — 짧고 항상 적용되는 코드 컨벤션

작업 종류와 무관하게 늘 알고 있어야 하는 짧은 컨벤션. 이식 시 그대로 들고 감.

- `frame/general/method-order.md` — Service / Repository / Controller CRUD 메서드 순서
- `frame/db/sql-query.md` — SQL 쿼리 룰 (`SELECT *` 금지·인덱스·N+1)

### `project/` — 현재 프로젝트 식별자·시스템 정보

이식 시 새 프로젝트 식별자로 교체되는 **항상 알아야 하는 사실 자료**.

- `project/README.md` — **프로젝트 매핑 표** (식별자·테이블 접두사·코드 도메인 등 — 이식 시 변경 포인트)
- `project/general/system-info.md` — 빌드 명령·기술 스택·아키텍처·환경 프로파일
- `project/db/business-domain.md` — 업무 도메인 prefix → 의미 매핑 (테이블 / 시퀀스 / 패키지 설계 시)

---

## 3-tier 분담

| | `.claude/rules/` | `docs/global-rules/` | `{디렉토리}/guide/` |
|---|---|---|---|
| 의미 | **헌법 — 무조건 따른다** | **전역 작업 시점 룰** | **로컬 작업 시점 룰** |
| 로드 | 매 세션 자동 로드 | 명시 Read (description 매칭) | 명시 Read (description 매칭) |
| 적용 범위 | 모든 작업 | 여러 디렉토리에 영향 | 해당 디렉토리만 |
| 예시 | 메타룰·SQL 기본 룰·식별자 매핑 | API 응답 포맷·Kafka 토픽 설계·ERD 변경 절차 | JPA 컨벤션·React 컨벤션·Docker 설정 |

새 룰을 만들 때 결정 절차: [`_meta/rule-management.md`](_meta/rule-management.md)

---

## 새 디렉토리 추가 시 — 본 README 갱신 필수

`.claude/rules/` 하위에 새 영역이 추가되면 **본 README 의 "폴더 구조" 와 "각 영역" 섹션을 반드시 갱신**.
