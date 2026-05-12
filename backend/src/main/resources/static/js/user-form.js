/* ============================================================
 * 관리자 등록/수정 + 잠금해제 + 비밀번호 재설정 통합 모달
 *
 * 정책 (04 §1-2, ADR-010):
 *  - 등록: email/password 입력. 비밀번호 정규식 검증.
 *  - 수정: email/password 잠금. 잠금해제/비밀번호재설정 액션 노출.
 * ============================================================ */

const AdminUserForm = (() => {
    let modalInstance = null;
    let passwordModalInstance = null;
    let mode = 'create';
    let rolesCache = null;

    function getModal() {
        if (!modalInstance) {
            modalInstance = new bootstrap.Modal(document.querySelector('#userModal'));
        }
        return modalInstance;
    }
    function getPasswordModal() {
        if (!passwordModalInstance) {
            passwordModalInstance = new bootstrap.Modal(document.querySelector('#passwordModal'));
        }
        return passwordModalInstance;
    }

    async function loadRoles() {
        if (rolesCache) return rolesCache;
        try {
            const res = await App.get('/api/roles?size=100');
            rolesCache = res.data.content ?? [];
            return rolesCache;
        } catch (e) {
            App.toast(e.message, 'error');
            return [];
        }
    }

    function fillRoleSelect(roles, selected) {
        const sel = document.querySelector('#roleSelect');
        sel.innerHTML = '<option value="">— 미부여 —</option>';
        for (const r of roles) {
            if (r.useYn !== 'Y') continue;
            const opt = document.createElement('option');
            opt.value = r.roleId;
            opt.textContent = `${r.roleName} (${r.roleCode})`;
            if (selected != null && String(selected) === String(r.roleId)) opt.selected = true;
            sel.appendChild(opt);
        }
    }

    async function openCreate() {
        mode = 'create';
        document.querySelector('#userModalTitle').textContent = '관리자 등록';
        const form = document.querySelector('#userForm');
        form.reset();
        form.elements.userId.value = '';
        form.elements.email.readOnly = false;
        form.elements.password.required = true;
        document.querySelector('#passwordRow').classList.remove('d-none');
        document.querySelector('#useYnRow').classList.add('d-none');
        document.querySelector('#adminActionsRow').classList.add('d-none');

        const roles = await loadRoles();
        fillRoleSelect(roles, null);
        getModal().show();
    }

    async function openUpdate(userId) {
        mode = 'update';
        try {
            const res = await App.get(`/api/users/${userId}`);
            const u = res.data;
            document.querySelector('#userModalTitle').textContent = `관리자 수정 — ${u.email}`;
            const form = document.querySelector('#userForm');
            form.reset();
            form.elements.userId.value   = u.userId;
            form.elements.email.value    = u.email;
            form.elements.email.readOnly = true;
            form.elements.userName.value = u.userName;
            form.elements.phone.value    = u.phone ?? '';
            form.elements.useYn.value    = u.useYn ?? 'Y';

            // 수정 모드: 비밀번호 입력 숨김, 사용여부/액션 노출
            document.querySelector('#passwordRow').classList.add('d-none');
            form.elements.password.required = false;
            document.querySelector('#useYnRow').classList.remove('d-none');
            document.querySelector('#adminActionsRow').classList.remove('d-none');

            // 잠금 상태 표시
            const lockEl = document.querySelector('#lockStatus');
            if (u.locked) {
                lockEl.innerHTML = `<span class="badge bg-red-lt">잠금 (실패 ${u.loginFailCnt}회)</span>`;
            } else {
                lockEl.innerHTML = `<span class="text-secondary">정상 (실패 ${u.loginFailCnt}회)</span>`;
            }

            const roles = await loadRoles();
            fillRoleSelect(roles, u.roleId);
            getModal().show();
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    async function save() {
        const form = document.querySelector('#userForm');
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }
        const data = formToObject(form);

        try {
            if (mode === 'create') {
                const payload = {
                    email:    data.email,
                    password: data.password,
                    userName: data.userName,
                    phone:    data.phone || null,
                    roleId:   data.roleId ? Number(data.roleId) : null,
                };
                const res = await App.post('/api/users', payload);
                App.toast(res.message || '등록되었습니다', 'success');
            } else {
                const payload = {
                    userName: data.userName,
                    phone:    data.phone || null,
                    roleId:   data.roleId ? Number(data.roleId) : null,
                    useYn:    data.useYn || 'Y',
                };
                const res = await App.put(`/api/users/${data.userId}`, payload);
                App.toast(res.message || '수정되었습니다', 'success');
            }
            getModal().hide();
            AdminUserList.load();
        } catch (e) {
            const detail = (e.errors && e.errors.length)
                ? e.errors.map(f => `${f.field}: ${f.reason}`).join(', ')
                : '';
            App.toast((e.message || '저장 실패') + (detail ? ` (${detail})` : ''), 'error');
        }
    }

    async function unlock() {
        const userId = document.querySelector('#userForm').elements.userId.value;
        if (!userId) return;
        try {
            const res = await App.put(`/api/users/${userId}/unlock`, {});
            App.toast(res.message || '잠금 해제되었습니다', 'success');
            document.querySelector('#lockStatus').innerHTML = '<span class="text-secondary">정상 (실패 0회)</span>';
            AdminUserList.load();
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    function openPasswordReset() {
        const userForm = document.querySelector('#userForm');
        const userId = userForm.elements.userId.value;
        const email  = userForm.elements.email.value;
        if (!userId) return;
        const pForm = document.querySelector('#passwordForm');
        pForm.reset();
        pForm.elements.userId.value = userId;
        document.querySelector('#passwordTargetEmail').textContent = email;
        getPasswordModal().show();
    }

    async function savePasswordReset() {
        const pForm = document.querySelector('#passwordForm');
        if (!pForm.checkValidity()) {
            pForm.reportValidity();
            return;
        }
        const userId = pForm.elements.userId.value;
        const newPassword = pForm.elements.newPassword.value;
        try {
            const res = await App.put(`/api/users/${userId}/password`, { newPassword });
            App.toast(res.message || '비밀번호가 재설정되었습니다', 'success');
            getPasswordModal().hide();
            // 모달 안의 잠금 상태도 정상으로 변경
            document.querySelector('#lockStatus').innerHTML = '<span class="text-secondary">정상 (실패 0회)</span>';
            AdminUserList.load();
        } catch (e) {
            const detail = (e.errors && e.errors.length)
                ? e.errors.map(f => `${f.field}: ${f.reason}`).join(', ')
                : '';
            App.toast((e.message || '재설정 실패') + (detail ? ` (${detail})` : ''), 'error');
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

    return { openCreate, openUpdate, save, unlock, openPasswordReset, savePasswordReset };
})();
