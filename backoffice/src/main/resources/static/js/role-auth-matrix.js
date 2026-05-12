/* ============================================================
 * 권한 매트릭스 — 역할 × AUTH 키 체크박스
 *
 * 데이터 흐름:
 *   페이지 진입 시:
 *     GET /api/auths                  - 전체 AUTH 마스터
 *     GET /api/roles/{roleId}/auths   - 현재 부여된 AUTH_CODE 셋
 *     → 그리드에 합쳐 표시 (granted: true/false)
 *
 *   저장 시:
 *     PUT /api/roles/{roleId}/auths { authCodes: [...] }
 *
 * SUPER_ADMIN: 체크박스 disabled + 저장 비활성.
 * 클라이언트 필터 (메뉴/AUTH_TYPE/부여상태) — gridApi.setFilterModel 없이 setRowData 재설정.
 * ============================================================ */

const RoleAuthMatrix = (() => {
    let gridApi = null;
    let allRows = [];      // 전체 (필터 전 원본)
    let isSuperAdmin = false;
    let roleId = null;

    function getRoleId() {
        return roleId ?? (roleId = Number(document.querySelector('#roleId').value));
    }

    function readSuperAdmin() {
        return document.querySelector('#roleSuperAdmin').value === 'true';
    }

    function typeBadge(p) {
        const map = {
            VIEW:    'bg-blue-lt',
            CREATE:  'bg-green-lt',
            UPDATE:  'bg-yellow-lt',
            DELETE:  'bg-red-lt',
            EXECUTE: 'bg-purple-lt',
        };
        return `<span class="badge ${map[p.value] || 'bg-secondary-lt'}">${p.value}</span>`;
    }

    function checkboxRenderer(p) {
        const checked = p.value === true ? 'checked' : '';
        const disabled = isSuperAdmin ? 'disabled' : '';
        const code = p.data.authCode;
        return `<input type="checkbox" class="form-check-input m-0" ${checked} ${disabled}
                       onchange="RoleAuthMatrix.toggleOne('${code}', this.checked)">`;
    }

    const columnDefs = [
        { headerName: '메뉴',      field: 'menuName',  minWidth: 160, flex: 1 },
        { headerName: 'AUTH 코드', field: 'authCode',  minWidth: 220, flex: 1 },
        { headerName: '권한명',    field: 'authName',  minWidth: 220, flex: 2 },
        { headerName: '유형',      field: 'authType',  minWidth: 110, flex: 0, cellRenderer: typeBadge },
        { headerName: '부여',      field: 'granted',   minWidth: 90,  flex: 0,
          sortable: false, filter: false, cellRenderer: checkboxRenderer },
    ];

    async function init() {
        isSuperAdmin = readSuperAdmin();
        if (isSuperAdmin) {
            document.querySelector('#btnSave').disabled = true;
        }

        const el = document.querySelector('#matrixGrid');
        const opts = {
            ...AgGridDefaults,
            paginationPageSize: 100,
            paginationPageSizeSelector: [50, 100, 200],
            columnDefs,
            rowData: [],
        };
        gridApi = agGrid.createGrid(el, opts);

        try {
            const [authsRes, grantedRes] = await Promise.all([
                App.get('/api/auths'),
                App.get(`/api/roles/${getRoleId()}/auths`),
            ]);
            const granted = new Set(grantedRes.data || []);
            allRows = (authsRes.data || []).map(a => ({
                authCode: a.authCode,
                authName: a.authName,
                menuId:   a.menuId,
                menuName: a.menuName ?? '-',
                authType: a.authType,
                granted:  granted.has(a.authCode),
            }));
            applyFilter();
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    function toggleOne(authCode, checked) {
        const row = allRows.find(r => r.authCode === authCode);
        if (row) row.granted = !!checked;
    }

    function toggleAll(checked) {
        if (isSuperAdmin) return;
        const visible = getVisibleRows();
        const visibleCodes = new Set(visible.map(r => r.authCode));
        for (const row of allRows) {
            if (visibleCodes.has(row.authCode)) row.granted = checked;
        }
        applyFilter();
    }

    function getVisibleRows() {
        const menuQ = (document.querySelector('#filterMenu').value || '').trim().toLowerCase();
        const typeQ = document.querySelector('#filterType').value || '';
        const grantQ = document.querySelector('#filterGranted').value || '';

        return allRows.filter(r => {
            if (menuQ && !(r.menuName || '').toLowerCase().includes(menuQ)) return false;
            if (typeQ && r.authType !== typeQ) return false;
            if (grantQ === 'Y' && !r.granted) return false;
            if (grantQ === 'N' && r.granted) return false;
            return true;
        });
    }

    function applyFilter() {
        gridApi.setGridOption('rowData', getVisibleRows());
    }

    async function save() {
        if (isSuperAdmin) {
            App.toast('SUPER_ADMIN 매핑은 변경할 수 없습니다', 'error');
            return;
        }
        const granted = allRows.filter(r => r.granted).map(r => r.authCode);
        try {
            const res = await App.put(`/api/roles/${getRoleId()}/auths`, { authCodes: granted });
            App.toast(res.message || '저장되었습니다', 'success');
        } catch (e) {
            App.toast(e.message || '저장 실패', 'error');
        }
    }

    return { init, save, toggleOne, toggleAll, applyFilter };
})();

document.addEventListener('DOMContentLoaded', RoleAuthMatrix.init);
