/* ============================================================
 * 수납 등록 모달 — 수동 등록.
 * 청구 select 는 UNPAID / OVERDUE 만 (서버에서 필터링 — billingStatus).
 * billingId 선택 시 paymentAmount 자동 채움 (분할 X — 청구금액 그대로).
 * ============================================================ */

const PaymentForm = (() => {
    let modalInstance = null;
    let billingCache = new Map();   // billingId → { amount, billingNo }

    function getModal() {
        if (!modalInstance) {
            modalInstance = new bootstrap.Modal(document.querySelector('#registerModal'));
        }
        return modalInstance;
    }

    async function openRegister() {
        const form = document.querySelector('#registerForm');
        form.reset();
        form.elements.paymentDate.value = new Date().toISOString().slice(0, 10);
        await loadPayableBillings();
        getModal().show();
    }

    /** UNPAID / OVERDUE 청구 합집합 로딩 — 2번 호출 후 결합. */
    async function loadPayableBillings() {
        const sel = document.querySelector('#registerBillingId');
        sel.innerHTML = '<option value="">— 수납 가능 청구 선택 —</option>';
        billingCache.clear();

        try {
            const [unpaid, overdue] = await Promise.all([
                App.get('/api/billings?billingStatus=UNPAID&size=500'),
                App.get('/api/billings?billingStatus=OVERDUE&size=500'),
            ]);
            const rows = [
                ...(unpaid.data?.content  ?? []),
                ...(overdue.data?.content ?? []),
            ];
            rows.sort((a, b) => (b.billingId ?? 0) - (a.billingId ?? 0));
            for (const b of rows) {
                const label = `${b.billingNo} | ${b.billingMonth} | ${b.customerName ?? '-'} | ${Number(b.billingAmount ?? 0).toLocaleString()}원 | ${b.billingStatus}`;
                const opt = document.createElement('option');
                opt.value = b.billingId;
                opt.textContent = label;
                sel.appendChild(opt);
                billingCache.set(String(b.billingId), { amount: b.billingAmount, billingNo: b.billingNo });
            }
            // 청구 변경 시 금액 자동 채움
            sel.onchange = () => {
                const info = billingCache.get(sel.value);
                if (info) document.querySelector('#registerForm').elements.paymentAmount.value = info.amount ?? '';
            };
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    async function submitRegister() {
        const form = document.querySelector('#registerForm');
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }
        const data = formToObject(form);
        const payload = {
            billingId:      Number(data.billingId),
            paymentAmount:  Number(data.paymentAmount),
            paymentMethod:  data.paymentMethod,
            paymentDate:    data.paymentDate,
            tossOrderId:    data.tossOrderId    || null,
            tossPaymentKey: data.tossPaymentKey || null,
        };
        try {
            const res = await App.post('/api/payments', payload);
            App.toast(res.message || '수납이 등록되었습니다', 'success');
            getModal().hide();
            PaymentList.load();
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

    return { openRegister, submitRegister };
})();
