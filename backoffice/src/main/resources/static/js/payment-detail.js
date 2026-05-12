/* ============================================================
 * 수납 상세 — 취소 / 환불 액션.
 * 사유 모달은 공용. action 변수로 분기.
 * ============================================================ */

const PaymentDetail = (() => {
    let reasonModalInstance = null;
    let pendingAction = null;   // 'cancel' or 'refund'

    function paymentId() {
        return document.querySelector('#paymentId').value;
    }

    function getReasonModal() {
        if (!reasonModalInstance) {
            reasonModalInstance = new bootstrap.Modal(document.querySelector('#reasonModal'));
        }
        return reasonModalInstance;
    }

    function cancel() {
        pendingAction = 'cancel';
        document.querySelector('#reasonModalTitle').textContent = '수납 취소 사유';
        document.querySelector('#reasonForm').reset();
        getReasonModal().show();
    }

    function refund() {
        pendingAction = 'refund';
        document.querySelector('#reasonModalTitle').textContent = '환불 사유';
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
        const url = `/api/payments/${paymentId()}/${pendingAction}`;
        try {
            const res = await App.put(url, { reason });
            App.toast(res.message || '처리되었습니다', 'success');
            getReasonModal().hide();
            location.reload();
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    return { cancel, refund, submitReason };
})();
