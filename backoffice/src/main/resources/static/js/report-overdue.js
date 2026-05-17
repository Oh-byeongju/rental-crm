/* ============================================================
 * 미납 현황 리포트 (Ch.2) — 목록 조회 + 엑셀 스트리밍 다운로드
 * 미수납 청구 (UNPAID + OVERDUE) · 고객/계약 조인
 * 엑셀: /api/reports/overdue/excel — 0건이면 JSON 오류 → toast
 * ============================================================ */

const ReportOverdue = (() => {
    let gridApi = null;
    let currentPage = 0;
    let currentSize = 20;

    const moneyFormatter = (p) => (p.value == null) ? '' : Number(p.value).toLocaleString();
    const statusBadge = (code) => code === 'OVERDUE'
        ? '<span class="badge bg-red-lt">연체</span>'
        : code === 'UNPAID'
            ? '<span class="badge bg-yellow-lt">미납</span>'
            : (code ?? '-');
    const daysRenderer = (p) => {
        if (p.value == null) return '-';
        const d = Number(p.value);
        if (d >= 60) return `<span class="text-danger fw-bold">${d}일</span>`;
        if (d >= 30) return `<span class="text-warning fw-bold">${d}일</span>`;
        return `${d}일`;
    };

    const columnDefs = [
        { headerName: '청구번호', field: 'billingNo',    minWidth: 210, flex: 0 },
        { headerName: '청구월',   field: 'billingMonth', minWidth: 100, flex: 0 },
        { headerName: '고객',     minWidth: 180, flex: 1,
          valueGetter: (p) => p.data ? `${p.data.customerName ?? '-'} (${p.data.customerNo ?? '-'})` : '' },
        { headerName: '연락처',   field: 'phone',        minWidth: 140, flex: 0 },
        { headerName: '계약번호', field: 'contractNo',   minWidth: 170, flex: 0 },
        { headerName: '청구액',   field: 'billingAmount', minWidth: 130, flex: 0,
          type: 'rightAligned', valueFormatter: moneyFormatter },
        { headerName: '납기일',   field: 'dueDate',      minWidth: 120, flex: 0 },
        { headerName: '상태',     field: 'billingStatus', minWidth: 100, flex: 0,
          cellRenderer: (p) => statusBadge(p.value) },
        { headerName: '연체일수', field: 'overdueDays',  minWidth: 110, flex: 0,
          type: 'rightAligned', cellRenderer: daysRenderer },
    ];

    function init() {
        const el = document.querySelector('#reportOverdueGrid');
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

    function collectSearchParams() {
        const form = document.querySelector('#searchForm');
        const data = new FormData(form);
        const out = {};
        for (const [k, v] of data.entries()) if (v) out[k] = v;
        return out;
    }

    async function load() {
        const params = collectSearchParams();
        params.page = currentPage;
        params.size = currentSize;
        const qs = new URLSearchParams(params).toString();
        try {
            const res = await App.get('/api/reports/overdue?' + qs);
            gridApi.setGridOption('rowData', res.data.content);
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    function search(event) { event.preventDefault(); currentPage = 0; load(); return false; }
    function reset() { document.querySelector('#searchForm').reset(); currentPage = 0; load(); }

    /** 엑셀 스트리밍 다운로드. 0건이면 서버가 JSON 오류 → toast 로 안내. */
    async function downloadExcel() {
        const qs = new URLSearchParams(collectSearchParams()).toString();
        try {
            const res = await fetch('/api/reports/overdue/excel?' + qs, {
                credentials: 'same-origin',
            });
            const ct = res.headers.get('content-type') || '';
            if (!res.ok || ct.includes('application/json')) {
                const payload = ct.includes('application/json') ? await res.json() : null;
                App.toast((payload && payload.message) || `다운로드 실패 (HTTP ${res.status})`, 'error');
                return;
            }
            const blob = await res.blob();
            const cd = res.headers.get('content-disposition') || '';
            const m = cd.match(/filename="?([^"]+)"?/);
            const fname = m ? m[1] : 'overdue-report.xlsx';
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = fname;
            document.body.appendChild(a);
            a.click();
            a.remove();
            URL.revokeObjectURL(url);
            App.toast('엑셀 다운로드를 시작합니다', 'success');
        } catch (e) {
            App.toast(e.message || '다운로드 실패', 'error');
        }
    }

    return { init, load, search, reset, downloadExcel };
})();

document.addEventListener('DOMContentLoaded', ReportOverdue.init);
