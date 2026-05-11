# `backend/guide/` — 백엔드 로컬 룰 (인덱스 / 라우터)

> Spring Boot 백엔드 작업에만 적용되는 작업 시점 룰. Claude 가 자동 로드하지 않음.
> 작업 시작 시 본 인덱스 + 각 룰의 `description` frontmatter 매칭으로 필요한 룰만 Read.

---

## 룰 인덱스

| 룰 | description | 파일 |
|---|---|---|
| 백오피스 화면 구현 규칙 | 백오피스 화면(Thymeleaf + Tabler + AG Grid) 작성 시 — 그리드 컬럼 자동 확장 / 검색 폼 잘림 방지 / 모달 패턴 | [conventions/backoffice-ui-rules.md](conventions/backoffice-ui-rules.md) |
| 감사 컬럼 자동 주입 | JPA 엔티티 작성 시 — 9컬럼 자동 주입 메커니즘 | [decisions/ADR-005](decisions/ADR-005-audit-columns-auto-injection.md) |
| Spring Boot 버전 | Spring Boot 신규 의존성 검토 시 — 3.5.0 채택 근거 | [decisions/ADR-006](decisions/ADR-006-spring-boot-version.md) |

---

## 후보 (코드 진입 시 추가될 가능성)

- 패키지 구조 (`com.rental.crm.{domain}.{controller|service|repository|entity}`)
- JPA 엔티티 컨벤션 (감사 컬럼·연관관계·Lombok 사용 범위)
- Repository 명명 / 쿼리 메서드 컨벤션
- Service 트랜잭션 경계 정책
- DTO 명명 (`*Request`, `*Response`, `*Command`)
- 예외 계층 / `@ControllerAdvice` 정책
- 청구 도메인 룰 (배치 멱등성·상태 전이)
- 수납 도메인 룰 (트랜잭션 + Kafka 발행 순서)

---

## decisions/

backend 한정 의사결정 기록 (ADR). 전역 영향 결정은 [`docs/decisions/`](../../docs/decisions/) 에 둔다.

---

## 새 룰 추가 시

1. 본 README 인덱스 표에 행 추가
2. 룰 파일 첫 줄에 `description` frontmatter
3. 두 번째 디렉토리(portal/infra) 에서도 같은 패턴이 등장하면 → **전역 룰** (`docs/global-rules/`) 로 승격 검토

상세 절차: [`@.claude/rules/_meta/rule-management.md`](../../.claude/rules/_meta/rule-management.md)
