/* ============================================================
 * 공통코드 그룹 — 목록 + 검색 + AG Grid
 * 04 §1-1 + 07 §2-1 매핑
 * ============================================================ */

const CodeGroupList = (() => {
    let gridApi = null;
    let currentPage = 0;
    let currentSize = 20;

    const columnDefs = [
        { headerCheckboxSelection: true, checkboxSelection: true, width: 50, flex: 0,
          sortable: false, filter: false, resizable: false, pinned: 'left' },
        { headerName: '그룹 코드', field: 'groupCode',   minWidth: 180, flex: 1,
          cellRenderer: (p) => `<a href="/admin/codes/${p.value}" class="text-primary fw-bold">${p.value}</a>` },
        { headerName: '그룹명',    field: 'groupName',   minWidth: 140, flex: 1 },
        { headerName: '설명',      field: 'description', minWidth: 240, flex: 3 },
        { headerName: '사용여부',  field: 'useYn',       minWidth: 100, flex: 0,
          cellRenderer: (p) => p.value === 'Y'
              ? '<span class="badge bg-green-lt">사용</span>'
              : '<span class="badge bg-secondary-lt">미사용</span>' },
        { headerName: '등록자',    field: 'createdBy',   minWidth: 110, flex: 0 },
        { headerName: '등록일시',  field: 'createdAt',   minWidth: 165, flex: 0, valueFormatter: dateTimeFormatter },
    ];

    function init() {
        const el = document.querySelector('#codeGroupGrid');
        const opts = {
            ...AgGridDefaults,
            columnDefs,
            rowData: [],
            onRowDoubleClicked: (e) => CodeGroupForm.openUpdate(e.data.groupCode),
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
            const res = await App.get('/api/code-groups?' + qs);
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

    return { init, load, search, reset };
})();

document.addEventListener('DOMContentLoaded', CodeGroupList.init);
