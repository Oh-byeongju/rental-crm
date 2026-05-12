/* ============================================================
 * 방문 배정/수정 모달 + 완료/취소 액션.
 *
 * 정책:
 *  - 등록: contractId/engineerId/visitType/scheduledDate/memo. 모든 select 옵션 fetch.
 *  - 수정: SCHEDULED 상태만. visitType / contractId 변경 X.
 *  - 완료: confirm 후 PUT /complete
 *  - 취소: 사유 모달 → PUT /cancel
 * ============================================================ */

const VisitForm = (() => {
    let modalInstance = null;
    let cancelModalInstance = null;
    let mode = 'create';
    let contractsCache = null;
    let engineersCache = null;
    let typesCache = null;

    function getModal() {
        if (!modalInstance) modalInstance = new bootstrap.Modal(document.querySelector('#visitModal'));
        return modalInstance;
    }
    function getCancelModal() {
        if (!cancelModalInstance) cancelModalInstance = new bootstrap.Modal(document.querySelector('#cancelModal'));
        return cancelModalInstance;
    }

    async function loadOptions() {
        if (contractsCache && engineersCache && typesCache) return;
        try {
            const [contracts, engineers, types] = await Promise.all([
                App.get('/api/contracts?size=500&contractStatus=ACTIVE'),
                App.get('/api/engineers?size=500&useYn=Y'),
                App.get('/api/code-groups/VISIT_TYPE/options'),
            ]);
            contractsCache = contracts.data?.content ?? [];
            engineersCache = engineers.data?.content ?? [];
            typesCache     = types.data ?? [];
        } catch (e) {
            App.toast(e.message, 'error');
            contractsCache = contractsCache ?? [];
            engineersCache = engineersCache ?? [];
            typesCache     = typesCache     ?? [];
        }
    }

    function fillSelect(selector, items, valueKey, labelFn, selected) {
        const sel = document.querySelector(selector);
        sel.innerHTML = '<option value="">— 선택 —</option>';
        for (const it of items) {
            const opt = document.createElement('option');
            opt.value = it[valueKey];
            opt.textContent = labelFn(it);
            if (selected != null && String(selected) === String(it[valueKey])) opt.selected = true;
            sel.appendChild(opt);
        }
    }

    async function openCreate() {
        mode = 'create';
        document.querySelector('#visitModalTitle').textContent = '방문 배정';
        const form = document.querySelector('#visitForm');
        form.reset();
        form.elements.visitId.value = '';
        document.querySelector('#contractRow').classList.remove('d-none');
        document.querySelector('#visitTypeRow').classList.remove('d-none');

        await loadOptions();
        fillSelect('#formContractId', contractsCache, 'contractId',
            (c) => `${c.contractNo} - ${c.customerName ?? ''}`, null);
        fillSelect('#formEngineerId', engineersCache, 'engineerId',
            (e) => `${e.engineerName} (${e.engineerCode}${e.area ? ' / ' + e.area : ''})`, null);
        fillSelect('#formVisitType', typesCache, 'codeValue',
            (t) => t.codeName, null);

        getModal().show();
    }

    async function openUpdate(visitId) {
        mode = 'update';
        try {
            const res = await App.get(`/api/visits/${visitId}`);
            const v = res.data;
            document.querySelector('#visitModalTitle').textContent = `방문 수정 — #${v.visitId}`;
            const form = document.querySelector('#visitForm');
            form.reset();
            form.elements.visitId.value       = v.visitId;
            form.elements.scheduledDate.value = v.scheduledDate ?? '';
            form.elements.memo.value          = v.memo ?? '';

            await loadOptions();
            // 수정 시 contract / visitType 은 readOnly 표시 (변경 X)
            document.querySelector('#contractRow').classList.add('d-none');
            document.querySelector('#visitTypeRow').classList.add('d-none');
            fillSelect('#formEngineerId', engineersCache, 'engineerId',
                (e) => `${e.engineerName} (${e.engineerCode}${e.area ? ' / ' + e.area : ''})`, v.engineerId);

            getModal().show();
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    async function save() {
        const form = document.querySelector('#visitForm');
        if (!form.checkValidity()) { form.reportValidity(); return; }
        const data = formToObject(form);
        try {
            if (mode === 'create') {
                const payload = {
                    contractId:    Number(data.contractId),
                    engineerId:    Number(data.engineerId),
                    visitType:     data.visitType,
                    scheduledDate: data.scheduledDate,
                    memo:          data.memo || null,
                };
                const res = await App.post('/api/visits', payload);
                App.toast(res.message || '배정되었습니다', 'success');
            } else {
                const payload = {
                    engineerId:    Number(data.engineerId),
                    scheduledDate: data.scheduledDate,
                    memo:          data.memo || null,
                };
                const res = await App.put(`/api/visits/${data.visitId}`, payload);
                App.toast(res.message || '수정되었습니다', 'success');
            }
            getModal().hide();
            VisitList.load();
        } catch (e) {
            const detail = (e.errors && e.errors.length)
                ? e.errors.map(f => `${f.field}: ${f.reason}`).join(', ')
                : '';
            App.toast((e.message || '저장 실패') + (detail ? ` (${detail})` : ''), 'error');
        }
    }

    async function complete(visitId) {
        if (!confirm('이 방문을 완료 처리하시겠습니까?')) return;
        try {
            const res = await App.put(`/api/visits/${visitId}/complete`, {});
            App.toast(res.message || '완료 처리되었습니다', 'success');
            VisitList.load();
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    function openCancel(visitId) {
        const form = document.querySelector('#cancelForm');
        form.reset();
        form.elements.visitId.value = visitId;
        getCancelModal().show();
    }

    async function submitCancel() {
        const form = document.querySelector('#cancelForm');
        if (!form.checkValidity()) { form.reportValidity(); return; }
        const visitId = form.elements.visitId.value;
        const reason  = form.elements.reason.value;
        try {
            const res = await App.put(`/api/visits/${visitId}/cancel`, { reason });
            App.toast(res.message || '취소되었습니다', 'success');
            getCancelModal().hide();
            VisitList.load();
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    function formToObject(form) {
        const data = new FormData(form);
        const obj = {};
        for (const [k, v] of data.entries()) obj[k] = v === '' ? null : v;
        return obj;
    }

    return { openCreate, openUpdate, save, complete, openCancel, submitCancel };
})();
