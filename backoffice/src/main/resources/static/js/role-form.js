/* ============================================================
 * 역할 등록/수정 모달
 *
 * 정책 (04 §1-3, ADR-008):
 *  - SUPER_ADMIN 은 수정 불가 (서버 거부 + UI 차단)
 *  - 등록: roleCode 입력. useYn 숨김 (기본 'Y').
 *  - 수정: roleCode readonly. useYn 노출.
 * ============================================================ */

const RoleForm = (() => {
    let modalInstance = null;
    let mode = 'create';

    function getModal() {
        if (!modalInstance) {
            modalInstance = new bootstrap.Modal(document.querySelector('#roleModal'));
        }
        return modalInstance;
    }

    function openCreate() {
        mode = 'create';
        document.querySelector('#roleModalTitle').textContent = '역할 등록';
        const form = document.querySelector('#roleForm');
        form.reset();
        form.elements.roleId.value = '';
        form.elements.roleCode.readOnly = false;
        form.elements.useYn.value = 'Y';
        document.querySelector('#useYnRow').classList.add('d-none');
        getModal().show();
    }

    async function openUpdate(roleId) {
        mode = 'update';
        try {
            const res = await App.get(`/api/roles/${roleId}`);
            const r = res.data;
            if (r.superAdmin) {
                App.toast('SUPER_ADMIN 역할은 수정할 수 없습니다', 'error');
                return;
            }
            document.querySelector('#roleModalTitle').textContent = `역할 수정 — ${r.roleCode}`;
            const form = document.querySelector('#roleForm');
            form.reset();
            form.elements.roleId.value      = r.roleId;
            form.elements.roleCode.value    = r.roleCode;
            form.elements.roleCode.readOnly = true;
            form.elements.roleName.value    = r.roleName;
            form.elements.description.value = r.description ?? '';
            form.elements.useYn.value       = r.useYn ?? 'Y';
            document.querySelector('#useYnRow').classList.remove('d-none');
            getModal().show();
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    async function save() {
        const form = document.querySelector('#roleForm');
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }
        const data = formToObject(form);

        try {
            if (mode === 'create') {
                const payload = {
                    roleCode:    data.roleCode,
                    roleName:    data.roleName,
                    description: data.description || null,
                };
                const res = await App.post('/api/roles', payload);
                App.toast(res.message || '등록되었습니다', 'success');
            } else {
                const payload = {
                    roleName:    data.roleName,
                    description: data.description || null,
                    useYn:       data.useYn || 'Y',
                };
                const res = await App.put(`/api/roles/${data.roleId}`, payload);
                App.toast(res.message || '수정되었습니다', 'success');
            }
            getModal().hide();
            RoleList.load();
        } catch (e) {
            const detail = (e.errors && e.errors.length)
                ? e.errors.map(f => `${f.field}: ${f.reason}`).join(', ')
                : '';
            App.toast((e.message || '저장 실패') + (detail ? ` (${detail})` : ''), 'error');
        }
    }

    function formToObject(form) {
        const data = new FormData(form);
        const obj = {};
        for (const [k, v] of data.entries()) {
            obj[k] = v === '' ? null : v;
        }
        return obj;
    }

    return { openCreate, openUpdate, save };
})();
