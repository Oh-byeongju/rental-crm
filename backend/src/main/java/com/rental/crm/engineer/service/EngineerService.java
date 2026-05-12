package com.rental.crm.engineer.service;

import com.rental.crm.common.exception.BusinessException;
import com.rental.crm.common.exception.ErrorCode;
import com.rental.crm.engineer.dto.EngineerCreateRequest;
import com.rental.crm.engineer.dto.EngineerResponse;
import com.rental.crm.engineer.dto.EngineerSearchRequest;
import com.rental.crm.engineer.dto.EngineerUpdateRequest;
import com.rental.crm.engineer.entity.Engineer;
import com.rental.crm.engineer.repository.EngineerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기사 도메인 — 단순 CRUD + ENGINEER_TYPE 검증 + UNIQUE 사전 검증.
 *
 * <p>책임:
 * <ul>
 *   <li>CT_ENGINEER CRUD</li>
 *   <li>ENGINEER_CODE UNIQUE 사전 검증 — `api-safety.md §2-3`</li>
 *   <li>ENGINEER_TYPE 유효성 검증 — INTERNAL/EXTERNAL 만</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EngineerService {

    private final EngineerRepository engineerRepository;

    // ===================== Create =====================
    @Transactional
    public EngineerResponse register(EngineerCreateRequest req) {
        if (engineerRepository.existsByEngineerCode(req.engineerCode())) {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS,
                    "이미 등록된 기사코드: " + req.engineerCode());
        }
        validateEngineerType(req.engineerType());

        var engineer = Engineer.builder()
                .engineerCode(req.engineerCode())
                .engineerName(req.engineerName())
                .engineerType(req.engineerType())
                .phone(req.phone())
                .email(req.email())
                .area(req.area())
                .build();
        return EngineerResponse.from(engineerRepository.save(engineer));
    }

    // ===================== Read =====================
    public Page<EngineerResponse> search(EngineerSearchRequest req, Pageable pageable) {
        return engineerRepository.search(
                req.hasEngineerCode() ? req.engineerCode() : null,
                req.hasEngineerName() ? req.engineerName() : null,
                req.hasEngineerType() ? req.engineerType() : null,
                req.hasArea()         ? req.area()         : null,
                req.hasUseYn()        ? req.useYn()        : null,
                pageable
        ).map(EngineerResponse::from);
    }

    public EngineerResponse findById(Long engineerId) {
        return EngineerResponse.from(loadEngineer(engineerId));
    }

    // ===================== Update =====================
    @Transactional
    public EngineerResponse update(Long engineerId, EngineerUpdateRequest req) {
        var engineer = loadEngineer(engineerId);
        validateEngineerType(req.engineerType());

        // ENGINEER_CODE 변경 시 UNIQUE 사전 검증 (본인 제외)
        boolean codeChanged = !engineer.getEngineerCode().equals(req.engineerCode());
        if (codeChanged && engineerRepository.existsByEngineerCode(req.engineerCode())) {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS,
                    "이미 등록된 기사코드: " + req.engineerCode());
        }

        engineer.changeEngineerCode(req.engineerCode());
        engineer.changeEngineerName(req.engineerName());
        engineer.changeEngineerType(req.engineerType());
        engineer.changePhone(req.phone());
        engineer.changeEmail(req.email());
        engineer.changeArea(req.area());
        engineer.changeUseYn(req.useYn());

        return EngineerResponse.from(engineer);
    }

    // ===================== Delete (Soft) =====================
    @Transactional
    public void deactivate(Long engineerId) {
        var engineer = loadEngineer(engineerId);
        engineer.deactivate();
    }

    // ===================== Private =====================
    private Engineer loadEngineer(Long engineerId) {
        return engineerRepository.findById(engineerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "기사 없음: " + engineerId));
    }

    private void validateEngineerType(String type) {
        if (!Engineer.VALID_TYPES.contains(type)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "유효하지 않은 기사 유형: " + type + " (INTERNAL 또는 EXTERNAL)");
        }
    }
}
