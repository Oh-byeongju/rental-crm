/* ============================================================
 * 청구 관리 — 목록 + 검색 + AG Grid
 * 단건 등록 X (배치 일괄 생성). 행 더블클릭 = 상세 모달.
 * ============================================================ */

const BillingList = (() => {
    let gridApi = null;
    let currentPage = 0;
    let currentSize = 20;

    const moneyFormatter = (p) => (p.value == null) ? '' : Number(p.value).toLocaleString();
    const statusBadge = (status) => {
        if (status === 'UNPAID')    return '<span class="badge bg-yellow-lt">미납</span>';
        if (status === 'OVERDUE')   return '<span class="badge bg-red-lt">연체</span>';
        if (status === 'PAID')      return '<span class="badge bg-green-lt">수납완료</span>';
        if (status === 'CANCELLED') return '<span class="badge bg-secondary-lt">취소</span>';
        return status;
    };

    const columnDefs = [
        { headerCheckboxSelection: true, checkboxSelection: true, width: 50, flex: 0,
          sortable: false, filter: false, resizable: false, pinned: 'left' },
        { headerName: '청구번호', field: 'billingNo',     minWidth: 200, flex: 0,
          cellRenderer: (p) => `<span class="text-primary fw-bold">${p.value}</span>` },
        { headerName: '계약번호', field: 'contractNo',    minWidth: 200, flex: 0,
          cellRenderer: (p) => p.value ? `<a href="/admin/contracts/${p.data.contractId}" class="text-primary">${p.value}</a>` : '-' },
        { headerName: '고객',     minWidth: 160, flex: 1,
          valueGetter: (p) => p.data ? `${p.data.customerName ?? '-'} (${p.data.customerNo ?? '-'})` : '' },
        { headerName: '청구월',   field: 'billingMonth',  minWidth: 100, flex: 0 },
        { headerName: '청구금액', field: 'billingAmount', minWidth: 130, flex: 0, type: 'rightAligned', valueFormatter: moneyFormatter },
        { headerName: '발행일',   field: 'issueDate',     minWidth: 120, flex: 0 },
        { headerName: '납기일',   field: 'dueDate',       minWidth: 120, flex: 0 },
        { headerName: '상태',     field: 'billingStatus', minWidth: 110, flex: 0,
          cellRenderer: (p) => statusBadge(p.value) },
        { headerName: '수납일시', field: 'paidAt',        minWidth: 165, flex: 0, valueFormatter: dateTimeFormatter },
    ];

    function init() {
        const el = document.querySelector('#billingGrid');
        gridApi = agGrid.createGrid(el, {
            ...AgGridDefaults,
            columnDefs,
            rowData: [],
            onRowDoubleClicked: (e) => BillingForm.openDetail(e.data.billingId),
            onPaginationChanged: (e) => {
                if (!e.newPage) return;
                const page = gridApi.paginationGetCurrentPage();
                if (page !== currentPage) {
                    currentPage = page;
                    load();
                }
            },
        });
        loadSearchOptions();
        load();
    }

    async function loadSearchOptions() {
        try {
            const [customers, statusOpts] = await Promise.all([
                App.get('/api/customers?size=500'),
                App.get('/api/code-groups/BILLING_STATUS/options'),
            ]);
            const cSel = document.querySelector('#searchCustomerId');
            for (const c of customers.data?.content ?? []) {
                const opt = document.createElement('option');
                opt.value = c.customerId;
                opt.textContent = `${c.customerNo} - ${c.customerName}`;
                cSel.appendChild(opt);
            }
            const sSel = document.querySelector('#searchBillingStatus');
            for (const s of statusOpts.data ?? []) {
                const opt = document.createElement('option');
                opt.value = s.codeValue;
                opt.textContent = s.codeName;
                sSel.appendChild(opt);
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
            const res = await App.get('/api/billings?' + qs);
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

document.addEventListener('DOMContentLoaded', BillingList.init);
