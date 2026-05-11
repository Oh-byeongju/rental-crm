/* ============================================================
 * 역할 관리 — 목록 + 검색 + AG Grid
 * 07 §4 매핑
 * ============================================================ */

const RoleList = (() => {
    let gridApi = null;
    let currentPage = 0;
    let currentSize = 20;

    const columnDefs = [
        { headerCheckboxSelection: true, checkboxSelection: true, width: 50, flex: 0,
          sortable: false, filter: false, resizable: false, pinned: 'left' },
        { headerName: 'ID',       field: 'roleId',   minWidth: 80,  flex: 0, type: 'rightAligned' },
        { headerName: '역할 코드', field: 'roleCode', minWidth: 160, flex: 1,
          cellRenderer: (p) => `<a href="/admin/roles/${p.data.roleId}/auths" class="text-primary fw-bold">${p.value}</a>`,
          tooltipValueGetter: () => '클릭 시 권한 매트릭스 화면' },
        { headerName: '역할명',    field: 'roleName',    minWidth: 140, flex: 1 },
        { headerName: '설명',      field: 'description', minWidth: 280, flex: 3 },
        { headerName: 'SUPER',    field: 'superAdmin',   minWidth: 90,  flex: 0,
          cellRenderer: (p) => p.value
              ? '<span class="badge bg-red-lt">SUPER</span>'
              : '' },
        { headerName: '사용',      field: 'useYn',     minWidth: 90,  flex: 0,
          cellRenderer: (p) => p.value === 'Y'
              ? '<span class="badge bg-green-lt">사용</span>'
              : '<span class="badge bg-secondary-lt">미사용</span>' },
        { headerName: '등록자',    field: 'createdBy', minWidth: 110, flex: 0 },
        { headerName: '등록일시',  field: 'createdAt', minWidth: 165, flex: 0, valueFormatter: dateTimeFormatter },
    ];

    function init() {
        const el = document.querySelector('#roleGrid');
        const opts = {
            ...AgGridDefaults,
            columnDefs,
            rowData: [],
            onRowDoubleClicked: (e) => RoleForm.openUpdate(e.data.roleId),
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
            const res = await App.get('/api/roles?' + qs);
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

document.addEventListener('DOMContentLoaded', RoleList.init);
