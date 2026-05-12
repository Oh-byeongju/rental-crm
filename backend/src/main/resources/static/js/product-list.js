/* ============================================================
 * 상품 관리 — 목록 + 검색 + AG Grid
 * 단순 CRUD + 장비 FK + 4 NUMBER 컬럼 (월렌탈료/계약개월/보증금/설치비)
 * ============================================================ */

const ProductList = (() => {
    let gridApi = null;
    let currentPage = 0;
    let currentSize = 20;

    const moneyFormatter = (p) => (p.value == null) ? '' : Number(p.value).toLocaleString();

    const columnDefs = [
        { headerCheckboxSelection: true, checkboxSelection: true, width: 50, flex: 0,
          sortable: false, filter: false, resizable: false, pinned: 'left' },
        { headerName: 'ID',         field: 'productId',   minWidth: 80,  flex: 0, type: 'rightAligned' },
        { headerName: '상품코드',   field: 'productCode', minWidth: 140, flex: 0 },
        { headerName: '장비',       minWidth: 240, flex: 2,
          valueGetter: (p) => p.data ? `${p.data.equipmentCode ?? '-'} / ${p.data.equipmentModelName ?? '-'}` : '',
          cellRenderer: (p) => {
              const code  = p.data?.equipmentCode ?? '-';
              const model = p.data?.equipmentModelName ?? '-';
              const stock = p.data?.equipmentStockQty ?? 0;
              const stockBadge = stock > 0
                  ? `<span class="badge bg-green-lt ms-1">재고 ${stock}</span>`
                  : '<span class="badge bg-red-lt ms-1">재고 0</span>';
              return `<span class="text-secondary">${code}</span> / ${model}${stockBadge}`;
          } },
        { headerName: '상품명',     field: 'productName',     minWidth: 180, flex: 2 },
        { headerName: '월 렌탈료', field: 'monthlyFee',      minWidth: 130, flex: 0, type: 'rightAligned',
          valueFormatter: moneyFormatter },
        { headerName: '계약',       field: 'contractMonths',  minWidth: 90,  flex: 0, type: 'rightAligned',
          valueFormatter: (p) => p.value != null ? `${p.value}개월` : '' },
        { headerName: '보증금',     field: 'depositAmount',   minWidth: 120, flex: 0, type: 'rightAligned',
          valueFormatter: moneyFormatter },
        { headerName: '설치비',     field: 'installFee',      minWidth: 120, flex: 0, type: 'rightAligned',
          valueFormatter: moneyFormatter },
        { headerName: '사용',       field: 'useYn',           minWidth: 90,  flex: 0,
          cellRenderer: (p) => p.value === 'Y'
              ? '<span class="badge bg-green-lt">사용</span>'
              : '<span class="badge bg-secondary-lt">미사용</span>' },
        { headerName: '등록일시',   field: 'createdAt',       minWidth: 165, flex: 0, valueFormatter: dateTimeFormatter },
    ];

    function init() {
        const el = document.querySelector('#productGrid');
        const opts = {
            ...AgGridDefaults,
            columnDefs,
            rowData: [],
            onRowDoubleClicked: (e) => ProductForm.openUpdate(e.data.productId),
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
        loadSearchEquipments();
        load();
    }

    /** 검색폼의 장비 selectbox 채움 — 전체 장비 (USE_YN 제약 없음, 정책 합의). */
    async function loadSearchEquipments() {
        try {
            const res = await App.get('/api/equipments?size=500');
            const sel = document.querySelector('#searchEquipmentId');
            for (const e of res.data?.content ?? []) {
                const opt = document.createElement('option');
                opt.value = e.equipmentId;
                opt.textContent = `${e.equipmentCode} - ${e.modelName}`;
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
            const res = await App.get('/api/products?' + qs);
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

document.addEventListener('DOMContentLoaded', ProductList.init);
