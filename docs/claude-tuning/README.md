# Claude 작업 환경 튜닝 로그

> Claude 코드 / 도구 / 룰 / 디렉토리 구조 *변경 이력*. 메타 작업 기록.

---

## 사용 방법

- 파일명: `YYMMDD_주제.md` (예: `260512_TODO도입_retro_튜닝로그_신설.md`)
- 기록 단위:
  - `.claude/rules/` 변경
  - `docs/global-rules/` 추가/제거
  - 슬래시 커맨드 도입/변경
  - 디렉토리 구조 재편
  - 메타룰 변경

## ADR / Retro 와의 차이

| | ADR | Retro | Claude 튜닝 |
|---|---|---|---|
| 대상 | 비즈니스·기술 결정 | 작업 사후 평가 | Claude 운영 환경 변경 |
| 위치 | `docs/decisions/` | `docs/retro/` | `docs/claude-tuning/` |
| 예 | "권한 모델 AUTH 키 단위" | "AUTH 키 모델 2주 사용 평가" | "TODO.md 도입 / claude-tuning 폴더 신설" |

---

> 본 디렉토리 신설: 2026-05-12 — ref-project 의 `guide/06. claude-tuning/` 패턴 도입.
