/* ============================================================
 * 계약 상세 페이지 — 수정 + 상태 전이 액션 (일시정지/재개/해지).
 * 각 액션은 별도 API 호출. 사유 입력은 공용 모달.
 * ============================================================ */

const ContractDetail = (() => {
    let editModalInstance = null;
    let reasonModalInstance = null;
    let pendingAction = null;   // 'suspend' or 'terminate' (재개는 confirm 후 즉시 호출)

    function contractId() {
        return document.querySelector('#contractId').value;
    }

    function getEditModal() {
        if (!editModalInstance) {
            editModalInstance = new bootstrap.Modal(document.querySelector('#editModal'));
        }
        return editModalInstance;
    }

    function getReasonModal() {
        if (!reasonModalInstance) {
            reasonModalInstance = new bootstrap.Modal(document.querySelector('#reasonModal'));
        }
        return reasonModalInstance;
    }

    function openEdit() {
        getEditModal().show();
    }

    async function save() {
        const form = document.querySelector('#editForm');
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }
        const data = formToObject(form);
        try {
            const payload = {
                endDate:        data.endDate,
                installAddress: data.installAddress,
            };
            const res = await App.put(`/api/contracts/${contractId()}`, payload);
            App.toast(res.message || '수정되었습니다', 'success');
            getEditModal().hide();
            location.reload();
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    function suspend() {
        pendingAction = 'suspend';
        document.querySelector('#reasonModalTitle').textContent = '일시정지 사유';
        document.querySelector('#reasonForm').reset();
        getReasonModal().show();
    }

    async function resume() {
        if (!confirm('계약을 재개하시겠습니까?')) return;
        try {
            const res = await App.put(`/api/contracts/${contractId()}/resume`, {});
            App.toast(res.message || '재개되었습니다', 'success');
            location.reload();
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    function terminate() {
        pendingAction = 'terminate';
        document.querySelector('#reasonModalTitle').textContent = '해지 사유';
        document.querySelector('#reasonForm').reset();
        getReasonModal().show();
    }

    async function submitReason() {
        const form = document.querySelector('#reasonForm');
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }
        const reason = form.elements.reason.value;
        const url = `/api/contracts/${contractId()}/${pendingAction}`;
        try {
            const res = await App.put(url, { reason });
            App.toast(res.message || '처리되었습니다', 'success');
            getReasonModal().hide();
            location.reload();
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

    return { openEdit, save, suspend, resume, terminate, submitReason };
})();
