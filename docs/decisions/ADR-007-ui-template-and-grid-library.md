# ADR-007 — UI 템플릿 + 그리드 라이브러리 채택

- **작성일**: 2026-05-11
- **상태**: 확정
- **연관**:
  - `docs/03_기술 스택 정의서.md` §3 갱신
  - `docs/global-rules/frontend-grid-library.md` (Webix 금지 룰)
  - `docs/05_화면 설계서.md` 작성 기반

---

## 1. Context

산출물 03 초안엔 "Bootstrap 또는 AdminLTE", "Ant Design 또는 MUI" 같은 선택지가 모호하게 남아 있었음.
참고 프로젝트(GDI)에서 본 ERP 백오피스 그리드 패턴 — 정보 밀도 높고 권한 체크박스 다수 — 을 모방하면서, **상용 라이브러리(Webix) 사용 금지** 제약 하에서 무료 대안을 확정 필요.

또한 사용자가 "이쁘장한 백오피스" 와 "고객 포털 디자인 차별화" 를 요구. AdminLTE 의 옛날 느낌 회피.

---

## 2. Decision

### 2-1. 백오피스 — **Tabler + AG Grid Community**

| 영역 | 채택 | 사유 |
|---|---|---|
| **UI 템플릿** | **Tabler** (Bootstrap 5, MIT) | 2024-2025 모던 디자인. ERP 도메인 데이터 밀도 표현 가능. 다크모드 기본. AdminLTE 보다 세련 |
| **그리드** | **AG Grid Community** (MIT) | ERP 업계 표준. 페이징/정렬/필터/체크박스/셀 에디터 모두 Community 에 포함. Enterprise 기능(피벗/마스터디테일) 없이도 본 프로젝트 요구 충족 |
| **JS 베이스** | Vanilla JS (`fetch`) | jQuery 의존 회피. AG Grid 가 Vanilla 우선 지원. 학습 가치 ↑ |

비채택:
- **AdminLTE**: 자료 풍부하나 디자인이 옛날 느낌. 포트폴리오 첫인상 약함
- **CoreUI**: Tabler 와 비슷. Tabler 가 더 가볍고 무료 범위가 넓음
- **DataTables.js**: 한국 SI 친숙하나 jQuery 의존. AG Grid 가 더 강력
- **Webix**: 판권 문제 — `docs/global-rules/frontend-grid-library.md` §1 명시 금지

### 2-2. 고객 포털 — **shadcn/ui + TanStack Table**

| 영역 | 채택 | 사유 |
|---|---|---|
| **CSS** | **Tailwind CSS** | 유틸리티 기반. shadcn/ui 의 베이스. Stitch export(Tailwind) 호환 |
| **UI 컴포넌트** | **shadcn/ui** (MIT) | 2025 React 표준 트렌드. CLI 로 컴포넌트 코드를 우리 레포에 복사 → 자유 커스터마이징. AntD/MUI 같은 의존성 종속 회피 |
| **데이터 그리드** | **TanStack Table v8** (MIT) | shadcn/ui `DataTable` 이 TanStack 래퍼 → 그리드 정책과 자연스럽게 정합 |

비채택:
- **AntD**: 훌륭하나 디자인 시스템 종속. shadcn/ui 의 "코드 직접 보유" 패턴이 학습 가치 ↑
- **MUI**: 동상 + 약간 무거움
- **PrimeReact**: 완성형이나 디자인 자유도 ↓
- **Monet**: 라이선스 불명확 + 데이터 컴포넌트 부재 (`docs/global-rules/frontend-grid-library.md` §1 참고)

### 2-3. 디자인 시안 도구

| 영역 | 도구 | 사유 |
|---|---|---|
| **백오피스 시안** | **Tabler 데모 페이지 캡처** (https://preview.tabler.io) | ERP 화면 24개 직접 와이어프레임 그리는 비용 회피. Tabler 가 이미 데모로 admin/datatables/modals/forms 제공 |
| **고객 포털 시안** | **Stitch** (Google) | 5화면 자유 디자인. HTML/Tailwind export → shadcn/ui 컴포넌트로 변환 |

---

## 3. Consequences

### 긍정
- 두 영역(백오피스/포털) 모두 **2024-2025 모던 트렌드** 따름 — 포트폴리오 첫인상 ↑
- 라이선스 모두 MIT (무료 + 상업 사용 가능)
- 학습 자산: Tabler(Bootstrap 5), shadcn/ui(Tailwind), AG Grid(ERP 표준), TanStack Table(React) — 4가지 검증된 시스템 학습
- Stitch export(Tailwind) 와 shadcn/ui(Tailwind) 의 시안→코드 변환 부담 ↓
- Webix 미사용으로 판권 위험 0

### 부정 / 비용
- **CSS 프레임워크 두 개** (Bootstrap + Tailwind) — 개발자가 둘 다 익혀야 함
- **shadcn/ui CLI 셋업** 학습 곡선 약간
- **AG Grid Enterprise 기능 제한** — 본 프로젝트엔 영향 없으나 향후 피벗/마스터-디테일 필요 시 유료 또는 직접 구현
- **Tailwind 클래스 조합으로 HTML 가독성 ↓** — JSX 안 className 길어짐

---

## 4. 사용자가 검토할 포인트

1. **CSS 두 시스템 부담** — 다른 사용자/문맥이라 통일 강제 불필요로 판단. 동의 여부
2. **Stitch 결과를 코드에 직접 안 쓴다는 점** — Stitch HTML 은 시안. 실제 React 컴포넌트는 shadcn/ui 로 별도 작성

---

## 5. 다음 액션

- [x] 03 산출물 갱신
- [ ] 05 화면 설계서 작성 (Tabler 데모 매핑 + Stitch 시안 5개 자리)
- [ ] backoffice/src/main/resources/static/ 에 Tabler + AG Grid 자산 배치 (코드 진입 시)
- [ ] customer-portal/ 에 React + Tailwind + shadcn/ui 셋업 (코드 진입 시)
