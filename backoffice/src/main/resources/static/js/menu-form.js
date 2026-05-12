/* ============================================================
 * 메뉴 관리 — 등록/수정 모달
 *
 * 정책 (학습 단순화):
 *  - 등록: parentMenuId / menuType 자유 선택
 *  - 수정: parentMenuId / menuType 변경 불가 (트리 정합성)
 * ============================================================ */

const MenuForm = (() => {
    let modalInstance = null;
    let mode = 'create';
    let groupsCache = null;

    function getModal() {
        if (!modalInstance) {
            modalInstance = new bootstrap.Modal(document.querySelector('#menuModal'));
        }
        return modalInstance;
    }

    async function loadGroups() {
        if (groupsCache) return groupsCache;
        try {
            const res = await App.get('/api/menus/groups');
            groupsCache = res.data ?? [];
            return groupsCache;
        } catch (e) {
            App.toast(e.message, 'error');
            return [];
        }
    }

    function fillParentSelect(groups, selected) {
        const sel = document.querySelector('#parentSelect');
        sel.innerHTML = '<option value="">— 루트 (depth 1) —</option>';
        for (const g of groups) {
            const opt = document.createElement('option');
            opt.value = g.menuId;
            opt.textContent = `[${g.menuId}] ${g.menuName}`;
            if (selected != null && String(selected) === String(g.menuId)) opt.selected = true;
            sel.appendChild(opt);
        }
    }

    async function openCreate() {
        mode = 'create';
        document.querySelector('#menuModalTitle').textContent = '메뉴 등록';
        const form = document.querySelector('#menuForm');
        form.reset();
        form.elements.menuId.value = '';
        form.elements.useYn.value = 'Y';
        form.elements.sortOrder.value = '0';

        const groups = await loadGroups();
        fillParentSelect(groups, null);

        // 등록 모드: parent/type 활성, useYn 숨김
        form.elements.parentMenuId.disabled = false;
        form.elements.menuType.disabled = false;
        document.querySelector('#useYnRow').classList.add('d-none');

        getModal().show();
    }

    async function openUpdate(menuId) {
        mode = 'update';
        try {
            const res = await App.get(`/api/menus/${menuId}`);
            const m = res.data;
            document.querySelector('#menuModalTitle').textContent = `메뉴 수정 — ${m.menuName}`;
            const form = document.querySelector('#menuForm');
            form.reset();
            form.elements.menuId.value    = m.menuId;
            form.elements.menuName.value  = m.menuName;
            form.elements.menuType.value  = m.menuType;
            form.elements.menuUrl.value   = m.menuUrl ?? '';
            form.elements.iconClass.value = m.iconClass ?? '';
            form.elements.sortOrder.value = m.sortOrder;
            form.elements.useYn.value     = m.useYn ?? 'Y';

            const groups = await loadGroups();
            fillParentSelect(groups, m.parentMenuId);

            // 수정 모드: parent/type 잠금, useYn 노출
            form.elements.parentMenuId.disabled = true;
            form.elements.menuType.disabled = true;
            document.querySelector('#useYnRow').classList.remove('d-none');

            getModal().show();
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    async function save() {
        const form = document.querySelector('#menuForm');
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }
        const data = formToObject(form);

        try {
            if (mode === 'create') {
                const payload = {
                    parentMenuId: data.parentMenuId ? Number(data.parentMenuId) : null,
                    menuName:     data.menuName,
                    menuType:     data.menuType,
                    menuUrl:      data.menuType === 'GROUP' ? null : (data.menuUrl || null),
                    iconClass:    data.iconClass || null,
                    sortOrder:    data.sortOrder ? Number(data.sortOrder) : 0,
                };
                const res = await App.post('/api/menus', payload);
                App.toast(res.message || '등록되었습니다', 'success');
            } else {
                const payload = {
                    menuName:  data.menuName,
                    menuUrl:   data.menuUrl || null,
                    iconClass: data.iconClass || null,
                    sortOrder: data.sortOrder ? Number(data.sortOrder) : 0,
                    useYn:     data.useYn || 'Y',
                };
                const res = await App.put(`/api/menus/${data.menuId}`, payload);
                App.toast(res.message || '수정되었습니다', 'success');
            }
            // 새 GROUP 이 추가됐을 수 있으니 캐시 무효화
            groupsCache = null;
            getModal().hide();
            MenuList.load();
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
