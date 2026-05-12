package com.rental.crm.code.service;

import com.rental.crm.code.dto.CodeCreateRequest;
import com.rental.crm.code.dto.CodeGroupCreateRequest;
import com.rental.crm.code.dto.CodeGroupResponse;
import com.rental.crm.code.dto.CodeGroupSearchRequest;
import com.rental.crm.code.dto.CodeGroupUpdateRequest;
import com.rental.crm.code.dto.CodeResponse;
import com.rental.crm.code.dto.CodeSearchRequest;
import com.rental.crm.code.dto.CodeUpdateRequest;
import com.rental.crm.code.entity.Code;
import com.rental.crm.code.entity.CodeGroup;
import com.rental.crm.code.repository.CodeGroupRepository;
import com.rental.crm.code.repository.CodeRepository;
import com.rental.crm.common.exception.BusinessException;
import com.rental.crm.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 공통코드 도메인 — 04 §1-1.
 *
 * <p>그룹 + 코드값 한 서비스에 묶음 (도메인 응집도 우선).
 * 메서드 순서: 그룹 CRUD → 코드 CRUD → selectbox 헬퍼.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CodeService {

    private final CodeGroupRepository groupRepository;
    private final CodeRepository codeRepository;

    // ===================== 그룹 — Create =====================
    @Transactional
    public CodeGroupResponse registerGroup(CodeGroupCreateRequest req) {
        if (groupRepository.existsById(req.groupCode())) {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS,
                    "이미 존재하는 그룹 코드: " + req.groupCode());
        }
        var group = CodeGroup.builder()
                .groupCode(req.groupCode())
                .groupName(req.groupName())
                .description(req.description())
                .build();
        return CodeGroupResponse.from(groupRepository.save(group));
    }

    // ===================== 그룹 — Read =====================
    public Page<CodeGroupResponse> searchGroups(CodeGroupSearchRequest req, Pageable pageable) {
        return groupRepository.search(
                req.hasGroupCode() ? req.groupCode() : null,
                req.hasGroupName() ? req.groupName() : null,
                req.hasUseYn()     ? req.useYn()     : null,
                pageable
        ).map(CodeGroupResponse::from);
    }

    public CodeGroupResponse findGroup(String groupCode) {
        return CodeGroupResponse.from(loadGroup(groupCode));
    }

    // ===================== 그룹 — Update =====================
    @Transactional
    public CodeGroupResponse updateGroup(String groupCode, CodeGroupUpdateRequest req) {
        var group = loadGroup(groupCode);
        validateNotSystem(group);
        group.changeName(req.groupName());
        group.changeDescription(req.description());
        group.changeUseYn(req.useYn());
        return CodeGroupResponse.from(group);
    }

    // ===================== 그룹 — Delete =====================
    @Transactional
    public void deleteGroup(String groupCode) {
        var group = loadGroup(groupCode);
        validateNotSystem(group);
        long codeCount = codeRepository.countByGroupCode(groupCode);
        if (codeCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "하위 코드 " + codeCount + "건이 있어 삭제 불가. 비활성화(USE_YN='N') 권장.");
        }
        groupRepository.delete(group);
    }

    // ===================== 코드값 — Create =====================
    @Transactional
    public CodeResponse registerCode(CodeCreateRequest req) {
        var group = loadGroup(req.groupCode());
        validateNotSystem(group);
        if (codeRepository.existsByGroupCodeAndCodeValue(req.groupCode(), req.codeValue())) {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS,
                    "이미 존재하는 코드값: " + req.groupCode() + "/" + req.codeValue());
        }
        var code = Code.builder()
                .groupCode(req.groupCode())
                .codeValue(req.codeValue())
                .codeName(req.codeName())
                .sortOrder(req.sortOrder())
                .description(req.description())
                .propVal1(req.propVal1())
                .propVal2(req.propVal2())
                .propVal3(req.propVal3())
                .build();
        return CodeResponse.from(codeRepository.save(code));
    }

    // ===================== 코드값 — Read =====================
    public Page<CodeResponse> searchCodes(String groupCode, CodeSearchRequest req, Pageable pageable) {
        if (!groupRepository.existsById(groupCode)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "그룹 코드 없음: " + groupCode);
        }
        return codeRepository.search(
                groupCode,
                req.hasCodeValue() ? req.codeValue() : null,
                req.hasCodeName()  ? req.codeName()  : null,
                req.hasUseYn()     ? req.useYn()     : null,
                pageable
        ).map(CodeResponse::from);
    }

    public CodeResponse findCode(Long codeId) {
        return CodeResponse.from(loadCode(codeId));
    }

    /**
     * Selectbox 옵션 — 사용중(Y) 코드만 정렬 순서대로.
     * 화면 / 외부 호출자가 자주 쓰는 API.
     */
    public List<CodeResponse> findActiveCodesByGroup(String groupCode) {
        return codeRepository.findByGroupCode(groupCode, "Y")
                .stream()
                .map(CodeResponse::from)
                .toList();
    }

    // ===================== 코드값 — Update =====================
    @Transactional
    public CodeResponse updateCode(Long codeId, CodeUpdateRequest req) {
        var code = loadCode(codeId);
        validateNotSystem(loadGroup(code.getGroupCode()));
        code.changeName(req.codeName());
        code.changeSortOrder(req.sortOrder());
        code.changeDescription(req.description());
        code.changePropVal1(req.propVal1());
        code.changePropVal2(req.propVal2());
        code.changePropVal3(req.propVal3());
        code.changeUseYn(req.useYn());
        return CodeResponse.from(code);
    }

    // ===================== 코드값 — Delete (Soft) =====================
    @Transactional
    public void deactivateCode(Long codeId) {
        var code = loadCode(codeId);
        validateNotSystem(loadGroup(code.getGroupCode()));
        code.deactivate();
    }

    // ===================== Private =====================

    /** 시스템 코드 그룹 변경 차단 — backend/guide/conventions/delete-defense.md allow-list 패턴. */
    private void validateNotSystem(CodeGroup group) {
        if (group.isSystem()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "시스템 코드 그룹은 변경/삭제할 수 없습니다: " + group.getGroupCode());
        }
    }

    private CodeGroup loadGroup(String groupCode) {
        return groupRepository.findById(groupCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "그룹 코드 없음: " + groupCode));
    }

    private Code loadCode(Long codeId) {
        return codeRepository.findById(codeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "코드 없음: " + codeId));
    }
}
