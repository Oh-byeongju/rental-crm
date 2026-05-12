/* ============================================================
 * 배치 실행 이력 — 조회 전용. Ch.1 성능 측정 결과 확인.
 * DURATION_MS 컬럼이 학습 핵심.
 * ============================================================ */

const BatchLogList = (() => {
    let gridApi = null;
    let currentPage = 0;
    let currentSize = 20;

    const typeLabel = (t) => {
        if (t === 'BILLING_CREATE') return '월 청구 생성';
        if (t === 'OVERDUE_UPDATE') return '연체 갱신';
        return t;
    };
    const statusBadge = (s) => {
        if (s === 'RUNNING')   return '<span class="badge bg-blue-lt">실행 중</span>';
        if (s === 'COMPLETED') return '<span class="badge bg-green-lt">완료</span>';
        if (s === 'FAILED')    return '<span class="badge bg-red-lt">실패</span>';
        return s;
    };
    const durationFormat = (p) => {
        if (p.value == null) return '';
        const ms = Number(p.value);
        if (ms < 1000) return `${ms} ms`;
        return `${(ms / 1000).toFixed(2)} s`;
    };

    const columnDefs = [
        { headerName: 'ID',       field: 'batchLogId',  minWidth: 80,  flex: 0, type: 'rightAligned' },
        { headerName: '유형',     field: 'batchType',   minWidth: 130, flex: 0,
          cellRenderer: (p) => typeLabel(p.value) },
        { headerName: '청구월',   field: 'billingMonth', minWidth: 100, flex: 0 },
        { headerName: '상태',     field: 'batchStatus', minWidth: 100, flex: 0,
          cellRenderer: (p) => statusBadge(p.value) },
        { headerName: '대상',     field: 'targetCount',  minWidth: 80, flex: 0, type: 'rightAligned' },
        { headerName: '처리',     field: 'processCount', minWidth: 80, flex: 0, type: 'rightAligned' },
        { headerName: '성공',     field: 'successCount', minWidth: 80, flex: 0, type: 'rightAligned',
          cellRenderer: (p) => p.value > 0 ? `<span class="text-success">${p.value}</span>` : p.value },
        { headerName: '실패',     field: 'failCount',    minWidth: 80, flex: 0, type: 'rightAligned',
          cellRenderer: (p) => p.value > 0 ? `<span class="text-danger fw-bold">${p.value}</span>` : p.value },
        { headerName: '소요시간', field: 'durationMs',   minWidth: 110, flex: 0, type: 'rightAligned',
          valueFormatter: durationFormat,
          cellRenderer: (p) => p.value != null ? `<span class="fw-bold">${durationFormat(p)}</span>` : '-' },
        { headerName: '시작',     field: 'startedAt',    minWidth: 165, flex: 0, valueFormatter: dateTimeFormatter },
        { headerName: '완료',     field: 'completedAt',  minWidth: 165, flex: 0, valueFormatter: dateTimeFormatter },
        { headerName: '오류',     field: 'errorMsg',     minWidth: 200, flex: 1,
          cellRenderer: (p) => p.value ? `<span class="text-danger small">${p.value}</span>` : '-' },
    ];

    function init() {
        const el = document.querySelector('#batchLogGrid');
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
        load();
    }

    async function load() {
        const params = collectSearchParams();
        params.page = currentPage;
        params.size = currentSize;
        const qs = new URLSearchParams(params).toString();
        try {
            const res = await App.get('/api/batches?' + qs);
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

document.addEventListener('DOMContentLoaded', BatchLogList.init);
