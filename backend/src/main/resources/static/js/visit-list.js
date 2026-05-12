/* ============================================================
 * 방문 이력 — 목록 + 검색 + AG Grid
 * 그리드 행 마다 [완료][취소] 액션 버튼 (SCHEDULED 일 때만 활성)
 * ============================================================ */

const VisitList = (() => {
    let gridApi = null;
    let currentPage = 0;
    let currentSize = 20;

    const statusBadge = (status) => {
        if (status === 'SCHEDULED') return '<span class="badge bg-blue-lt">예정</span>';
        if (status === 'COMPLETED') return '<span class="badge bg-green-lt">완료</span>';
        if (status === 'CANCELLED') return '<span class="badge bg-red-lt">취소</span>';
        return status;
    };

    const typeLabel = (type) => {
        if (type === 'INSTALL') return '설치';
        if (type === 'CHECK')   return '점검';
        if (type === 'COLLECT') return '수거';
        return type;
    };

    const actionRenderer = (p) => {
        if (p.data?.visitStatus !== 'SCHEDULED') {
            return '<span class="text-secondary small">—</span>';
        }
        const id = p.data.visitId;
        return `
            <button class="btn btn-sm btn-success me-1" onclick="VisitForm.complete(${id})">완료</button>
            <button class="btn btn-sm btn-danger" onclick="VisitForm.openCancel(${id})">취소</button>
        `;
    };

    const columnDefs = [
        { headerCheckboxSelection: true, checkboxSelection: true, width: 50, flex: 0,
          sortable: false, filter: false, resizable: false, pinned: 'left' },
        { headerName: 'ID',       field: 'visitId',     minWidth: 80,  flex: 0, type: 'rightAligned' },
        { headerName: '계약번호', field: 'contractNo',  minWidth: 200, flex: 0,
          cellRenderer: (p) => p.value ? `<a href="/admin/contracts/${p.data.contractId}" class="text-primary">${p.value}</a>` : '-' },
        { headerName: '기사',     minWidth: 180, flex: 1,
          valueGetter: (p) => p.data ? `${p.data.engineerName ?? '-'} (${p.data.engineerCode ?? '-'})` : '' },
        { headerName: '지역',     field: 'engineerArea', minWidth: 140, flex: 1 },
        { headerName: '유형',     field: 'visitType',    minWidth: 90,  flex: 0,
          cellRenderer: (p) => typeLabel(p.value) },
        { headerName: '예정일',   field: 'scheduledDate', minWidth: 120, flex: 0 },
        { headerName: '완료일',   field: 'completedDate', minWidth: 120, flex: 0 },
        { headerName: '상태',     field: 'visitStatus',  minWidth: 110, flex: 0,
          cellRenderer: (p) => statusBadge(p.value) },
        { headerName: '액션',     minWidth: 160, flex: 0,
          cellRenderer: actionRenderer, sortable: false, filter: false },
        { headerName: '등록일시', field: 'createdAt',    minWidth: 165, flex: 0, valueFormatter: dateTimeFormatter },
    ];

    function init() {
        const el = document.querySelector('#visitGrid');
        gridApi = agGrid.createGrid(el, {
            ...AgGridDefaults,
            columnDefs,
            rowData: [],
            onRowDoubleClicked: (e) => {
                if (e.data.visitStatus === 'SCHEDULED') VisitForm.openUpdate(e.data.visitId);
                else App.toast('SCHEDULED 상태만 수정 가능', 'warning');
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
        loadSearchOptions();
        load();
    }

    async function loadSearchOptions() {
        try {
            const [engineers, types] = await Promise.all([
                App.get('/api/engineers?size=500'),
                App.get('/api/code-groups/VISIT_TYPE/options'),
            ]);
            const eSel = document.querySelector('#searchEngineerId');
            for (const e of engineers.data?.content ?? []) {
                const opt = document.createElement('option');
                opt.value = e.engineerId;
                opt.textContent = `${e.engineerName} (${e.engineerCode})`;
                eSel.appendChild(opt);
            }
            const tSel = document.querySelector('#searchVisitType');
            for (const t of types.data ?? []) {
                const opt = document.createElement('option');
                opt.value = t.codeValue;
                opt.textContent = t.codeName;
                tSel.appendChild(opt);
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
            const res = await App.get('/api/visits?' + qs);
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

document.addEventListener('DOMContentLoaded', VisitList.init);
