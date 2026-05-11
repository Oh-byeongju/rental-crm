/* ============================================================
 * 공통코드 (값) — 등록/수정 모달
 *
 * 정책:
 *  - 등록: codeValue 입력. groupCode 는 hidden (페이지의 그룹).
 *  - 수정: codeValue readonly. codeName/sortOrder/useYn 변경.
 * ============================================================ */

const CodeForm = (() => {
    let modalInstance = null;
    let mode = 'create';

    function getModal() {
        if (!modalInstance) {
            modalInstance = new bootstrap.Modal(document.querySelector('#codeModal'));
        }
        return modalInstance;
    }

    function openCreate() {
        mode = 'create';
        document.querySelector('#codeModalTitle').textContent = '코드 등록';
        const form = document.querySelector('#codeForm');
        form.reset();
        form.elements.groupCode.value = CodeList.getGroupCode();
        form.elements.codeId.value    = '';
        document.querySelector('#codeValueRow input').readOnly = false;
        form.elements.useYn.value = 'Y';
        form.elements.sortOrder.value = '0';
        getModal().show();
    }

    async function openUpdate(codeId) {
        mode = 'update';
        try {
            const res = await App.get(`/api/codes/${codeId}`);
            const c = res.data;
            document.querySelector('#codeModalTitle').textContent = `코드 수정 — ${c.codeValue}`;
            const form = document.querySelector('#codeForm');
            form.reset();
            form.elements.codeId.value    = c.codeId;
            form.elements.groupCode.value = c.groupCode;
            form.elements.codeValue.value = c.codeValue;
            form.elements.codeName.value  = c.codeName;
            form.elements.sortOrder.value = c.sortOrder;
            form.elements.useYn.value     = c.useYn ?? 'Y';
            // 수정 모드: codeValue 변경 불가
            document.querySelector('#codeValueRow input').readOnly = true;
            getModal().show();
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    async function save() {
        const form = document.querySelector('#codeForm');
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }
        const data = formToObject(form);

        try {
            if (mode === 'create') {
                const payload = {
                    groupCode: data.groupCode,
                    codeValue: data.codeValue,
                    codeName:  data.codeName,
                    sortOrder: data.sortOrder ? Number(data.sortOrder) : 0,
                };
                const res = await App.post('/api/codes', payload);
                App.toast(res.message || '등록되었습니다', 'success');
            } else {
                const payload = {
                    codeName:  data.codeName,
                    sortOrder: data.sortOrder ? Number(data.sortOrder) : 0,
                    useYn:     data.useYn || 'Y',
                };
                const res = await App.put(`/api/codes/${data.codeId}`, payload);
                App.toast(res.message || '수정되었습니다', 'success');
            }
            getModal().hide();
            CodeList.load();
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
