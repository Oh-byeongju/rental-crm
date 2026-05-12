/* ============================================================
 * 상품 등록/수정 모달
 *
 * 정책:
 *  - 등록: productCode 입력 / contractMonths default 12 / useYn 행 숨김
 *  - 수정: productCode 변경 가능 / useYn 행 노출
 *  - 장비 selectbox: 전체 장비 (USE_YN 제약 없음 — 단종 장비도 선택 가능)
 * ============================================================ */

const ProductForm = (() => {
    let modalInstance = null;
    let mode = 'create';
    let equipmentsCache = null;

    function getModal() {
        if (!modalInstance) {
            modalInstance = new bootstrap.Modal(document.querySelector('#productModal'));
        }
        return modalInstance;
    }

    async function loadEquipments() {
        if (equipmentsCache) return equipmentsCache;
        try {
            const res = await App.get('/api/equipments?size=500');
            equipmentsCache = res.data?.content ?? [];
            return equipmentsCache;
        } catch (e) {
            App.toast(e.message, 'error');
            return [];
        }
    }

    function fillEquipmentSelect(equipments, selected) {
        const sel = document.querySelector('#formEquipmentId');
        sel.innerHTML = '<option value="">— 선택 —</option>';
        for (const e of equipments) {
            const opt = document.createElement('option');
            opt.value = e.equipmentId;
            const stockLabel = (e.stockQty != null && e.stockQty === 0) ? ' [재고 0]' : '';
            opt.textContent = `${e.equipmentCode} - ${e.modelName} (${e.manufacturer})${stockLabel}`;
            if (selected != null && String(selected) === String(e.equipmentId)) opt.selected = true;
            sel.appendChild(opt);
        }
    }

    async function openCreate() {
        mode = 'create';
        document.querySelector('#productModalTitle').textContent = '상품 등록';
        const form = document.querySelector('#productForm');
        form.reset();
        form.elements.productId.value = '';
        form.elements.productCode.readOnly = false;
        document.querySelector('#useYnRow').classList.add('d-none');

        const equipments = await loadEquipments();
        fillEquipmentSelect(equipments, null);
        getModal().show();
    }

    async function openUpdate(productId) {
        mode = 'update';
        try {
            const res = await App.get(`/api/products/${productId}`);
            const p = res.data;
            document.querySelector('#productModalTitle').textContent = `상품 수정 — ${p.productCode}`;
            const form = document.querySelector('#productForm');
            form.reset();
            form.elements.productId.value       = p.productId;
            form.elements.productCode.value     = p.productCode;
            form.elements.productCode.readOnly  = false;
            form.elements.productName.value     = p.productName;
            form.elements.monthlyFee.value      = p.monthlyFee;
            form.elements.contractMonths.value  = p.contractMonths;
            form.elements.depositAmount.value   = p.depositAmount;
            form.elements.installFee.value      = p.installFee;
            form.elements.description.value     = p.description ?? '';
            form.elements.useYn.value           = p.useYn ?? 'Y';

            document.querySelector('#useYnRow').classList.remove('d-none');

            const equipments = await loadEquipments();
            fillEquipmentSelect(equipments, p.equipmentId);
            getModal().show();
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    async function save() {
        const form = document.querySelector('#productForm');
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }
        const data = formToObject(form);

        try {
            if (mode === 'create') {
                const payload = {
                    productCode:    data.productCode,
                    equipmentId:    Number(data.equipmentId),
                    productName:    data.productName,
                    monthlyFee:     Number(data.monthlyFee),
                    contractMonths: Number(data.contractMonths),
                    depositAmount:  Number(data.depositAmount || 0),
                    installFee:     Number(data.installFee    || 0),
                    description:    data.description || null,
                };
                const res = await App.post('/api/products', payload);
                App.toast(res.message || '등록되었습니다', 'success');
            } else {
                const payload = {
                    productCode:    data.productCode,
                    equipmentId:    Number(data.equipmentId),
                    productName:    data.productName,
                    monthlyFee:     Number(data.monthlyFee),
                    contractMonths: Number(data.contractMonths),
                    depositAmount:  Number(data.depositAmount || 0),
                    installFee:     Number(data.installFee    || 0),
                    description:    data.description || null,
                    useYn:          data.useYn || 'Y',
                };
                const res = await App.put(`/api/products/${data.productId}`, payload);
                App.toast(res.message || '수정되었습니다', 'success');
            }
            getModal().hide();
            ProductList.load();
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
