/* ============================================================
 * 계약 관리 — 목록 + 검색 + AG Grid
 * 계약번호 클릭 = 별도 상세 페이지 (/admin/contracts/{id})
 * 상태 전이 액션은 상세 페이지에서
 * ============================================================ */

const ContractList = (() => {
    let gridApi = null;
    let currentPage = 0;
    let currentSize = 20;

    const moneyFormatter = (p) => (p.value == null) ? '' : Number(p.value).toLocaleString();
    const statusBadge = (status) => {
        if (status === 'ACTIVE')     return '<span class="badge bg-green-lt">정상</span>';
        if (status === 'SUSPENDED')  return '<span class="badge bg-orange-lt">일시정지</span>';
        if (status === 'TERMINATED') return '<span class="badge bg-red-lt">해지</span>';
        return status;
    };

    const columnDefs = [
        { headerCheckboxSelection: true, checkboxSelection: true, width: 50, flex: 0,
          sortable: false, filter: false, resizable: false, pinned: 'left' },
        { headerName: '계약번호',  field: 'contractNo', minWidth: 200, flex: 0,
          cellRenderer: (p) => `<a href="/admin/contracts/${p.data.contractId}" class="text-primary fw-bold">${p.value}</a>` },
        { headerName: '고객',      minWidth: 160, flex: 1,
          valueGetter: (p) => p.data ? `${p.data.customerName ?? '-'} (${p.data.customerNo ?? '-'})` : '' },
        { headerName: '상품 / 장비', minWidth: 220, flex: 2,
          valueGetter: (p) => p.data ? `${p.data.productName ?? '-'} / ${p.data.equipmentModelName ?? '-'}` : '' },
        { headerName: '월 렌탈료', field: 'monthlyFee', minWidth: 130, flex: 0, type: 'rightAligned', valueFormatter: moneyFormatter },
        { headerName: '시작일',    field: 'startDate',  minWidth: 120, flex: 0 },
        { headerName: '종료일',    field: 'endDate',    minWidth: 120, flex: 0 },
        { headerName: '상태',      field: 'contractStatus', minWidth: 110, flex: 0,
          cellRenderer: (p) => statusBadge(p.value) },
        { headerName: '등록일시',  field: 'createdAt',  minWidth: 165, flex: 0, valueFormatter: dateTimeFormatter },
    ];

    function init() {
        const el = document.querySelector('#contractGrid');
        gridApi = agGrid.createGrid(el, {
            ...AgGridDefaults,
            columnDefs,
            rowData: [],
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

    /** 검색폼 selectbox (고객/상품/상태) 일괄 로드. */
    async function loadSearchOptions() {
        try {
            const [customers, products, statusOpts] = await Promise.all([
                App.get('/api/customers?size=500'),
                App.get('/api/products?size=500'),
                App.get('/api/code-groups/CONTRACT_STATUS/options'),
            ]);
            const cSel = document.querySelector('#searchCustomerId');
            for (const c of customers.data?.content ?? []) {
                const opt = document.createElement('option');
                opt.value = c.customerId;
                opt.textContent = `${c.customerNo} - ${c.customerName}`;
                cSel.appendChild(opt);
            }
            const pSel = document.querySelector('#searchProductId');
            for (const p of products.data?.content ?? []) {
                const opt = document.createElement('option');
                opt.value = p.productId;
                opt.textContent = `${p.productCode} - ${p.productName}`;
                pSel.appendChild(opt);
            }
            const sSel = document.querySelector('#searchStatus');
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
            const res = await App.get('/api/contracts?' + qs);
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

    function search(event) {
        event.preventDefault();
        currentPage = 0;
        load();
        return false;
    }

    function reset() {
        document.querySelector('#searchForm').reset();
        currentPage = 0;
        load();
    }

    return { init, load, search, reset };
})();

document.addEventListener('DOMContentLoaded', ContractList.init);
