# `customer-portal/guide/` — 고객 포털 로컬 룰 (인덱스 / 라우터)

> React 고객 포털 작업에만 적용되는 작업 시점 룰. Claude 가 자동 로드하지 않음.
> 작업 시작 시 본 인덱스 + 각 룰의 `description` frontmatter 매칭으로 필요한 룰만 Read.

---

## 룰 인덱스

> 현재 비어 있음. 코드 작성 시작 후 첫 패턴이 정립되면 추출.

| 룰 | description | 파일 |
|---|---|---|
| (없음) | | |

---

## 후보 (코드 진입 시 추가될 가능성)

- 폴더 구조 (features / components / hooks / stores 등)
- Zustand 스토어 컨벤션 (전역 인증/알림 분리)
- Axios 인스턴스 + 인터셉터 (JWT 자동 첨부 / 401 처리)
- API 호출 레이어 (api/* 디렉토리 패턴)
- 라우팅 가드 (인증 필요 페이지)
- Toss Payments SDK 연동 패턴 (결제 위젯 + 검증 API + 중복 방지)
- 에러 바운더리 / 토스트 알림

---

## decisions/

portal 한정 의사결정 기록 (ADR). 전역 영향 결정은 [`docs/decisions/`](../../docs/decisions/) 에 둔다.

---

## 새 룰 추가 시

1. 본 README 인덱스 표에 행 추가
2. 룰 파일 첫 줄에 `description` frontmatter
3. 두 번째 디렉토리(backend/infra) 에서도 같은 패턴이 등장하면 → **전역 룰** (`docs/global-rules/`) 로 승격 검토

상세 절차: [`@.claude/rules/_meta/rule-management.md`](../../.claude/rules/_meta/rule-management.md)
