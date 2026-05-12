/* ============================================================
 * 연체 관리 — 목록 + 검색 + AG Grid
 * 기본 정렬: overdueDays DESC (장기 연체자 우선)
 * 행 더블클릭 = 해제 모달 (미해결만)
 * ============================================================ */

const OverdueList = (() => {
    let gridApi = null;
    let currentPage = 0;
    let currentSize = 20;

    const moneyFormatter = (p) => (p.value == null) ? '' : Number(p.value).toLocaleString();
    const resolvedBadge = (resolved) => resolved
        ? '<span class="badge bg-green-lt">해결</span>'
        : '<span class="badge bg-red-lt">미해결</span>';
    const daysRenderer = (p) => {
        const days = Number(p.value ?? 0);
        if (days >= 60) return `<span class="text-danger fw-bold">${days}일</span>`;
        if (days >= 30) return `<span class="text-warning fw-bold">${days}일</span>`;
        return `${days}일`;
    };

    const columnDefs = [
        { headerName: 'ID',       field: 'overdueId',    minWidth: 80,  flex: 0, type: 'rightAligned' },
        { headerName: '청구번호', field: 'billingNo',    minWidth: 200, flex: 0,
          cellRenderer: (p) => p.value ? `<a href="/admin/billings" class="text-primary">${p.value}</a>` : '-' },
        { headerName: '청구월',   field: 'billingMonth', minWidth: 100, flex: 0 },
        { headerName: '고객',     minWidth: 160, flex: 1,
          valueGetter: (p) => p.data ? `${p.data.customerName ?? '-'} (${p.data.customerNo ?? '-'})` : '' },
        { headerName: '연체금액', field: 'overdueAmount', minWidth: 130, flex: 0, type: 'rightAligned', valueFormatter: moneyFormatter },
        { headerName: '연체일수', field: 'overdueDays',  minWidth: 120, flex: 0, type: 'rightAligned',
          cellRenderer: daysRenderer },
        { headerName: '상태',     field: 'resolved',     minWidth: 100, flex: 0,
          cellRenderer: (p) => resolvedBadge(p.value) },
        { headerName: '해제일시', field: 'resolvedAt',   minWidth: 165, flex: 0, valueFormatter: dateTimeFormatter },
        { headerName: '해제사유', field: 'resolveReason', minWidth: 200, flex: 1 },
        { headerName: '등록일시', field: 'createdAt',    minWidth: 165, flex: 0, valueFormatter: dateTimeFormatter },
    ];

    function init() {
        const el = document.querySelector('#overdueGrid');
        gridApi = agGrid.createGrid(el, {
            ...AgGridDefaults,
            columnDefs,
            rowData: [],
            onRowDoubleClicked: (e) => {
                if (e.data.resolved) {
                    App.toast('이미 해결된 연체', 'warning');
                    return;
                }
                OverdueForm.openResolve(e.data);
            },
            onPaginationChanged: (e) => {
                if (!e.newPage) return;
                const page = gridApi.paginationGetCurrentPage();
                if (page !== currentPage) {
                    currentPage = page;
                    load();
                }
            },
        });
        loadCustomers();
        load();
    }

    async function loadCustomers() {
        try {
            const res = await App.get('/api/customers?size=500');
            const sel = document.querySelector('#searchCustomerId');
            for (const c of res.data?.content ?? []) {
                const opt = document.createElement('option');
                opt.value = c.customerId;
                opt.textContent = `${c.customerNo} - ${c.customerName}`;
                sel.appendChild(opt);
            }
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    async function load() {
        const params = collectSearchParams();
        params.page = currentPage;
        params.size = currentSize;
        const qs = new URLSearchParams(params).toString();
        try {
            const res = await App.get('/api/overdues?' + qs);
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

document.addEventListener('DOMContentLoaded', OverdueList.init);
