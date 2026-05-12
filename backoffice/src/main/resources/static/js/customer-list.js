/* ============================================================
 * 고객 관리 — 목록 + 검색 + AG Grid
 * 05 §2-7 + 07 §5 매핑
 * ============================================================ */

const CustomerList = (() => {
    let gridApi = null;
    let currentPage = 0;
    let currentSize = 20;

    // 컬럼 폭 정책: flex + minWidth — 텍스트 길이 따라 늘어남, 잘리지 않음.
    // 가이드: backoffice/guide/conventions/backoffice-ui-rules.md §1
    const columnDefs = [
        { headerCheckboxSelection: true, checkboxSelection: true, width: 50, flex: 0,
          sortable: false, filter: false, resizable: false, pinned: 'left' },
        { headerName: '고객번호', field: 'customerNo',   minWidth: 210, flex: 1 },
        { headerName: '고객명',   field: 'customerName', minWidth: 120, flex: 1 },
        { headerName: '이메일',   field: 'email',        minWidth: 230, flex: 2 },
        { headerName: '연락처',   field: 'phone',        minWidth: 140, flex: 1 },
        { headerName: '주소',     field: 'address',      minWidth: 240, flex: 3 },
        { headerName: '사용여부', field: 'useYn',        minWidth: 110, flex: 0, cellRenderer: statusBadgeRenderer },
        { headerName: '등록일시', field: 'firsRegDts',   minWidth: 165, flex: 0, valueFormatter: dateTimeFormatter },
    ];

    function init() {
        const el = document.querySelector('#customerGrid');
        const opts = {
            ...AgGridDefaults,
            columnDefs,
            rowData: [],
            onRowDoubleClicked: (e) => CustomerForm.openUpdate(e.data.customerId),
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
            const res = await App.get('/api/customers?' + qs);
            const page = res.data;
            gridApi.setGridOption('rowData', page.content);
            // pagination 정보 표시는 AG Grid 자체 페이저가 처리. 서버 페이징 동기화는 추후 확장.
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

document.addEventListener('DOMContentLoaded', CustomerList.init);
