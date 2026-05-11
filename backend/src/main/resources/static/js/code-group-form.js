/* ============================================================
 * 공통코드 그룹 — 등록/수정 모달
 * 04 §1-1 + 07 §2-1 매핑
 *
 * 정책:
 *  - 등록: groupCode 입력 (영문 대문자/숫자/언더스코어). 사용여부 노출 안 함 (기본 'Y').
 *  - 수정: groupCode readonly. groupName/description/useYn 변경 가능.
 * ============================================================ */

const CodeGroupForm = (() => {
    let modalInstance = null;
    let mode = 'create';

    function getModal() {
        if (!modalInstance) {
            modalInstance = new bootstrap.Modal(document.querySelector('#codeGroupModal'));
        }
        return modalInstance;
    }

    function openCreate() {
        mode = 'create';
        document.querySelector('#codeGroupModalTitle').textContent = '그룹 등록';
        const form = document.querySelector('#codeGroupForm');
        form.reset();
        form.elements.groupCode.readOnly = false;
        // 등록 모드: useYn 숨김 (기본 Y), 수정 모드에서 활성화
        document.querySelector('#useYnRow').classList.add('d-none');
        getModal().show();
    }

    async function openUpdate(groupCode) {
        mode = 'update';
        try {
            const res = await App.get(`/api/code-groups/${groupCode}`);
            const g = res.data;
            document.querySelector('#codeGroupModalTitle').textContent = `그룹 수정 — ${g.groupCode}`;
            const form = document.querySelector('#codeGroupForm');
            form.reset();
            form.elements.groupCode.value   = g.groupCode;
            form.elements.groupCode.readOnly = true;
            form.elements.groupName.value   = g.groupName;
            form.elements.description.value = g.description ?? '';
            form.elements.useYn.value       = g.useYn ?? 'Y';
            // 수정 모드: useYn 노출
            document.querySelector('#useYnRow').classList.remove('d-none');
            getModal().show();
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    async function save() {
        const form = document.querySelector('#codeGroupForm');
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }
        const data = formToObject(form);

        try {
            if (mode === 'create') {
                const payload = {
                    groupCode:   data.groupCode,
                    groupName:   data.groupName,
                    description: data.description || null,
                };
                const res = await App.post('/api/code-groups', payload);
                App.toast(res.message || '등록되었습니다', 'success');
            } else {
                const payload = {
                    groupName:   data.groupName,
                    description: data.description || null,
                    useYn:       data.useYn || 'Y',
                };
                const res = await App.put(`/api/code-groups/${data.groupCode}`, payload);
                App.toast(res.message || '수정되었습니다', 'success');
            }
            getModal().hide();
            CodeGroupList.load();
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
