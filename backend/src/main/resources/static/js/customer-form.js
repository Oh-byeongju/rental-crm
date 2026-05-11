/* ============================================================
 * 고객 관리 — 등록/수정 모달
 * 05 §2-7 + 07 §5 매핑
 *
 * 사용여부(useYn) 정책:
 *  - 등록: 기본 'Y' (활성). select 노출.
 *  - 수정: 현재 값 표시. 'Y' / 'N' 변경 가능 → 저장 시점 반영.
 *  - 별도 [비활성화] 버튼 없음. select + 저장으로 통합.
 * ============================================================ */

const CustomerForm = (() => {
    let modalInstance = null;
    let mode = 'create';   // 'create' | 'update'

    function getModal() {
        if (!modalInstance) {
            modalInstance = new bootstrap.Modal(document.querySelector('#customerModal'));
        }
        return modalInstance;
    }

    function openCreate() {
        mode = 'create';
        document.querySelector('#customerModalTitle').textContent = '고객 등록';
        const form = document.querySelector('#customerForm');
        form.reset();
        // 등록 모드: customerNo 숨김 / email·password 활성 / 사용여부 기본 'Y'
        document.querySelector('#customerNoRow').classList.add('d-none');
        document.querySelector('#emailRow').classList.remove('d-none');
        document.querySelector('#passwordRow').classList.remove('d-none');
        document.querySelector('#passwordRow input').required = true;
        document.querySelector('#emailRow input').readOnly = false;
        form.elements.useYn.value = 'Y';
        getModal().show();
    }

    async function openUpdate(customerId) {
        mode = 'update';
        try {
            const res = await App.get(`/api/customers/${customerId}`);
            const c = res.data;
            document.querySelector('#customerModalTitle').textContent = `고객 수정 — ${c.customerNo}`;
            const form = document.querySelector('#customerForm');
            form.reset();
            form.elements.customerId.value   = c.customerId;
            form.elements.customerNo.value   = c.customerNo;
            form.elements.customerName.value = c.customerName;
            form.elements.email.value        = c.email;
            form.elements.phone.value        = c.phone;
            form.elements.birthDate.value    = c.birthDate ?? '';
            form.elements.addressZip.value   = c.addressZip ?? '';
            form.elements.address.value      = c.address;
            form.elements.useYn.value        = c.useYn ?? 'Y';
            form.elements.wrkRmk.value       = c.wrkRmk ?? '';
            form.elements.termsAgreeYn.checked = (c.termsAgreeYn === 'Y');

            // 수정 모드: customerNo 표시, email/password readonly/숨김
            document.querySelector('#customerNoRow').classList.remove('d-none');
            document.querySelector('#emailRow input').readOnly = true;
            document.querySelector('#passwordRow').classList.add('d-none');
            document.querySelector('#passwordRow input').required = false;
            getModal().show();
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    async function save() {
        const form = document.querySelector('#customerForm');
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }
        const data = formToObject(form);
        // 체크박스 termsAgreeYn 처리
        data.termsAgreeYn = form.elements.termsAgreeYn.checked ? 'Y' : 'N';

        try {
            if (mode === 'create') {
                const res = await App.post('/api/customers', data);
                App.toast(res.message || '등록되었습니다', 'success');
            } else {
                const id = data.customerId;
                // 수정 요청 payload — 허용된 필드만 (이메일/비밀번호 제외)
                const updatePayload = {
                    customerName: data.customerName,
                    phone:        data.phone,
                    birthDate:    data.birthDate || null,
                    addressZip:   data.addressZip || null,
                    address:      data.address,
                    useYn:        data.useYn || 'Y',
                    wrkRmk:       data.wrkRmk || null,
                };
                const res = await App.put(`/api/customers/${id}`, updatePayload);
                App.toast(res.message || '수정되었습니다', 'success');
            }
            getModal().hide();
            CustomerList.load();
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
