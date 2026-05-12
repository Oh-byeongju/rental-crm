/* ============================================================
 * 계약 등록 모달 — 신규 계약만.
 * 수정/상태 전이는 상세 페이지 (/admin/contracts/{id}).
 *
 * 자동 동작:
 *  - 고객 선택 → 설치 주소 자동 채움 (고객.address)
 *  - 상품 선택 → 월 렌탈료 미리보기 (실제 저장은 서버 스냅샷)
 * ============================================================ */

const ContractForm = (() => {
    let modalInstance = null;
    let customersCache = null;
    let productsCache = null;

    function getModal() {
        if (!modalInstance) {
            modalInstance = new bootstrap.Modal(document.querySelector('#contractModal'));
        }
        return modalInstance;
    }

    async function loadCustomers() {
        if (customersCache) return customersCache;
        try {
            const res = await App.get('/api/customers?size=500');
            customersCache = res.data?.content ?? [];
        } catch (e) {
            App.toast(e.message, 'error');
            customersCache = [];
        }
        return customersCache;
    }

    async function loadProducts() {
        if (productsCache) return productsCache;
        try {
            const res = await App.get('/api/products?size=500');
            productsCache = res.data?.content ?? [];
        } catch (e) {
            App.toast(e.message, 'error');
            productsCache = [];
        }
        return productsCache;
    }

    function fillCustomerSelect(customers) {
        const sel = document.querySelector('#formCustomerId');
        sel.innerHTML = '<option value="">— 선택 —</option>';
        for (const c of customers) {
            const opt = document.createElement('option');
            opt.value = c.customerId;
            opt.textContent = `${c.customerNo} - ${c.customerName}`;
            opt.dataset.address = c.address ?? '';
            sel.appendChild(opt);
        }
    }

    function fillProductSelect(products) {
        const sel = document.querySelector('#formProductId');
        sel.innerHTML = '<option value="">— 선택 —</option>';
        for (const p of products) {
            const opt = document.createElement('option');
            opt.value = p.productId;
            opt.textContent = `${p.productCode} - ${p.productName} (월 ${Number(p.monthlyFee).toLocaleString()}원)`;
            opt.dataset.monthlyFee = p.monthlyFee;
            sel.appendChild(opt);
        }
    }

    async function openCreate() {
        const form = document.querySelector('#contractForm');
        form.reset();
        document.querySelector('#previewMonthlyFee').value = '';

        const [customers, products] = await Promise.all([loadCustomers(), loadProducts()]);
        fillCustomerSelect(customers);
        fillProductSelect(products);

        // 고객 선택 시 주소 자동 입력 (D9)
        document.querySelector('#formCustomerId').onchange = (e) => {
            const opt = e.target.selectedOptions[0];
            form.elements.installAddress.value = opt?.dataset.address || '';
        };

        // 상품 선택 시 월 렌탈료 미리보기
        document.querySelector('#formProductId').onchange = (e) => {
            const opt = e.target.selectedOptions[0];
            const fee = opt?.dataset.monthlyFee;
            document.querySelector('#previewMonthlyFee').value = fee
                ? Number(fee).toLocaleString() + ' 원'
                : '';
        };

        getModal().show();
    }

    async function save() {
        const form = document.querySelector('#contractForm');
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }
        const data = formToObject(form);
        try {
            const payload = {
                customerId:     Number(data.customerId),
                productId:      Number(data.productId),
                startDate:      data.startDate,
                installAddress: data.installAddress,
            };
            const res = await App.post('/api/contracts', payload);
            App.toast(res.message || '등록되었습니다', 'success');
            getModal().hide();
            ContractList.load();
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

    return { openCreate, save };
})();
