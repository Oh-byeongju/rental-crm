/* ============================================================
 * 기사 관리 — 목록 + 검색 + AG Grid
 * 지역별 가용 기사 조회 — IDX_CT_ENGINEER_AREA 활용
 * ============================================================ */

const EngineerList = (() => {
    let gridApi = null;
    let currentPage = 0;
    let currentSize = 20;

    const typeBadge = (type) => {
        if (type === 'INTERNAL') return '<span class="badge bg-blue-lt">내부 직원</span>';
        if (type === 'EXTERNAL') return '<span class="badge bg-purple-lt">외주</span>';
        return type;
    };

    const columnDefs = [
        { headerCheckboxSelection: true, checkboxSelection: true, width: 50, flex: 0,
          sortable: false, filter: false, resizable: false, pinned: 'left' },
        { headerName: 'ID',       field: 'engineerId',   minWidth: 80,  flex: 0, type: 'rightAligned' },
        { headerName: '기사코드', field: 'engineerCode', minWidth: 130, flex: 0 },
        { headerName: '기사명',   field: 'engineerName', minWidth: 130, flex: 1 },
        { headerName: '유형',     field: 'engineerType', minWidth: 110, flex: 0,
          cellRenderer: (p) => typeBadge(p.value) },
        { headerName: '연락처',   field: 'phone',        minWidth: 130, flex: 0 },
        { headerName: '이메일',   field: 'email',        minWidth: 200, flex: 1 },
        { headerName: '담당 지역', field: 'area',        minWidth: 160, flex: 1 },
        { headerName: '사용',     field: 'useYn',        minWidth: 90,  flex: 0,
          cellRenderer: (p) => p.value === 'Y'
              ? '<span class="badge bg-green-lt">사용</span>'
              : '<span class="badge bg-secondary-lt">미사용</span>' },
        { headerName: '등록일시', field: 'createdAt',    minWidth: 165, flex: 0, valueFormatter: dateTimeFormatter },
    ];

    function init() {
        const el = document.querySelector('#engineerGrid');
        gridApi = agGrid.createGrid(el, {
            ...AgGridDefaults,
            columnDefs,
            rowData: [],
            onRowDoubleClicked: (e) => EngineerForm.openUpdate(e.data.engineerId),
            onPaginationChanged: (e) => {
                if (!e.newPage) return;
                const page = gridApi.paginationGetCurrentPage();
                if (page !== currentPage) {
                    currentPage = page;
                    load();
                }
            },
        });
        load();
    }

    async function load() {
        const params = collectSearchParams();
        params.page = currentPage;
        params.size = currentSize;
        const qs = new URLSearchParams(params).toString();
        try {
            const res = await App.get('/api/engineers?' + qs);
            gridApi.setGridOption('rowData', res.data.content);
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    function collectSearchParams() {
        const form = document.querySelector('#searchForm');
        const data = new FormData(form);
        const out = {};
        for (const [k, v] of data.entries()) if (v) out[k] = v;
        return out;
    }

    function search(event) { event.preventDefault(); currentPage = 0; load(); return false; }
    function reset() { document.querySelector('#searchForm').reset(); currentPage = 0; load(); }

    return { init, load, search, reset };
})();

document.addEventListener('DOMContentLoaded', EngineerList.init);
