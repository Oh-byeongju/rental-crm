package com.rental.crm.equipment.service;

import com.rental.crm.code.entity.Code;
import com.rental.crm.code.repository.CodeRepository;
import com.rental.crm.common.exception.BusinessException;
import com.rental.crm.common.exception.ErrorCode;
import com.rental.crm.equipment.dto.EquipmentCreateRequest;
import com.rental.crm.equipment.dto.EquipmentResponse;
import com.rental.crm.equipment.dto.EquipmentSearchRequest;
import com.rental.crm.equipment.dto.EquipmentUpdateRequest;
import com.rental.crm.equipment.entity.Equipment;
import com.rental.crm.equipment.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 장비 도메인 — 단순 CRUD + EQUIPMENT_TYPE 코드 검증.
 *
 * <p>책임:
 * <ul>
 *   <li>CT_EQUIPMENT CRUD</li>
 *   <li>EQUIPMENT_CODE / (MODEL_NAME, MANUFACTURER) UNIQUE 사전 검증
 *       — `backend/guide/conventions/api-safety.md §2-3`</li>
 *   <li>EQUIPMENT_TYPE 유효성 검증 — CM_CODE 그룹 EQUIPMENT_TYPE 의 사용중 코드만 허용</li>
 *   <li>응답 매핑 시 equipmentTypeName 함께 반환 (그리드 표시용)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EquipmentService {

    private static final String EQUIPMENT_TYPE_GROUP = "EQUIPMENT_TYPE";

    private final EquipmentRepository equipmentRepository;
    private final CodeRepository codeRepository;

    // ===================== Create =====================
    @Transactional
    public EquipmentResponse register(EquipmentCreateRequest req) {
        if (equipmentRepository.existsByEquipmentCode(req.equipmentCode())) {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS,
                    "이미 등록된 장비코드: " + req.equipmentCode());
        }
        if (equipmentRepository.existsByModelNameAndManufacturer(req.modelName(), req.manufacturer())) {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS,
                    "이미 등록된 모델: " + req.modelName() + " / " + req.manufacturer());
        }
        validateEquipmentType(req.equipmentType());

        var equipment = Equipment.builder()
                .equipmentCode(req.equipmentCode())
                .equipmentType(req.equipmentType())
                .modelName(req.modelName())
                .manufacturer(req.manufacturer())
                .releaseDate(req.releaseDate())
                .imageUrl(req.imageUrl())
                .description(req.description())
                .stockQty(req.stockQty())
                .build();
        var saved = equipmentRepository.save(equipment);
        return toResponse(saved);
    }

    // ===================== Read =====================
    public Page<EquipmentResponse> search(EquipmentSearchRequest req, Pageable pageable) {
        Page<Equipment> page = equipmentRepository.search(
                req.hasEquipmentCode() ? req.equipmentCode() : null,
                req.hasEquipmentType() ? req.equipmentType() : null,
                req.hasModelName()     ? req.modelName()     : null,
                req.hasManufacturer()  ? req.manufacturer()  : null,
                req.hasUseYn()         ? req.useYn()         : null,
                req.hasStockFilter()   ? req.stockFilter()   : null,
                pageable);

        Map<String, String> nameMap = loadTypeNameMap();
        return page.map(e -> EquipmentResponse.from(e, nameMap.get(e.getEquipmentType())));
    }

    public EquipmentResponse findById(Long equipmentId) {
        var equipment = loadEquipment(equipmentId);
        return toResponse(equipment);
    }

    // ===================== Update =====================
    @Transactional
    public EquipmentResponse update(Long equipmentId, EquipmentUpdateRequest req) {
        var equipment = loadEquipment(equipmentId);
        validateEquipmentType(req.equipmentType());

        // 장비코드 변경 시 UNIQUE 사전 검증 (본인 제외)
        boolean codeChanged = !equipment.getEquipmentCode().equals(req.equipmentCode());
        if (codeChanged && equipmentRepository.existsByEquipmentCode(req.equipmentCode())) {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS,
                    "이미 등록된 장비코드: " + req.equipmentCode());
        }

        // 모델명+제조사 변경 시 UNIQUE 사전 검증 (본인 제외)
        boolean modelChanged = !equipment.getModelName().equals(req.modelName())
                            || !equipment.getManufacturer().equals(req.manufacturer());
        if (modelChanged
                && equipmentRepository.existsByModelNameAndManufacturer(req.modelName(), req.manufacturer())) {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS,
                    "이미 등록된 모델: " + req.modelName() + " / " + req.manufacturer());
        }

        equipment.changeEquipmentCode(req.equipmentCode());
        equipment.changeEquipmentType(req.equipmentType());
        equipment.changeModelName(req.modelName());
        equipment.changeManufacturer(req.manufacturer());
        equipment.changeReleaseDate(req.releaseDate());
        equipment.changeImageUrl(req.imageUrl());
        equipment.changeDescription(req.description());
        equipment.changeStockQty(req.stockQty());
        equipment.changeUseYn(req.useYn());

        return toResponse(equipment);
    }

    // ===================== Delete (Soft) =====================
    @Transactional
    public void deactivate(Long equipmentId) {
        var equipment = loadEquipment(equipmentId);
        equipment.deactivate();
    }

    // ===================== Private =====================
    private Equipment loadEquipment(Long equipmentId) {
        return equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "장비 없음: " + equipmentId));
    }

    private void validateEquipmentType(String codeValue) {
        codeRepository.findByGroupCodeAndCodeValue(EQUIPMENT_TYPE_GROUP, codeValue)
                .filter(c -> "Y".equals(c.getUseYn()))
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "유효하지 않은 장비 유형: " + codeValue));
    }

    private EquipmentResponse toResponse(Equipment e) {
        String typeName = codeRepository
                .findByGroupCodeAndCodeValue(EQUIPMENT_TYPE_GROUP, e.getEquipmentType())
                .map(Code::getCodeName)
                .orElse(null);
        return EquipmentResponse.from(e, typeName);
    }

    private Map<String, String> loadTypeNameMap() {
        return codeRepository.findByGroupCode(EQUIPMENT_TYPE_GROUP, null).stream()
                .collect(Collectors.toMap(Code::getCodeValue, Code::getCodeName));
    }
}
