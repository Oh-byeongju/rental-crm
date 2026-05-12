package com.rental.crm.auth.service;

import com.rental.crm.admin.service.PermissionCacheService;
import com.rental.crm.auth.dto.RoleAuthUpdateRequest;
import com.rental.crm.auth.dto.RoleCreateRequest;
import com.rental.crm.auth.dto.RoleResponse;
import com.rental.crm.auth.dto.RoleSearchRequest;
import com.rental.crm.auth.dto.RoleUpdateRequest;
import com.rental.crm.auth.entity.Role;
import com.rental.crm.auth.entity.RoleAuth;
import com.rental.crm.auth.repository.AuthRepository;
import com.rental.crm.auth.repository.RoleAuthRepository;
import com.rental.crm.auth.repository.RoleRepository;
import com.rental.crm.common.exception.BusinessException;
import com.rental.crm.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 역할 도메인 + 역할-권한 매핑 — 04 §1-3, ADR-008/009/010.
 *
 * <p>매트릭스 저장 방식: 기존 매핑 전체 삭제 → 새 authCodes 재삽입.
 * 권한 변경 후 Redis 캐시 무효화 트리거 (ADR-010 §2-2) — 5단계 후속 (관리자 도메인 완료 시점).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleService {

    private final RoleRepository roleRepository;
    private final RoleAuthRepository roleAuthRepository;
    private final AuthRepository authRepository;
    private final PermissionCacheService permissionCacheService;

    // ===================== Role — Create =====================
    @Transactional
    public RoleResponse register(RoleCreateRequest req) {
        if (roleRepository.existsByRoleCode(req.roleCode())) {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS,
                    "이미 존재하는 역할 코드: " + req.roleCode());
        }
        var role = Role.builder()
                .roleCode(req.roleCode())
                .roleName(req.roleName())
                .description(req.description())
                .build();
        return RoleResponse.from(roleRepository.save(role));
    }

    // ===================== Role — Read =====================
    public Page<RoleResponse> search(RoleSearchRequest req, Pageable pageable) {
        return roleRepository.search(
                req.hasRoleCode() ? req.roleCode() : null,
                req.hasRoleName() ? req.roleName() : null,
                req.hasUseYn()    ? req.useYn()    : null,
                pageable
        ).map(RoleResponse::from);
    }

    public RoleResponse findById(Long roleId) {
        return RoleResponse.from(loadRole(roleId));
    }

    // ===================== Role — Update =====================
    @Transactional
    public RoleResponse update(Long roleId, RoleUpdateRequest req) {
        var role = loadRole(roleId);
        if (role.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "SUPER_ADMIN 역할은 수정 불가");
        }
        role.changeName(req.roleName());
        role.changeDescription(req.description());
        role.changeUseYn(req.useYn());
        return RoleResponse.from(role);
    }

    // ===================== Role — Delete =====================
    @Transactional
    public void delete(Long roleId) {
        var role = loadRole(roleId);
        if (role.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "SUPER_ADMIN 역할은 삭제 불가");
        }
        long mappingCount = roleAuthRepository.countByRoleId(roleId);
        if (mappingCount > 0) {
            // 매핑 함께 삭제
            roleAuthRepository.deleteByRoleId(roleId);
        }
        // ADR-010 §2-2 — 사용자 캐시 무효화 (역할 사용 사용자가 있을 시점)
        permissionCacheService.invalidateByRole(roleId);
        // TODO 후속: 해당 역할 보유 사용자 존재 시 CONFLICT (정책 결정 후)
        roleRepository.delete(role);
    }

    // ===================== Role-Auth 매트릭스 =====================

    /** 역할에 부여된 AUTH_CODE 목록 — 매트릭스 화면 초기 체크 상태용. */
    public List<String> findAuthCodes(Long roleId) {
        loadRole(roleId);
        return roleAuthRepository.findAuthCodesByRoleId(roleId);
    }

    /**
     * 매트릭스 일괄 저장 — 전체 삭제 후 재삽입.
     * SUPER_ADMIN 은 변경 불가 (모든 AUTH 자동 부여 정책 — 04 §1-3).
     */
    @Transactional
    public void updateRoleAuths(Long roleId, RoleAuthUpdateRequest req) {
        var role = loadRole(roleId);
        if (role.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "SUPER_ADMIN 역할의 권한은 변경 불가 (모든 AUTH 자동 부여)");
        }

        // 유효한 AUTH_CODE 만 통과
        Set<String> validAuthCodes = authRepository.findAll().stream()
                .map(a -> a.getAuthCode())
                .collect(Collectors.toSet());

        Set<String> requested = new HashSet<>(req.authCodes());
        Set<String> invalid = requested.stream()
                .filter(c -> !validAuthCodes.contains(c))
                .collect(Collectors.toSet());
        if (!invalid.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "유효하지 않은 AUTH_CODE: " + invalid);
        }

        roleAuthRepository.deleteByRoleId(roleId);
        // 중복 제거 + INSERT
        var toSave = requested.stream()
                .map(authCode -> RoleAuth.builder().roleId(roleId).authCode(authCode).build())
                .toList();
        roleAuthRepository.saveAll(toSave);

        // ADR-010 §2-2 — 역할 권한 변경 시 해당 역할 보유 모든 사용자 캐시 무효화
        permissionCacheService.invalidateByRole(roleId);
    }

    // ===================== Private =====================
    private Role loadRole(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "역할 없음: " + roleId));
    }
}
