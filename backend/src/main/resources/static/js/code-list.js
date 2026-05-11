/* ============================================================
 * 공통코드 (값) — 목록 + 검색 + AG Grid
 * groupCode 는 hidden input 에서 읽어옴 (Thymeleaf 가 페이지 진입 시 주입)
 * ============================================================ */

const CodeList = (() => {
    let gridApi = null;
    let currentPage = 0;
    let currentSize = 20;

    const columnDefs = [
        { headerCheckboxSelection: true, checkboxSelection: true, width: 50, flex: 0,
          sortable: false, filter: false, resizable: false, pinned: 'left' },
        { headerName: '코드 값',   field: 'codeValue', minWidth: 160, flex: 1 },
        { headerName: '코드명',    field: 'codeName',  minWidth: 180, flex: 2 },
        { headerName: '정렬',      field: 'sortOrder', minWidth: 90,  flex: 0, type: 'rightAligned' },
        { headerName: '사용여부',  field: 'useYn',     minWidth: 100, flex: 0,
          cellRenderer: (p) => p.value === 'Y'
              ? '<span class="badge bg-green-lt">사용</span>'
              : '<span class="badge bg-secondary-lt">미사용</span>' },
        { headerName: '등록자',    field: 'createdBy', minWidth: 110, flex: 0 },
        { headerName: '등록일시',  field: 'createdAt', minWidth: 165, flex: 0, valueFormatter: dateTimeFormatter },
    ];

    function getGroupCode() {
        return document.querySelector('#groupCode').value;
    }

    function init() {
        const el = document.querySelector('#codeGrid');
        const opts = {
            ...AgGridDefaults,
            columnDefs,
            rowData: [],
            onRowDoubleClicked: (e) => CodeForm.openUpdate(e.data.codeId),
            onPaginationChanged: (e) => {
                if (!e.newPage) return;
                const page = gridApi.paginationGetCurrentPage();
                if (page !== currentPage) {
                    currentPage = page;
                    load();
                }
            },
        };
        gridApi = agGrid.createGrid(el, opts);
        load();
    }

    async function load() {
        const params = collectSearchParams();
        params.page = currentPage;
        params.size = currentSize;
        const qs = new URLSearchParams(params).toString();
        try {
            const res = await App.get(`/api/code-groups/${getGroupCode()}/codes?` + qs);
            gridApi.setGridOption('rowData', res.data.content);
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    function collectSearchParams() {
        const form = document.querySelector('#searchForm');
        const data = new FormData(form);
        const out = {};
        for (const [k, v] of data.entries()) {
            if (v) out[k] = v;
        }
        return out;
    }

    function search(event) {
        event.preventDefault();
        currentPage = 0;
        load();
        return false;
    }

    function reset() {
        const form = document.querySelector('#searchForm');
        form.reset();
        currentPage = 0;
        load();
    }

    return { init, load, search, reset, getGroupCode };
})();

document.addEventListener('DOMContentLoaded', CodeList.init);
