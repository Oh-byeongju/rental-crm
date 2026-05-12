---
description: "신규 화면에 그리드(데이터 테이블) 사용 시 — 채택 가능 라이브러리 + 라이선스 제약"
---

# 프론트엔드 그리드 라이브러리 정책

> 백오피스(Thymeleaf + JS)와 고객 포털(React) 양쪽 모두 적용되는 전역 룰.
> 그리드/데이터 테이블 사용 시 본 룰을 따른다.

---

## 1. 금지

### ❌ Webix
**판권/라이선스 문제** — 상용 제품. 학습/오픈소스 프로젝트에 사용 불가.

참고 프로젝트(GDI)에서 Webix 그리드를 사용한 사례가 있으나 본 프로젝트에는 가져오지 않는다.
GDI 의 그리드 패턴(컬럼/행 액션 버튼/체크박스 컬럼) 은 **기능 패턴만 차용**, 라이브러리 코드는 일절 미사용.

### ❌ 기타 상용 라이선스
- Kendo UI, DevExpress, Syncfusion, Wijmo, FlexGrid 등 상용 그리드 모두 금지
- "무료 평가판" 도 금지 (라이선스 만료 위험)

---

## 2. 허용 — 무료 라이브러리 (MIT / Apache)

### 백오피스 (Thymeleaf + JavaScript) — 채택 후보

| 라이브러리 | 라이선스 | 비고 |
|---|---|---|
| **AG Grid Community** | MIT | ⭐ ERP 업계 표준. Vanilla JS 지원. Enterprise 기능 일부 유료지만 Community 로 충분 |
| **Tabulator** | MIT | Vanilla JS, jQuery 무관, 가벼움 |
| **DataTables.js** | MIT | jQuery 기반. AdminLTE/Tabler 통합 자료 풍부. 단 jQuery 의존 |

### 고객 포털 (React) — 채택 후보

| 라이브러리 | 라이선스 | 비고 |
|---|---|---|
| **TanStack Table v8** | MIT | ⭐ Headless. React 친화. 스타일 직접 (Tailwind/ShadCN 호환) |
| **MUI X Data Grid Community** | MIT | Material UI 의존. 일부 기능(트리/피벗) 유료 |
| **PrimeReact DataTable** | MIT | PrimeReact 의존. 완성도 높음 |
| **AntD Table** | MIT | AntD 의존. 가벼운 그리드엔 OK |

---

## 3. 채택 결정

본 프로젝트의 채택은 **별도 ADR 로 확정**.

- 백오피스 그리드 ADR: (작성 예정)
- 고객 포털 그리드 ADR: (작성 예정)

---

## 4. 직접 구현하는 경우

라이브러리 사용 없이 직접 그리드를 구현하는 것도 허용. 단:
- 학습 가치 측면에서 (가상 스크롤, 컬럼 리사이즈 등) 일부 기능만 구현하고 일반 기능은 라이브러리 권장
- 직접 구현 시 별도 ADR 로 사유 명시 (왜 라이브러리 미사용)

---

## 5. 신규 그리드 후보 추가 시

본 표에 행 추가 + 라이선스 검증. 사용자가 추가 검토 후 확정.

---

## 6. 관련 룰

- 백오피스 화면 디자인: `docs/global-rules/backoffice-ui-template.md` (작성 예정 — Tabler 또는 AdminLTE 채택 후)
- React 컨벤션: `customer-portal/guide/` (작성 예정)
