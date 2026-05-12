/* ============================================================
 * 장비 등록/수정 모달
 *
 * 정책:
 *  - 등록: equipmentCode 입력 / useYn 행 숨김 (서버에서 'Y' 자동)
 *  - 수정: equipmentCode 변경 가능 / useYn 행 노출
 *  - EQUIPMENT_TYPE 은 CM_CODE 그룹에서 fetch — typesCache
 * ============================================================ */

const EquipmentForm = (() => {
    let modalInstance = null;
    let mode = 'create';
    let typesCache = null;

    function getModal() {
        if (!modalInstance) {
            modalInstance = new bootstrap.Modal(document.querySelector('#equipmentModal'));
        }
        return modalInstance;
    }

    async function loadTypes() {
        if (typesCache) return typesCache;
        try {
            const res = await App.get('/api/code-groups/EQUIPMENT_TYPE/options');
            typesCache = res.data ?? [];
            return typesCache;
        } catch (e) {
            App.toast(e.message, 'error');
            return [];
        }
    }

    function fillTypeSelect(types, selected) {
        const sel = document.querySelector('#formEquipmentType');
        sel.innerHTML = '<option value="">— 선택 —</option>';
        for (const t of types) {
            const opt = document.createElement('option');
            opt.value = t.codeValue;
            opt.textContent = t.codeName;
            if (selected != null && selected === t.codeValue) opt.selected = true;
            sel.appendChild(opt);
        }
    }

    async function openCreate() {
        mode = 'create';
        document.querySelector('#equipmentModalTitle').textContent = '장비 등록';
        const form = document.querySelector('#equipmentForm');
        form.reset();
        form.elements.equipmentId.value = '';
        form.elements.equipmentCode.readOnly = false;
        document.querySelector('#useYnRow').classList.add('d-none');

        const types = await loadTypes();
        fillTypeSelect(types, null);
        getModal().show();
    }

    async function openUpdate(equipmentId) {
        mode = 'update';
        try {
            const res = await App.get(`/api/equipments/${equipmentId}`);
            const e = res.data;
            document.querySelector('#equipmentModalTitle').textContent = `장비 수정 — ${e.equipmentCode}`;
            const form = document.querySelector('#equipmentForm');
            form.reset();
            form.elements.equipmentId.value    = e.equipmentId;
            form.elements.equipmentCode.value  = e.equipmentCode;
            form.elements.equipmentCode.readOnly = false;
            form.elements.modelName.value      = e.modelName;
            form.elements.manufacturer.value   = e.manufacturer;
            form.elements.releaseDate.value    = e.releaseDate ?? '';
            form.elements.imageUrl.value       = e.imageUrl ?? '';
            form.elements.description.value    = e.description ?? '';
            form.elements.stockQty.value       = e.stockQty ?? 0;
            form.elements.useYn.value          = e.useYn ?? 'Y';

            document.querySelector('#useYnRow').classList.remove('d-none');

            const types = await loadTypes();
            fillTypeSelect(types, e.equipmentType);
            getModal().show();
        } catch (e) {
            App.toast(e.message, 'error');
        }
    }

    async function save() {
        const form = document.querySelector('#equipmentForm');
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }
        const data = formToObject(form);

        try {
            if (mode === 'create') {
                const payload = {
                    equipmentCode: data.equipmentCode,
                    equipmentType: data.equipmentType,
                    modelName:     data.modelName,
                    manufacturer:  data.manufacturer,
                    releaseDate:   data.releaseDate || null,
                    imageUrl:      data.imageUrl    || null,
                    description:   data.description || null,
                    stockQty:      Number(data.stockQty || 0),
                };
                const res = await App.post('/api/equipments', payload);
                App.toast(res.message || '등록되었습니다', 'success');
            } else {
                const payload = {
                    equipmentCode: data.equipmentCode,
                    equipmentType: data.equipmentType,
                    modelName:     data.modelName,
                    manufacturer:  data.manufacturer,
                    releaseDate:   data.releaseDate || null,
                    imageUrl:      data.imageUrl    || null,
                    description:   data.description || null,
                    stockQty:      Number(data.stockQty || 0),
                    useYn:         data.useYn       || 'Y',
                };
                const res = await App.put(`/api/equipments/${data.equipmentId}`, payload);
                App.toast(res.message || '수정되었습니다', 'success');
            }
            getModal().hide();
            EquipmentList.load();
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
