/* ============================================================
 * 장비 관리 — 목록 + 검색 + AG Grid
 * 07 §X 매핑 — 단순 CRUD, EQUIPMENT_TYPE selectbox 검색
 * ============================================================ */

const EquipmentList = (() => {
    let gridApi = null;
    let currentPage = 0;
    let currentSize = 20;

    const columnDefs = [
        { headerCheckboxSelection: true, checkboxSelection: true, width: 50, flex: 0,
          sortable: false, filter: false, resizable: false, pinned: 'left' },
        { headerName: 'ID',       field: 'equipmentId',   minWidth: 80,  flex: 0, type: 'rightAligned' },
        { headerName: '장비코드', field: 'equipmentCode', minWidth: 130, flex: 0 },
        { headerName: '유형',     field: 'equipmentTypeName', minWidth: 110, flex: 0,
          cellRenderer: (p) => p.value
              ? `<span class="badge bg-blue-lt">${p.value}</span>`
              : `<span class="text-secondary">${p.data.equipmentType ?? '—'}</span>` },
        { headerName: '모델명',   field: 'modelName',     minWidth: 180, flex: 2 },
        { headerName: '제조사',   field: 'manufacturer',  minWidth: 140, flex: 1 },
        { headerName: '출시일',   field: 'releaseDate',   minWidth: 120, flex: 0 },
        { headerName: '재고',     field: 'stockQty',      minWidth: 80, flex: 0, type: 'rightAligned',
          cellRenderer: (p) => (p.value > 0)
              ? `<span class="text-success fw-bold">${p.value}</span>`
              : '<span class="badge bg-red-lt">0</span>' },
        { headerName: '사용',     field: 'useYn',         minWidth: 90, flex: 0,
          cellRenderer: (p) => p.value === 'Y'
              ? '<span class="badge bg-green-lt">사용</span>'
              : '<span class="badge bg-secondary-lt">단종</span>' },
        { headerName: '등록일시', field: 'createdAt',     minWidth: 165, flex: 0, valueFormatter: dateTimeFormatter },
    ];

    function init() {
        const el = document.querySelector('#equipmentGrid');
        const opts = {
            ...AgGridDefaults,
            columnDefs,
            rowData: [],
            onRowDoubleClicked: (e) => EquipmentForm.openUpdate(e.data.equipmentId),
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
        loadSearchTypes();
        load();
    }

    /** 검색폼의 유형 selectbox 채움 (EQUIPMENT_TYPE 그룹). */
    async function loadSearchTypes() {
        try {
            const res = await App.get('/api/code-groups/EQUIPMENT_TYPE/options');
            const sel = document.querySelector('#searchEquipmentType');
            for (const c of res.data ?? []) {
                const opt = document.createElement('option');
                opt.value = c.codeValue;
                opt.textContent = c.codeName;
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
            const res = await App.get('/api/equipments?' + qs);
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

document.addEventListener('DOMContentLoaded', EquipmentList.init);
