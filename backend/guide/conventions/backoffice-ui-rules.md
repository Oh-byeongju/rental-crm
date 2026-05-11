---
description: "백오피스 화면(Thymeleaf + Tabler + AG Grid) 작성 시 — 그리드 컬럼 자동 확장 / 검색 폼 잘림 방지 / 모달 패턴"
---

# 백오피스 화면 구현 규칙

> 백오피스 Thymeleaf + Tabler + AG Grid 화면 작성 시 필수 적용.
> 신규 도메인 화면 양산 시 본 룰을 따른다.

---

## 1. AG Grid — 컬럼 자동 확장 (잘림 방지)

### 1-1. 원칙

**컬럼 폭은 콘텐츠 길이를 보장한다.** 데이터가 컬럼 폭을 넘어 잘리지 않게 한다.

### 1-2. 채택 방식 — `flex` + `minWidth` 조합

```javascript
const columnDefs = [
    // 체크박스: 고정 width, flex 미사용
    { headerCheckboxSelection: true, checkboxSelection: true, width: 50, flex: 0,
      sortable: false, filter: false, resizable: false, pinned: 'left' },

    // 비즈니스 컬럼: minWidth 보장 + flex 로 늘어남
    { headerName: '고객번호', field: 'customerNo',  minWidth: 200, flex: 1 },
    { headerName: '고객명',   field: 'customerName', minWidth: 120, flex: 1 },
    { headerName: '이메일',   field: 'email',        minWidth: 220, flex: 2 },
    { headerName: '주소',     field: 'address',      minWidth: 240, flex: 3 }, // 긴 텍스트는 flex 크게

    // 상태/날짜 같은 짧은 고정 폭: flex: 0 + width 명시 또는 minWidth + 작은 flex
    { headerName: '사용여부', field: 'useYn',        minWidth: 110, flex: 0, cellRenderer: statusBadgeRenderer },
    { headerName: '등록일시', field: 'firsRegDts',   minWidth: 160, flex: 0, valueFormatter: dateTimeFormatter },
];
```

### 1-3. `minWidth` 추정 가이드

| 데이터 종류 | 권장 minWidth |
|---|---|
| ID (숫자 5~7자리) | 80 |
| 코드값 (`CUST-YYYYMMDD-NNNNN` 등 18자~) | **200** |
| 이름 (3~10자) | 120 |
| 이메일 | 220 |
| 연락처 (`010xxxxxxxx`) | 130 |
| 주소 | 240 |
| 상태 뱃지 (`정상`, `일시정지`, `수납완료`) | 110 |
| 일시 (`YYYY-MM-DD HH:mm`) | 160 |
| 금액 (`100,000 원`) | 130 |

> 한글 폰트 기준. 영문만이면 약 70% 폭으로 가능. 보수적으로 잡는 게 안전.

### 1-4. `flex` 비율 가이드

- **flex 0**: 고정 폭 (체크박스 / 상태 / 일시)
- **flex 1**: 일반 (이름 / 연락처 / 코드)
- **flex 2~3**: 긴 텍스트 (주소 / 비고 / 메모)

### 1-5. 컬럼 헤더 자동 줄임 방지

헤더가 잘려서 "사용..." 같이 보이는 문제. `minWidth` 가 헤더 텍스트보다 작으면 발생. **헤더 한글 길이 + 여유 30px** 이상 보장.

```javascript
// ❌ 잘못
{ headerName: '사용여부', field: 'useYn', minWidth: 80 }   // "사용..." 으로 잘림

// ✅ 정답
{ headerName: '사용여부', field: 'useYn', minWidth: 110, flex: 0 }
```

### 1-6. AG Grid 컨테이너 너비

`.ag-theme-quartz` 컨테이너는 부모(Tabler `card-body`) 의 width 100% 사용 — 자동 확장. 별도 width 명시 X.

---

## 2. 검색 폼 — 잘림 방지

### 2-1. 원칙

