/* ============================================================
 * 메뉴 관리 — 평탄 그리드 + 들여쓰기 (depth 따라 padding)
 * 04 §1-1 + 07 §4
 * ============================================================ */

const MenuList = (() => {
    let gridApi = null;
    let currentPage = 0;
    let currentSize = 50;

    /** 깊이별 들여쓰기 (depth 1 = 0px, depth 2 = 24px). */
    function nameCellRenderer(p) {
        const depth = p.data?.menuDepth ?? 1;
        const padPx = (depth - 1) * 24;
        const prefix = depth > 1 ? '└ ' : '';
        const icon = p.data?.iconClass
            ? `<i class="${p.data.iconClass} me-1"></i>`
            : '';
        return `<span style="padding-left:${padPx}px">${prefix}${icon}${p.value ?? ''}</span>`;
    }

    function typeBadgeRenderer(p) {
        return p.value === 'GROUP'
            ? '<span class="badge bg-blue-lt">GROUP</span>'
            : '<span class="badge bg-purple-lt">LEAF</span>';
    }

    const columnDefs = [
        { headerCheckboxSelection: true, checkboxSelection: true, width: 50, flex: 0,
          sortable: false, filter: false, resizable: false, pinned: 'left' },
        { headerName: '메뉴명',   field: 'menuName',  minWidth: 240, flex: 2, cellRenderer: nameCellRenderer },
        { headerName: '타입',     field: 'menuType',  minWidth: 100, flex: 0, cellRenderer: typeBadgeRenderer },
        { headerName: 'URL',      field: 'menuUrl',   minWidth: 200, flex: 2 },
        { headerName: '아이콘',   field: 'iconClass', minWidth: 140, flex: 1 },
        { headerName: '정렬',     field: 'sortOrder', minWidth: 80,  flex: 0, type: 'rightAligned' },
        { headerName: 'depth',    field: 'menuDepth', minWidth: 80,  flex: 0, type: 'rightAligned' },
        { headerName: '사용',     field: 'useYn',     minWidth: 90,  flex: 0,
          cellRenderer: (p) => p.value === 'Y'
              ? '<span class="badge bg-green-lt">사용</span>'
              : '<span class="badge bg-secondary-lt">미사용</span>' },
        { headerName: '등록일시', field: 'createdAt', minWidth: 165, flex: 0, valueFormatter: dateTimeFormatter },
    ];

    function init() {
        const el = document.querySelector('#menuGrid');
        const opts = {
            ...AgGridDefaults,
            paginationPageSize: currentSize,
            paginationPageSizeSelector: [50, 100, 200],
            columnDefs,
            rowData: [],
            onRowDoubleClicked: (e) => MenuForm.openUpdate(e.data.menuId),
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
            const res = await App.get('/api/menus/flat?' + qs);
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

document.addEventListener('DOMContentLoaded', MenuList.init);
