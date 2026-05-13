/* ============================================================
 * 배치 실행 — ADR-014 Step 5/7 (통신 + Ch.1 청구 배치)
 * ============================================================ */

const BatchTrigger = (() => {

    async function runDummy(scenario) {
        if (!confirm(`'${scenario}' 시나리오를 실행하시겠습니까?`)) return;
        try {
            const res = await App.post(`/api/admin/batch-trigger/run/${scenario}`, {});
            App.toast(res.message || '요청 전송됨', 'success');
        } catch (e) {
            App.toast(e.message || '실행 실패', 'error');
        }
    }

    async function runBillingCreate() {
        const billingMonth = document.getElementById('billingMonth').value.trim();
        const roundNo = parseInt(document.getElementById('roundNo').value, 10);

        if (!/^\d{4}-(0[1-9]|1[0-2])$/.test(billingMonth)) {
            App.toast('청구월 형식 오류 (YYYY-MM)', 'error');
            return;
        }
        const strategy = roundNo === 1 ? 'single-save'
                       : roundNo === 2 ? 'chunk-flush'
                       : roundNo === 3 ? 'bulk-jdbc'
                       : null;
        if (!strategy) { App.toast('해당 라운드는 Step 7-C 에서 지원', 'error'); return; }

        if (!confirm(`BILLING_CREATE 를 실행합니다.\n월=${billingMonth}, R${roundNo} (${strategy})\n5만건 INSERT — 수~수십초 소요.`)) return;

        try {
            const res = await App.post('/api/admin/batch-trigger/run/BILLING_CREATE',
                                       { billingMonth, roundNo, strategy });
            App.toast(res.message || '실행 요청됨', 'success');
            const modal = bootstrap.Modal.getInstance(document.getElementById('billingCreateModal'));
            if (modal) modal.hide();
        } catch (e) {
            App.toast(e.message || '실행 실패', 'error');
        }
    }

    return { runDummy, runBillingCreate };
})();