**입력 필드의 label / placeholder / value 가 잘리지 않도록 충분한 width 확보.**

### 2-2. Bootstrap grid 권장 폭

| 입력 유형 | 권장 col |
|---|---|
| 짧은 텍스트 (이름, 번호 일부) | `col-md-3` |
| 보통 텍스트 (연락처, 이메일) | `col-md-3` ~ `col-md-4` |
| select (옵션 한글 7자 이내) | `col-md-3` (❌ `col-md-2` 금지 — placeholder 잘림) |
| date range / 두 input 한 줄 | `col-md-4` ~ `col-md-6` |
| 액션 버튼 영역 | `col-md-2` 이상 + `d-flex align-items-end` |

### 2-3. 한 줄 컬럼 수 한도

**한 row 에 검색 필드 최대 4개 + 액션 1개**. 5개 이상이면 `<div class="row g-2">` 새 줄로 분리.

```html
<!-- ✅ 정답 -->
<form class="row g-2 search-form">
    <div class="col-md-3">
        <label class="form-label">고객명</label>
        <input type="text" name="name" class="form-control" placeholder="이름 prefix">
    </div>
    <div class="col-md-3">
        <label class="form-label">연락처</label>
        <input type="text" name="phone" class="form-control" placeholder="01012345678">
    </div>
    <div class="col-md-3">
        <label class="form-label">이메일</label>
        <input type="text" name="email" class="form-control" placeholder="hong@...">
    </div>
    <div class="col-md-3">
        <label class="form-label">사용여부</label>
        <select name="useYn" class="form-select">
            <option value="">전체</option>
            <option value="Y">사용</option>
            <option value="N">미사용</option>
        </select>
    </div>
    <div class="col-12 d-flex justify-content-end gap-2 mt-2">
        <button type="submit" class="btn btn-primary"><i class="ti ti-search"></i> 조회</button>
        <button type="button" class="btn btn-default">초기화</button>
    </div>
</form>
```

### 2-4. 잘림 자가 검증

화면 작성 후 다음 시나리오로 확인:
- placeholder 텍스트가 input 안에서 잘리지 않음
- select 의 최장 옵션 (`일시정지`, `수납완료` 등) 이 잘리지 않음
- label 한글 텍스트가 줄바꿈되지 않음

---

## 3. 모달 — 등록/수정/상세 패턴

### 3-1. 표준 구조

```html
<div class="modal modal-blur fade" id="{domain}Modal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="{domain}ModalTitle">{도메인} 등록</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <form id="{domain}Form">
                    <!-- 폼 필드 row.g-2 -->
                </form>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-link link-secondary" data-bs-dismiss="modal">닫기</button>
                <button type="button" class="btn btn-danger d-none" id="btnDeactivate">비활성화</button>
                <button type="button" class="btn btn-primary" id="btnSave">저장</button>
            </div>
        </div>
    </div>
</div>
```

### 3-2. 모드 분기

- **create 모드**: customerNo 숨김 / email·password 활성 / [비활성화] 숨김
- **update 모드**: customerNo 표시(readonly) / email readonly / password 숨김 / [비활성화] 표시

### 3-3. 폼 필드 width — 그리드와 동일 원칙

모달 안 form 도 §2 의 row.g-2 + col-md-* 사용. 단 모달은 좁으므로 한 row 에 2~3 필드 권장.

---

## 4. Tabler `empty` 컴포넌트 — 미구현/빈 데이터

```html
<div class="empty">
    <div class="empty-img"><i class="ti ti-{icon}" style="font-size: 4rem;"></i></div>
    <p class="empty-title">{타이틀}</p>
    <p class="empty-subtitle text-muted">{설명}</p>
    <div class="empty-action">
        <a href="{prev}" class="btn btn-primary">{액션}</a>
    </div>
</div>
```

미구현 메뉴 / 검색 결과 0건 / 로딩 에러 등 일관 적용.

---

