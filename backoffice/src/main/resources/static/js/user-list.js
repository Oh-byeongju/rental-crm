/* ============================================================
 * 관리자 관리 — 목록 + 검색 + AG Grid
 * 07 §3 매핑
 * ============================================================ */

const AdminUserList = (() => {
    let gridApi = null;
    let currentPage = 0;
    let currentSize = 20;

    const columnDefs = [
        { headerCheckboxSelection: true, checkboxSelection: true, width: 50, flex: 0,
          sortable: false, filter: false, resizable: false, pinned: 'left' },
        { headerName: 'ID',       field: 'userId',   minWidth: 70,  flex: 0, type: 'rightAligned' },
        { headerName: '이메일',   field: 'email',    minWidth: 220, flex: 2,
          cellRenderer: (p) => `<a href="/admin/users/${p.data.userId}/auths" class="text-primary fw-bold" title="권한 미세 조정">${p.value}</a>` },
        { headerName: '이름',     field: 'userName', minWidth: 110, flex: 1 },
        { headerName: '연락처',   field: 'phone',    minWidth: 130, flex: 1 },
        { headerName: '역할',     field: 'roleName', minWidth: 140, flex: 1,
          cellRenderer: (p) => p.value
              ? `<span class="badge bg-blue-lt">${p.value}</span>`
              : '<span class="text-secondary">— 미부여 —</span>' },
        { headerName: '사용',     field: 'useYn',    minWidth: 80, flex: 0,
          cellRenderer: (p) => p.value === 'Y'
              ? '<span class="badge bg-green-lt">사용</span>'
              : '<span class="badge bg-secondary-lt">미사용</span>' },
        { headerName: '잠금',     field: 'locked',   minWidth: 80, flex: 0,
          cellRenderer: (p) => p.value
              ? '<span class="badge bg-red-lt">잠금</span>'
              : '<span class="text-secondary">정상</span>' },
        { headerName: '실패',     field: 'loginFailCnt', minWidth: 70, flex: 0, type: 'rightAligned' },
        { headerName: '최근 로그인', field: 'lastLoginAt', minWidth: 165, flex: 0, valueFormatter: dateTimeFormatter },
        { headerName: '등록일시', field: 'createdAt',    minWidth: 165, flex: 0, valueFormatter: dateTimeFormatter },
    ];

    function init() {
        const el = document.querySelector('#userGrid');
        const opts = {
            ...AgGridDefaults,
            columnDefs,
            rowData: [],
            onRowDoubleClicked: (e) => AdminUserForm.openUpdate(e.data.userId),
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
            const res = await App.get('/api/users?' + qs);
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

document.addEventListener('DOMContentLoaded', AdminUserList.init);
