/* ============================================================
 * 배치 실행 — ADR-014 Step 5 (통신 뼈대 검증)
 * ============================================================ */

const BatchTrigger = (() => {
    async function run(scenario) {
        if (!confirm(`'${scenario}' 시나리오를 실행하시겠습니까?`)) return;
        try {
            const res = await App.post(`/api/admin/batch-trigger/run/${scenario}`);
            App.toast(res.message || '요청 전송됨', 'success');
        } catch (e) {
            App.toast(e.message || '실행 실패', 'error');
        }
    }
    return { run };
})();
