/* ============================================================
 * 사용자 권한 미세 조정 (ADR-009)
 *
 * 데이터 흐름:
 *   페이지 진입 시:
 *     GET /api/auths               - 전체 AUTH 마스터 (메뉴명 포함)
 *     GET /api/users/{u}/auths     - { roleAuths, userGrants, userRevokes, effective }
 *
 *   행별 상태:
 *     role     = 역할이 가진 권한 (체크박스 + readonly)
 *     adjust   = '', 'GRANT', 'REVOKE' (드롭다운)
 *     effective= role ∪ GRANT − REVOKE
 *
 *   저장:
 *     PUT /api/users/{u}/auths { grants: [...], revokes: [...] }
 * ============================================================ */

const UserAuthMatrix = (() => {
    let gridApi = null;
    let allRows = [];      // 전체 (필터 전 원본)
    let userId = null;
    let roleCode = null;

    function getUserId() {
        return userId ?? (userId = Number(document.querySelector('#userId').value));
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

    function roleCellRenderer(p) {
        return p.value
            ? '<i class="ti ti-check text-success"></i>'
            : '<span class="text-secondary">—</span>';
    }

    function adjustCellRenderer(p) {
        const code = p.data.authCode;
        const current = p.value || '';
        return `<select class="form-select form-select-sm" onchange="UserAuthMatrix.setAdjust('${code}', this.value)">
            <option value=""       ${current === ''       ? 'selected' : ''}>—</option>
            <option value="GRANT"  ${current === 'GRANT'  ? 'selected' : ''}>GRANT (추가)</option>
            <option value="REVOKE" ${current === 'REVOKE' ? 'selected' : ''}>REVOKE (제외)</option>
        </select>`;
    }

    function effectiveCellRenderer(p) {
        return p.value
            ? '<span class="badge bg-green">부여</span>'
            : '<span class="badge bg-secondary-lt">미부여</span>';
    }

    const columnDefs = [
        { headerName: '메뉴',      field: 'menuName',  minWidth: 140, flex: 1 },
        { headerName: 'AUTH 코드', field: 'authCode',  minWidth: 220, flex: 1 },
        { headerName: '권한명',    field: 'authName',  minWidth: 200, flex: 2 },
        { headerName: '유형',      field: 'authType',  minWidth: 110, flex: 0, cellRenderer: typeBadge },
        { headerName: '역할 권한', field: 'role',      minWidth: 100, flex: 0, cellRenderer: roleCellRenderer },
        { headerName: '개인 조정', field: 'adjust',    minWidth: 160, flex: 0, sortable: false, filter: false,
          cellRenderer: adjustCellRenderer },
        { headerName: '최종',      field: 'effective', minWidth: 90,  flex: 0, cellRenderer: effectiveCellRenderer },
    ];

    function computeEffective(row) {
        // role 이 true 면 기본 부여, REVOKE 면 해제
        // role 이 false 면 기본 미부여, GRANT 면 부여
        if (row.adjust === 'REVOKE') return false;
        if (row.adjust === 'GRANT')  return true;
        return row.role;
    }

    async function init() {
        roleCode = document.querySelector('#roleCode').value;
        if (roleCode === 'SUPER_ADMIN') {
            document.querySelector('#superAdminAlert').style.display = 'block';
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
            const [authsRes, matrixRes] = await Promise.all([
                App.get('/api/auths'),
                App.get(`/api/users/${getUserId()}/auths`),
            ]);
            const roleSet   = new Set(matrixRes.data.roleAuths   || []);
            const grantSet  = new Set(matrixRes.data.userGrants  || []);
            const revokeSet = new Set(matrixRes.data.userRevokes || []);

            allRows = (authsRes.data || []).map(a => {
                const isRole = roleSet.has(a.authCode);
                let adjust = '';
                if (grantSet.has(a.authCode))  adjust = 'GRANT';
                if (revokeSet.has(a.authCode)) adjust = 'REVOKE';
                const row = {
                    authCode: a.authCode,
                    authName: a.authName,
                    menuId:   a.menuId,
                    menuName: a.menuName ?? '-',
                    authType: a.authType,
                    role:     isRole,
                    adjust:   adjust,
                };
                row.effective = computeEffective(row);
                return row;
            });
            applyFilter();
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    function setAdjust(authCode, value) {
        const row = allRows.find(r => r.authCode === authCode);
        if (!row) return;
        row.adjust    = value;
        row.effective = computeEffective(row);
        applyFilter();
    }

    function getVisibleRows() {
        const menuQ  = (document.querySelector('#filterMenu').value || '').trim().toLowerCase();
        const typeQ  = document.querySelector('#filterType').value || '';
        const stateQ = document.querySelector('#filterState').value || '';

        return allRows.filter(r => {
            if (menuQ && !(r.menuName || '').toLowerCase().includes(menuQ)) return false;
            if (typeQ && r.authType !== typeQ) return false;
            if (stateQ === 'effective' && !r.effective) return false;
            if (stateQ === 'role'      && !r.role)      return false;
            if (stateQ === 'grant'     && r.adjust !== 'GRANT')  return false;
            if (stateQ === 'revoke'    && r.adjust !== 'REVOKE') return false;
            return true;
        });
    }

    function applyFilter() {
        gridApi.setGridOption('rowData', getVisibleRows());
    }

    async function save() {
        const grants  = allRows.filter(r => r.adjust === 'GRANT').map(r => r.authCode);
        const revokes = allRows.filter(r => r.adjust === 'REVOKE').map(r => r.authCode);
        try {
            const res = await App.put(`/api/users/${getUserId()}/auths`, { grants, revokes });
            App.toast(res.message || '저장되었습니다', 'success');
        } catch (e) {
            App.toast(e.message || '저장 실패', 'error');
        }
    }

    return { init, save, setAdjust, applyFilter };
})();

document.addEventListener('DOMContentLoaded', UserAuthMatrix.init);
