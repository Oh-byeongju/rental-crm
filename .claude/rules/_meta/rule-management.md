# 룰 관리 메타룰

> 룰을 **추가/수정/삭제** 할 때 어느 세션이든 따르는 절차.
> 이 파일 자체는 CLAUDE.md `## 작업 메타룰` 섹션의 `@import` 로 매 세션 자동 로드된다.

---

## 적용 시점

사용자 요청에 다음 의도가 보일 때:

- 룰 추가/생성 ("규칙 만들어줘", "이거 룰로 박아줘")
- 룰 변경 ("이 룰 바꿔", "조건 추가해")
- 룰 삭제 ("이 룰 빼", "이제 안 써")
- 룰 점검 ("지금 어떤 룰 적용 중이야?")

---

## 1. 위치 결정 — 3-tier 구조

```
.claude/rules/                   ← 헌법 (자동 로드, 매 세션)
├── _meta/                       ← 작업 메타룰
├── frame/                       ← 짧고 항상 적용 (이식 가능 컨벤션)
└── project/                     ← 식별자·시스템 정보 (이식 시 교체)

docs/global-rules/               ← 전역 룰 (작업 시점 Read, 모든 도메인 적용)
└── ...                          ← API 응답 포맷, Kafka 토픽 설계 등

{backoffice,customer-portal,infra}/guide/  ← 로컬 룰 (작업 시점 Read, 해당 디렉토리만)
└── ...                          ← JPA 컨벤션, React 컨벤션, Docker 설정 등
```

### 1-1. 결정 절차

**Step 1 — 자동 로드 필요?** (모든 세션에 짧고 항상 적용)

- Yes → `.claude/rules/`
  - 메타룰(작업 방식)? → `_meta/`
  - 짧은 코드 컨벤션 (모든 작업 공통, ~50줄)? → `frame/`
  - 프로젝트 식별자·스택? → `project/`
- No → Step 2

**Step 2 — 적용 범위가 여러 디렉토리?**

- Yes (backoffice + portal + infra 둘 이상에 영향) → `docs/global-rules/`
- No (한 디렉토리에만 적용) → Step 3

**Step 3 — 어느 디렉토리에 속함?**

- backoffice 코드/도메인 룰 → `backoffice/guide/`
- portal 컨벤션 → `customer-portal/guide/`
- infra 설정 → `infra/guide/`

### 1-2. 헌법화 기준

| 기준 | 위치 |
|---|---|
| 짧고 (~50줄) + 거의 모든 작업에 적용 | `.claude/rules/frame/` |
| 길거나 (100줄+) + 특정 시점에만 적용 | `docs/global-rules/` 또는 `{도메인}/guide/` |

### 1-3. 점진적 추상화 원칙

지금 모르는 미래("portal에도 똑같이 쓸지 몰라") 를 미리 global 로 추상화하지 않는다.

- 첫 번째 케이스 → 해당 디렉토리 `guide/` 에 둔다 (로컬)
- 두 번째 디렉토리에서 동일 패턴이 재현될 때 → `docs/global-rules/` 로 끌어올린다

YAGNI. 잘못된 추상화보다 늦은 추상화가 낫다.

범위가 애매하면 사용자에게 묻는다 — 추측해서 진행하지 않는다.

---

## 2. 등록 동기화 (필수)

룰 파일을 **생성/삭제할 때 어디에 등록되어야 하는지** 는 룰 종류에 따라 다르다.

| 룰 종류 | 등록처 | 자동 로드? |
|---|---|---|
| `.claude/rules/_meta/*` (항상 적용) | **CLAUDE.md `## 작업 메타룰`** 섹션에 `@import` | ✅ 매 세션 |
| `.claude/rules/_meta/*` (작업 시점) | 파일 첫 줄에 `description` frontmatter (등록처 없음) | ❌ 작업 시점 Read |
| `.claude/rules/frame/*` · `.claude/rules/project/*` | **CLAUDE.md 해당 섹션** 에 `@import` | ✅ 매 세션 |
| `docs/global-rules/*` (전역 작업 시점) | **`docs/global-rules/README.md` 인덱스 표** 에 행 추가 + 파일 첫 줄에 `description` frontmatter | ❌ 작업 시점 Read |
| `{도메인}/guide/*` (로컬 작업 시점) | **해당 `guide/README.md` 인덱스 표** 에 행 추가 + 파일 첫 줄에 `description` frontmatter | ❌ 작업 시점 Read |
| 새 영역 디렉토리 (`.claude/rules/`) | `.claude/rules/README.md` 폴더 구조 + 각 영역 섹션 | — |

### 핵심 규칙

- **헌법 (`.claude/rules/`)** → CLAUDE.md `@import` 또는 자동 로드.
- **작업 시점 룰 (`docs/global-rules/` · `{도메인}/guide/`)** → CLAUDE.md `@import` **금지**. 의도된 lazy-load (context 절감) 정책. 대신 README 인덱스 + frontmatter `description` 으로 라우팅.

### `description` frontmatter 형식

```yaml
---
description: "<언제 보는지 한 줄 — 예: JPA Repository 작성 시 — 메서드 명명 컨벤션>"
---

# 룰 본문 ...
```

Claude 가 해당 인덱스 README 와 각 룰의 description 을 보고 매칭되는 룰만 Read.

> ⚠️ **등록 누락 = 룰이 안 보임.** 파일만 만들고 등록 빼먹으면 "있긴 한데 작동 안 함" 안티패턴.

---

## 3. 충돌 우선순위

같은 주제에 두 룰이 모순되면:

1. **더 좁은 스코프가 우선**: 로컬(`{도메인}/guide/`) > 전역(`docs/global-rules/`) > 헌법(`.claude/rules/frame/`)
2. **project 가 frame 을 덮어쓴다**: 같은 도메인에서 frame 의 일반 패턴과 project 의 구체가 모순되면 project 우선 (구체화 layer)
3. 같은 스코프에서 충돌하면 사용자에게 물어 확정
4. 확정 결과를 룰 본문에 반영해 차기 충돌 방지

---

## 4. 즉시 반영 한계

룰 파일을 방금 수정했어도 **현재 세션은 옛 컨텍스트로 동작** 할 수 있다.

즉시 적용이 필요한 작업이면 사용자에게 안내:

- 새 세션 시작, 또는
- "방금 바꾼 룰 다시 읽고 따라줘" 라고 명시 요청

---

## 5. 작업 절차 (요약)

1. 사용자 요청 파싱 → 적용 범위 추정 → §1 결정 절차로 위치 결정
2. 파일 생성/수정/삭제
3. **등록 동기화** (§2) — 룰 종류에 맞는 등록처에 추가/제거
4. **새 영역 디렉토리 추가 시** `.claude/rules/README.md` 또는 해당 인덱스 README 갱신
5. 결과 보고:
   - 변경된 파일 경로
   - 추가/제거된 등록 라인
6. 즉시 반영 필요 여부 안내 (§4)

---

## 6. 권장 패턴

- 한 파일 = 한 책임. 여러 주제가 섞이면 분해한다.
- 폴더 안 파일이 많아지면 `README.md` 로 인덱스를 만든다.
- 파일 머리에 **`description` frontmatter** 박기 (작업 시점 룰의 경우 필수).
- 룰끼리 의존하면 명시적으로 참조 (예: "관련: §X" / 다른 룰 파일 경로).
