# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

---

## 프로젝트 시스템 정보

빌드·기술스택·아키텍처·환경·보안 등 매 세션 알아둘 컨텍스트.

- 시스템 정보: @.claude/rules/project/general/system-info.md
- DB 도메인 prefix 매핑: @.claude/rules/project/db/business-domain.md

## 룰 진입점 (3-tier 구조)

| 위치 | 의미 | 로드 |
|---|---|---|
| `.claude/rules/` | **헌법 — 무조건 따른다** (메타·식별자·짧은 컨벤션) | 매 세션 자동 로드 |
| `docs/global-rules/` | **전역 작업 시점 룰** (여러 디렉토리에 영향) | 명시 Read |
| `{backend,customer-portal,infra}/guide/` | **로컬 작업 시점 룰** (해당 디렉토리만) | 명시 Read |

- 항상 로드되는 룰 인덱스: @.claude/rules/README.md
- 프로젝트 매핑 (식별자 표): @.claude/rules/project/README.md
- 전역 작업 시점 룰 인덱스: [`docs/global-rules/README.md`](docs/global-rules/README.md)
- 로컬 룰 인덱스:
  - 백엔드: [`backend/guide/README.md`](backend/guide/README.md)
  - 고객 포털: [`customer-portal/guide/README.md`](customer-portal/guide/README.md)
  - 인프라: [`infra/guide/README.md`](infra/guide/README.md)

## 작업 메타룰 (모든 작업에 적용)

- 룰 관리: @.claude/rules/_meta/rule-management.md
- 파일/폴더 작업 시 영향 처리: @.claude/rules/_meta/file-operations.md
- 대화 원칙 (객관성 우선): @.claude/rules/_meta/communication.md

## 산출물 위치

| 자산 | 위치 |
|---|---|
| 기획~배포 산출물 (01~99 .md) | [`docs/`](docs/) |
| 전역 ADR (의사결정 기록) | [`docs/decisions/`](docs/decisions/) |
| 로컬 ADR (도메인 한정 결정) | `{디렉토리}/guide/decisions/` |
| 개인 메모 | [`docs/notes/`](docs/notes/) |

## 작업 시점 룰 라우팅

코딩 룰은 **자동 로드되지 않음**. Claude 는 작업 시작 시 다음 흐름으로 룰을 찾는다:

1. 작업이 어느 디렉토리/도메인에 속하는지 판단
2. 해당 위치의 인덱스 README 를 Read (전역 또는 로컬)
3. 각 룰의 `description` frontmatter 로 매칭
4. 매칭된 룰만 Read

**원칙**: 추측해서 모든 룰을 불러오지 않는다. 작업 성격에 맞는 것만 매칭.
