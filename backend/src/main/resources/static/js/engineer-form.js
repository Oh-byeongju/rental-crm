/* ============================================================
 * 기사 등록/수정 모달
 *
 * 정책:
 *  - 등록: engineerCode 입력 / useYn 행 숨김 (기본 Y)
 *  - 수정: engineerCode 변경 가능 (UNIQUE 사전 검증) / useYn 행 노출
 *  - ENGINEER_TYPE: INTERNAL/EXTERNAL 정적 옵션 (CM_CODE 미사용)
 * ============================================================ */

const EngineerForm = (() => {
    let modalInstance = null;
    let mode = 'create';

    function getModal() {
        if (!modalInstance) {
            modalInstance = new bootstrap.Modal(document.querySelector('#engineerModal'));
        }
        return modalInstance;
    }

    function openCreate() {
        mode = 'create';
        document.querySelector('#engineerModalTitle').textContent = '기사 등록';
        const form = document.querySelector('#engineerForm');
        form.reset();
        form.elements.engineerId.value = '';
        form.elements.engineerCode.readOnly = false;
        document.querySelector('#useYnRow').classList.add('d-none');
        getModal().show();
    }

    async function openUpdate(engineerId) {
        mode = 'update';
        try {
            const res = await App.get(`/api/engineers/${engineerId}`);
            const e = res.data;
            document.querySelector('#engineerModalTitle').textContent = `기사 수정 — ${e.engineerCode}`;
            const form = document.querySelector('#engineerForm');
            form.reset();
            form.elements.engineerId.value     = e.engineerId;
            form.elements.engineerCode.value   = e.engineerCode;
            form.elements.engineerCode.readOnly = false;
            form.elements.engineerName.value   = e.engineerName;
            form.elements.engineerType.value   = e.engineerType;
            form.elements.phone.value          = e.phone;
            form.elements.email.value          = e.email ?? '';
            form.elements.area.value           = e.area ?? '';
            form.elements.useYn.value          = e.useYn ?? 'Y';

            document.querySelector('#useYnRow').classList.remove('d-none');
            getModal().show();
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    async function save() {
        const form = document.querySelector('#engineerForm');
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }
        const data = formToObject(form);

        try {
            if (mode === 'create') {
                const payload = {
                    engineerCode: data.engineerCode,
                    engineerName: data.engineerName,
                    engineerType: data.engineerType,
                    phone:        data.phone,
                    email:        data.email || null,
                    area:         data.area  || null,
                };
                const res = await App.post('/api/engineers', payload);
                App.toast(res.message || '등록되었습니다', 'success');
            } else {
                const payload = {
                    engineerCode: data.engineerCode,
                    engineerName: data.engineerName,
                    engineerType: data.engineerType,
                    phone:        data.phone,
                    email:        data.email || null,
                    area:         data.area  || null,
                    useYn:        data.useYn || 'Y',
                };
                const res = await App.put(`/api/engineers/${data.engineerId}`, payload);
                App.toast(res.message || '수정되었습니다', 'success');
            }
            getModal().hide();
            EngineerList.load();
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
        for (const [k, v] of data.entries()) obj[k] = v === '' ? null : v;
        return obj;
    }

    return { openCreate, openUpdate, save };
})();
