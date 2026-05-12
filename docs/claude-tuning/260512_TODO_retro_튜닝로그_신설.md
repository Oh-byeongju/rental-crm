# 260512 — TODO / retro / claude-tuning 폴더 신설

태그: `tooling`

---

## 배경

ref-project (참고 프로젝트) 의 자동화 시스템 분석:

```
guide/
├── 04. modules/          ← 화면별 작업지시서 산출물
├── 05. retro/            ← 회고
└── 06. claude-tuning/    ← Claude 운영 튜닝 로그 (YYMMDD_주제.md)
TODO.md                   ← NOW / NEXT / LATER 3단
```

`.claude/commands/` + `guide/02. commands/spec.md` 두 트랙 (테이블 트랙 + 화면 트랙) 자동화도 발견. 하지만 도구 spec 자체가 작성에 수 시간 걸리고, 본 프로젝트(rental-crm)는 단순 CRUD 외 Kafka·배치·통계 패턴이 섞여 있어 **추측 spec 위험**이 있음.

## 결정

ref-project 도구를 **풀세트가 아니라 점진적 도입**.

| 차수 | 항목 | 시점 |
|---|---|---|
| 1차 (즉시) | `TODO.md` + `retro/` + `claude-tuning/` 폴더 신설 | 2026-05-12 (본 로그) |
| 2차 | DB 스키마 캐시 + `/cache-refresh` 슬래시 커맨드 | 다음 도메인 작업 직전 |
| 3차 | `/domain-spec` + `/domain-build` + `/domain-test` 자동화 | 도메인 8개 만든 후 (패턴 정형화 후) |

3차를 미루는 이유 — 단순 CRUD 도메인 5개 + Kafka·배치 도메인 2개 + 통계 도메인 1개 = 8개 만든 후 spec 정확도가 *경험 기반*으로 굳어짐. 추측 spec 으로 미리 작성하면 후속 수정 부담이 큼.

## 1차 변경 내용

1. `docs/TODO.md` 신설 — NOW/NEXT/LATER 3단. 2차/3차 작업도 LATER 에 미리 명시.
2. `docs/retro/README.md` 신설 — 회고 디렉토리 안내. `YYMMDD_주제.md` 파일명 규칙.
3. `docs/claude-tuning/README.md` 신설 — 본 폴더 자체. 동일 규칙.
4. `docs/99. 업무현황.md` — "다음 세션 할 일" 섹션을 TODO.md 참조로 단순화. 99 는 *완료 보고용*, TODO 는 *작업 예정 추적용*.

## 역할 분담

| 문서 | 역할 |
|---|---|
| ADR (`docs/decisions/`) | 비즈니스·기술 *결정* (의사결정 시점) |
| Retro (`docs/retro/`) | 결정 후 *사후 평가* (작업해보니 어땠는지) |
| Claude 튜닝 (`docs/claude-tuning/`) | Claude 운영 환경 *변경 이력* (메타 작업) |
| 99 업무현황 | *완료 보고* 스냅샷 (매 사이클 마무리) |
| TODO | *작업 예정* 추적 (NOW/NEXT/LATER) |

## 결과

- 도구 부담 낮음 (1시간 내 도입)
- 2차/3차 미래 작업 TODO 에 명시 — 잊지 않음
- 패턴 정형화 후 3차 진행 — spec 정확도 ↑

## 후속

- 2차 도입 시점: 다음 도메인 (장비/상품) 작업 시작 직전
- 3차 도입 시점: 도메인 8개 만든 후
- ADR-011 작성 예정 (3차 시점에 자동화 워크플로 결정 명문화)