## 4-A. 정적 자원 참조 — `th:src` / `th:href` 필수

### 원칙

JS/CSS 참조는 **반드시 Thymeleaf `@{...}` 표현식** 으로 작성한다.
운영(`prod` 프로파일) 에서 콘텐츠 해시 versioning 이 자동 부여되는 통로.

```html
<!-- ❌ 잘못 -->
<script src="/js/customer-list.js"></script>
<link rel="stylesheet" href="/css/app.css"/>

<!-- ✅ 정답 -->
<script th:src="@{/js/customer-list.js}"></script>
<link rel="stylesheet" th:href="@{/css/app.css}"/>
```

### 운영에서 어떻게 변환되나

| 프로파일 | HTML 응답 결과 |
|---|---|
| `local` | `<script src="/js/customer-list.js">` (해시 없음, 캐시 OFF) |
| `prod`  | `<script src="/js/customer-list-a1b2c3d4.js">` (콘텐츠 해시 + 1년 캐시) |

### 외부 CDN 은 예외

```html
<!-- 외부 URL 은 @{} 불필요 -->
<script src="https://cdn.jsdelivr.net/npm/@tabler/core/.../tabler.min.js"></script>
```

CDN 자체가 자체 캐시 정책을 가짐. Spring 의 자동 해시 부여 대상 아님.

### 환경별 캐시 전략 요약

| 환경 | 캐시 | 정적 자원 위치 | 효과 |
|---|---|---|---|
| `local` | OFF | `file:src/main/resources/static/` (디스크 직접) | 변경 즉시 반영 (재시작 X) |
| `prod`  | 1년 immutable | classpath only (jar 안) + 콘텐츠 해시 | CDN/브라우저 캐시 풀 활용. 변경 시에만 새 fetch |

> 운영에서 캐시 OFF 적용 시 매 요청 정적 자원 재다운로드 → 네트워크/서버 부담. 콘텐츠 해시 + 장기 캐시가 정석.

상세: [application-local.yml](../../src/main/resources/application-local.yml) / [application-prod.yml](../../src/main/resources/application-prod.yml)

---

## 5. 페이지 헤더 — 공통 구조

```html
<div class="page-header d-print-none">
    <div class="container-xl">
        <div class="row g-2 align-items-center">
            <div class="col">
                <div class="page-pretitle">{카테고리}</div>
                <h2 class="page-title">{페이지 제목}</h2>
            </div>
            <div class="col-auto ms-auto">
                <div class="btn-list">
                    <button class="btn btn-primary"><i class="ti ti-plus"></i> 등록</button>
                    <button class="btn btn-default"><i class="ti ti-download"></i> 엑셀</button>
                </div>
            </div>
        </div>
    </div>
</div>
```

- 좌측: 카테고리 (작은 텍스트) + 페이지 제목 (h2)
- 우측: 액션 버튼 (등록 / 엑셀 다운로드 등) — `btn-list` 로 묶음

---

## 6. 신규 도메인 화면 양산 체크리스트

다른 도메인(장비/상품/계약 등) 화면 만들 때:

- [ ] 페이지 헤더: 카테고리 + 제목 + 우측 [등록] 버튼 (§5)
- [ ] 검색 카드: row.g-2 + col-md-3 통일 (§2)
- [ ] 그리드 카드: `card-grid` 클래스 + AG Grid + `flex` + `minWidth` (§1)
- [ ] 모달: §3 표준 구조 + 등록/수정 모드 분기
- [ ] JS 두 파일: `{domain}-list.js` + `{domain}-form.js`
- [ ] REST API 명세 일치 (07 산출물)
- [ ] 컬럼 헤더 한글 길이 + 30px ≤ minWidth (§1-5)
- [ ] 정적 자원 참조 `th:src="@{...}"` / `th:href="@{...}"` (§4-A) — 운영 versioning 통로

---

## 7. 변경 이력

- 2026-05-11: 신규 작성 — Customer 첫 화면 검토 후 도출
