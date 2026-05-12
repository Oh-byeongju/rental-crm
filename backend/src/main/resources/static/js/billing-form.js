/* ============================================================
 * 청구 상세 모달 — read-only 정보 + 액션 (납기 변경 / 청구 취소).
 * 단건 등록 X (배치 일괄 생성). 상태 전이 PAID 는 Ch.3 수납 도메인 트리거.
 * ============================================================ */

const BillingForm = (() => {
    let modalInstance = null;

    const statusLabel = (status) => {
        if (status === 'UNPAID')    return '미납';
        if (status === 'OVERDUE')   return '연체';
        if (status === 'PAID')      return '수납완료';
        if (status === 'CANCELLED') return '취소';
        return status;
    };

    function getModal() {
        if (!modalInstance) {
            modalInstance = new bootstrap.Modal(document.querySelector('#billingModal'));
        }
        return modalInstance;
    }

    async function openDetail(billingId) {
        try {
            const res = await App.get(`/api/billings/${billingId}`);
            const b = res.data;
            document.querySelector('#billingModalTitle').textContent = `청구 상세 — ${b.billingNo}`;
            document.querySelector('#modalBillingId').value     = b.billingId;
            document.querySelector('#modalBillingStatus').value = b.billingStatus;
            document.querySelector('#modalBillingNo').textContent     = b.billingNo;
            document.querySelector('#modalStatus').textContent        = statusLabel(b.billingStatus);
            document.querySelector('#modalContractNo').textContent    = b.contractNo ?? '-';
            document.querySelector('#modalCustomer').textContent      = `${b.customerName ?? '-'} (${b.customerNo ?? '-'})`;
            document.querySelector('#modalBillingMonth').textContent  = b.billingMonth ?? '-';
            document.querySelector('#modalAmount').textContent        = Number(b.billingAmount ?? 0).toLocaleString() + ' 원';
            document.querySelector('#modalIssueDate').textContent     = b.issueDate ?? '-';
            document.querySelector('#modalDueDate').value             = b.dueDate ?? '';
            document.querySelector('#modalPaidAt').textContent        = b.paidAt ? b.paidAt.replace('T', ' ').substring(0, 16) : '-';

            // 액션 버튼 활성 분기
            const editable = (b.billingStatus !== 'PAID' && b.billingStatus !== 'CANCELLED');
            const cancellable = (b.billingStatus === 'UNPAID' || b.billingStatus === 'OVERDUE');
            document.querySelector('#modalDueDate').disabled    = !editable;
            document.querySelector('#btnUpdateDue').disabled    = !editable;
            document.querySelector('#btnCancel').disabled       = !cancellable;

            getModal().show();
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    async function updateDueDate() {
        const billingId = document.querySelector('#modalBillingId').value;
        const dueDate   = document.querySelector('#modalDueDate').value;
        if (!dueDate) {
            App.toast('납기일을 선택하세요', 'warning');
            return;
        }
        try {
            const res = await App.put(`/api/billings/${billingId}/due-date?dueDate=${dueDate}`, {});
            App.toast(res.message || '납기일이 변경되었습니다', 'success');
            getModal().hide();
            BillingList.load();
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    async function cancel() {
        if (!confirm('이 청구를 취소하시겠습니까? (CANCELLED 상태로 전환)')) return;
        const billingId = document.querySelector('#modalBillingId').value;
        try {
            const res = await App.put(`/api/billings/${billingId}/cancel`, {});
            App.toast(res.message || '청구가 취소되었습니다', 'success');
            getModal().hide();
            BillingList.load();
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    return { openDetail, updateDueDate, cancel };
})();
