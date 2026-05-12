/* ============================================================
 * 연체 해제 모달 — 수동 해제 (reason 필수)
 * ============================================================ */

const OverdueForm = (() => {
    let modalInstance = null;

    function getModal() {
        if (!modalInstance) {
            modalInstance = new bootstrap.Modal(document.querySelector('#resolveModal'));
        }
        return modalInstance;
    }

    function openResolve(rowData) {
        const form = document.querySelector('#resolveForm');
        form.reset();
        form.elements.overdueId.value = rowData.overdueId;
        const target = `${rowData.billingNo ?? '-'} / ${rowData.customerName ?? '-'} (${rowData.overdueDays}일, ${Number(rowData.overdueAmount ?? 0).toLocaleString()}원)`;
        document.querySelector('#resolveTarget').textContent = target;
        getModal().show();
    }

    async function submitResolve() {
        const form = document.querySelector('#resolveForm');
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }
        const overdueId = form.elements.overdueId.value;
        const reason    = form.elements.reason.value;
        try {
            const res = await App.put(`/api/overdues/${overdueId}/resolve`, { reason });
            App.toast(res.message || '연체가 해제되었습니다', 'success');
            getModal().hide();
            OverdueList.load();
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    return { openResolve, submitResolve };
})();
