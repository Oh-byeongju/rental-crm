/* ============================================================
 * 알림 관리 — 목록 + 검색 + 읽음 처리
 * X-User-Id 헤더: 현재 admin Seeder 첫 사용자 (ID=1) 하드코딩.
 *                 TODO: JWT 도입 시 인터셉터로 대체 (ADR-010 §2-8).
 * ============================================================ */

const NotificationList = (() => {
    const USER_ID = '1';
    let gridApi = null;
    let currentPage = 0;
    let currentSize = 20;

    /** App.get/put 은 X-User-Id 미지원 → 본 화면 전용 fetch helper. */
    async function api(method, url, body) {
        const opts = {
            method,
            headers: {
                'Content-Type': 'application/json',
                'X-User-Id': USER_ID,
            },
            credentials: 'same-origin',
        };
        if (body !== undefined && body !== null) opts.body = JSON.stringify(body);
        const res = await fetch(url, opts);
        const payload = (res.headers.get('content-type') || '').includes('application/json')
            ? await res.json() : await res.text();
        if (!res.ok) throw new Error((payload && payload.message) || `HTTP ${res.status}`);
        return payload;
    }

    const typeBadge = (t) => {
        const cls = {
            BILLING_CREATED:   'bg-blue-lt',
            PAYMENT_COMPLETED: 'bg-green-lt',
            PAYMENT_OVERDUE:   'bg-red-lt',
            VISIT_ASSIGNED:    'bg-purple-lt',
        }[t] || 'bg-secondary-lt';
        return `<span class="badge ${cls}">${t || '-'}</span>`;
    };
    const readBadge = (r) => r === 'Y'
        ? '<span class="badge bg-secondary-lt">읽음</span>'
        : '<span class="badge bg-yellow-lt">미읽음</span>';

    const columnDefs = [
        { headerName: 'ID',       field: 'notificationId',   minWidth: 80,  flex: 0, type: 'rightAligned' },
        { headerName: '종류',     field: 'notificationType', minWidth: 150, flex: 0,
          cellRenderer: (p) => typeBadge(p.value) },
        { headerName: '메시지',   field: 'message',          minWidth: 280, flex: 2 },
        { headerName: '참조',     minWidth: 160, flex: 1,
          valueGetter: (p) => p.data
              ? (p.data.refType ? `${p.data.refType} #${p.data.refId ?? '-'}` : '-')
              : '' },
        { headerName: '상태',     field: 'readYn',           minWidth: 100, flex: 0,
          cellRenderer: (p) => readBadge(p.value) },
        { headerName: '발생일시', field: 'createdAt',        minWidth: 165, flex: 0, valueFormatter: dateTimeFormatter },
    ];

    function init() {
        const el = document.querySelector('#notificationGrid');
        gridApi = agGrid.createGrid(el, {
            ...AgGridDefaults,
            columnDefs,
            rowData: [],
            onRowDoubleClicked: async (e) => {
                if (e.data.readYn === 'Y') {
                    App.toast('이미 읽은 알림', 'info');
                    return;
                }
                try {
                    await api('PUT', `/api/notifications/${e.data.notificationId}/read`);
                    App.toast('읽음 처리되었습니다', 'success');
                    load();
                } catch (err) {
                    App.toast(err.message, 'error');
                }
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
        load();
    }

    async function load() {
        const params = collectSearchParams();
        params.page = currentPage;
        params.size = currentSize;
        const qs = new URLSearchParams(params).toString();
        try {
            const res = await api('GET', '/api/notifications?' + qs);
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

    async function markAllRead() {
        if (!confirm('모든 미읽음 알림을 읽음 처리합니다. 계속할까요?')) return;
        try {
            const res = await api('PUT', '/api/notifications/read-all');
            App.toast(res.message || '전체 읽음 처리되었습니다', 'success');
            load();
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    return { init, load, search, reset, markAllRead };
})();

document.addEventListener('DOMContentLoaded', NotificationList.init);
