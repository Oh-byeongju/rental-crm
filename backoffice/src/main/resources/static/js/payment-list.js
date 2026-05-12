/* ============================================================
 * 수납 관리 — 목록 + 검색 + AG Grid
 * 기본 정렬: paymentId DESC (최신 등록 우선)
 * 행 클릭 = 상세 페이지 이동
 * ============================================================ */

const PaymentList = (() => {
    let gridApi = null;
    let currentPage = 0;
    let currentSize = 20;

    const moneyFormatter = (p) => (p.value == null) ? '' : Number(p.value).toLocaleString();

    const statusBadge = (s) => {
        const cls = {
            COMPLETED: 'bg-green-lt',
            CANCELLED: 'bg-red-lt',
            REFUNDED:  'bg-orange-lt',
        }[s] || 'bg-secondary-lt';
        return `<span class="badge ${cls}">${s || '-'}</span>`;
    };

    const methodBadge = (m) => {
        const cls = {
            CARD: 'bg-blue-lt',
            BANK: 'bg-cyan-lt',
            CASH: 'bg-green-lt',
            TOSS: 'bg-purple-lt',
        }[m] || 'bg-secondary-lt';
        return `<span class="badge ${cls}">${m || '-'}</span>`;
    };

    const columnDefs = [
        { headerName: 'ID',       field: 'paymentId',     minWidth: 80,  flex: 0, type: 'rightAligned' },
        { headerName: '수납번호', field: 'paymentNo',     minWidth: 180, flex: 0 },
        { headerName: '청구번호', field: 'billingNo',     minWidth: 180, flex: 0,
          cellRenderer: (p) => p.value
              ? `<a href="/admin/billings" class="text-primary" onclick="event.stopPropagation()">${p.value}</a>` : '-' },
        { headerName: '청구월',   field: 'billingMonth',  minWidth: 100, flex: 0 },
        { headerName: '고객',     minWidth: 160, flex: 1,
          valueGetter: (p) => p.data ? `${p.data.customerName ?? '-'} (${p.data.customerNo ?? '-'})` : '' },
        { headerName: '수납금액', field: 'paymentAmount', minWidth: 130, flex: 0, type: 'rightAligned', valueFormatter: moneyFormatter },
        { headerName: '결제수단', field: 'paymentMethod', minWidth: 110, flex: 0,
          cellRenderer: (p) => methodBadge(p.value) },
        { headerName: '수납일',   field: 'paymentDate',   minWidth: 110, flex: 0 },
        { headerName: '상태',     field: 'paymentStatus', minWidth: 110, flex: 0,
          cellRenderer: (p) => statusBadge(p.value) },
        { headerName: '등록일시', field: 'createdAt',     minWidth: 165, flex: 0, valueFormatter: dateTimeFormatter },
    ];

    function init() {
        const el = document.querySelector('#paymentGrid');
        gridApi = agGrid.createGrid(el, {
            ...AgGridDefaults,
            columnDefs,
            rowData: [],
            onRowClicked: (e) => {
                if (!e.data || !e.data.paymentId) return;
                location.href = `/admin/payments/${e.data.paymentId}`;
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
            const res = await App.get('/api/payments?' + qs);
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

document.addEventListener('DOMContentLoaded', PaymentList.init);
