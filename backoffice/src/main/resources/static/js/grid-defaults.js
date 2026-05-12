/* ============================================================
 * AG Grid 표준 옵션 (05 화면 설계서 §1-3)
 * 모든 그리드가 본 베이스를 spread 로 가져가 도메인 컬럼만 덮음.
 *
 * 사용법:
 *   const opts = {
 *     ...AgGridDefaults,
 *     columnDefs: [
 *       { headerCheckboxSelection: true, checkboxSelection: true, width: 40 },
 *       { headerName: '고객명', field: 'customerName' },
 *       ...
 *     ],
 *     rowData: data,
 *     onRowDoubleClicked: (e) => openDetail(e.data),
 *   };
 *   const grid = agGrid.createGrid(document.querySelector('#myGrid'), opts);
 * ============================================================ */

const AgGridDefaults = {
    rowSelection: 'multiple',
    pagination: true,
    paginationPageSize: 20,
    paginationPageSizeSelector: [20, 50, 100],
    defaultColDef: {
        sortable: true,
        filter: true,
        resizable: true,
        minWidth: 100,
        flex: 1,
    },
    rowHeight: 36,
    headerHeight: 40,
    animateRows: true,
    suppressColumnVirtualisation: false,
    suppressMenuHide: true,
    localeText: {
        // 페이징 한글화 (요점만)
        page: '페이지',
        more: '더보기',
        to: '~',
        of: '/',
        next: '다음',
        last: '마지막',
        first: '처음',
        previous: '이전',
        loadingOoo: '불러오는 중...',
        noRowsToShow: '데이터가 없습니다',
        // 필터
        searchOoo: '검색...',
        equals: '같음',
        notEqual: '다름',
        contains: '포함',
        startsWith: '시작',
        endsWith: '끝',
        // 헤더 메뉴
        pinColumn: '컬럼 고정',
        autosizeThiscolumn: '컬럼 너비 자동',
        autosizeAllColumns: '전체 컬럼 너비 자동',
        resetColumns: '컬럼 초기화',
    },
};

/**
 * 상태 컬럼 렌더러 — 청구상태 등을 뱃지로 표시.
 * 사용: { headerName: '상태', field: 'billingStatus', cellRenderer: statusBadgeRenderer }
 */
function statusBadgeRenderer(params) {
    const map = {
        UNPAID:     { label: '미납',     cls: 'badge-status-unpaid' },
        OVERDUE:    { label: '연체',     cls: 'badge-status-overdue' },
        PAID:       { label: '수납완료', cls: 'badge-status-paid' },
        CANCELLED:  { label: '취소',     cls: 'badge-status-cancelled' },
        ACTIVE:     { label: '정상',     cls: 'badge-status-paid' },
        SUSPENDED:  { label: '일시정지', cls: 'badge-status-overdue' },
        TERMINATED: { label: '해지',     cls: 'badge-status-cancelled' },
    };
    const m = map[params.value] || { label: params.value, cls: 'bg-secondary text-white' };
    return `<span class="badge ${m.cls}">${m.label}</span>`;
}

/**
 * 금액 컬럼 포맷터 — 1,000 단위 콤마 + " 원" 접미.
 * 사용: { headerName: '청구금액', field: 'billingAmount', valueFormatter: amountFormatter, type: 'rightAligned' }
 */
function amountFormatter(params) {
    if (params.value == null) return '';
    return params.value.toLocaleString('ko-KR') + ' 원';
}

/** 일시 컬럼 포맷터. */
function dateTimeFormatter(params) {
    if (!params.value) return '';
    return params.value.replace('T', ' ').substring(0, 16);
}
