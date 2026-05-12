package com.rental.backoffice.admin.service;

import com.rental.backoffice.admin.dto.AdminUserCreateRequest;
import com.rental.backoffice.admin.dto.AdminUserPasswordResetRequest;
import com.rental.backoffice.admin.dto.AdminUserResponse;
import com.rental.backoffice.admin.dto.AdminUserSearchRequest;
import com.rental.backoffice.admin.dto.AdminUserUpdateRequest;
import com.rental.backoffice.admin.dto.UserAuthMatrixResponse;
import com.rental.backoffice.admin.dto.UserAuthUpdateRequest;
import com.rental.domain.admin.entity.AdminUser;
import com.rental.domain.admin.entity.UserAuth;
import com.rental.domain.admin.repository.AdminUserRepository;
import com.rental.domain.admin.repository.UserAuthRepository;
import com.rental.domain.auth.entity.Role;
import com.rental.domain.auth.repository.AuthRepository;
import com.rental.domain.auth.repository.RoleAuthRepository;
import com.rental.domain.auth.repository.RoleRepository;
import com.rental.domain.common.exception.BusinessException;
import com.rental.domain.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 관리자 도메인 — 04 §1-2, ADR-009/010.
 *
 * <p>책임:
 * <ul>
 *   <li>CM_USER CRUD + 잠금해제 + 비밀번호 재설정</li>
 *   <li>CM_USER_AUTH 매트릭스 조회/저장 (전체 삭제 → 재삽입)</li>
 *   <li>권한 변경 시 PermissionCacheService.invalidateUser 트리거</li>
 *   <li>마지막 활성 관리자 비활성화 방지 (04 §1-2)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final AdminUserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final RoleRepository roleRepository;
    private final RoleAuthRepository roleAuthRepository;
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionCacheService cache;

    // ===================== Create =====================
    @Transactional
    public AdminUserResponse register(AdminUserCreateRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS,
                    "이미 가입된 이메일: " + req.email());
        }
        if (req.roleId() != null && !roleRepository.existsById(req.roleId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "역할 없음: " + req.roleId());
        }
        var user = AdminUser.builder()
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .userName(req.userName())
                .phone(req.phone())
                .roleId(req.roleId())
                .build();
        var saved = userRepository.save(user);
        Role role = req.roleId() == null ? null
                : roleRepository.findById(req.roleId()).orElse(null);
        return AdminUserResponse.from(saved, role);
    }

    // ===================== Read =====================
    public Page<AdminUserResponse> search(AdminUserSearchRequest req, Pageable pageable) {
        Page<AdminUser> page = userRepository.search(
                req.hasEmail()    ? req.email()    : null,
                req.hasUserName() ? req.userName() : null,
                req.hasUseYn()    ? req.useYn()    : null,
                pageable);

        Set<Long> roleIds = page.getContent().stream()
                .map(AdminUser::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Role> roleMap = roleRepository.findAllById(roleIds).stream()
                .collect(Collectors.toMap(Role::getRoleId, r -> r));

        return page.map(u -> AdminUserResponse.from(u, roleMap.get(u.getRoleId())));
    }

    public AdminUserResponse findById(Long userId) {
        var user = loadUser(userId);
        Role role = user.getRoleId() == null ? null
                : roleRepository.findById(user.getRoleId()).orElse(null);
        return AdminUserResponse.from(user, role);
    }

    // ===================== Update =====================
    @Transactional
    public AdminUserResponse update(Long userId, AdminUserUpdateRequest req) {
        var user = loadUser(userId);

        // 마지막 활성 관리자 비활성화 방지 (04 §1-2)
        if ("Y".equals(user.getUseYn()) && "N".equals(req.useYn())) {
            if (userRepository.countByUseYn("Y") <= 1) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE,
                        "마지막 활성 관리자 비활성화 불가");
            }
        }
        if (req.roleId() != null && !roleRepository.existsById(req.roleId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "역할 없음: " + req.roleId());
        }

        boolean roleChanged = !Objects.equals(user.getRoleId(), req.roleId());
        user.changeName(req.userName());
        user.changePhone(req.phone());
        user.changeRole(req.roleId());
        user.changeUseYn(req.useYn());

        if (roleChanged) {
            cache.invalidateUser(userId);
        }

        Role role = req.roleId() == null ? null
                : roleRepository.findById(req.roleId()).orElse(null);
        return AdminUserResponse.from(user, role);
    }

    // ===================== Delete (Soft) =====================
    @Transactional
    public void deactivate(Long userId) {
        var user = loadUser(userId);
        if ("Y".equals(user.getUseYn()) && userRepository.countByUseYn("Y") <= 1) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "마지막 활성 관리자 비활성화 불가");
        }
        user.deactivate();
        cache.invalidateUser(userId);
    }

    // ===================== 잠금 해제 / 비밀번호 재설정 =====================
    @Transactional
    public void unlock(Long userId) {
        var user = loadUser(userId);
        user.unlock();
        // 잠금 해제는 권한 변경 아님 — 캐시 무효화 불필요
    }

    @Transactional
    public void resetPassword(Long userId, AdminUserPasswordResetRequest req) {
        var user = loadUser(userId);
        user.changePassword(passwordEncoder.encode(req.newPassword()));
        user.unlock(); // 통상 비밀번호 재설정 시 잠금도 함께 해제
    }

    // ===================== User-Auth 매트릭스 =====================

    public UserAuthMatrixResponse findMatrix(Long userId) {
        var user = loadUser(userId);

        List<String> roleAuths = user.getRoleId() == null
                ? List.of()
                : roleAuthRepository.findAuthCodesByRoleId(user.getRoleId());

        var mappings = userAuthRepository.findByUserId(userId);
        List<String> grants = mappings.stream()
                .filter(m -> UserAuth.TYPE_GRANT.equals(m.getGrantType()))
                .map(UserAuth::getAuthCode)
                .sorted()
                .toList();
        List<String> revokes = mappings.stream()
                .filter(m -> UserAuth.TYPE_REVOKE.equals(m.getGrantType()))
                .map(UserAuth::getAuthCode)
                .sorted()
                .toList();

        Set<String> effective = new HashSet<>(roleAuths);
        effective.addAll(grants);
        effective.removeAll(revokes);

        return new UserAuthMatrixResponse(
                new ArrayList<>(roleAuths),
                grants,
                revokes,
                effective.stream().sorted().toList()
        );
    }

    @Transactional
    public void updateUserAuths(Long userId, UserAuthUpdateRequest req) {
        loadUser(userId); // 존재 검증

        Set<String> grants  = new HashSet<>(req.grants());
        Set<String> revokes = new HashSet<>(req.revokes());

        // 동일 AUTH 가 GRANT/REVOKE 양쪽에 있으면 거부
        Set<String> overlap = grants.stream()
                .filter(revokes::contains)
                .collect(Collectors.toSet());
        if (!overlap.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "동일 AUTH 가 GRANT/REVOKE 양쪽에 있음: " + overlap);
        }

        // 유효한 AUTH_CODE 만 통과
        Set<String> validCodes = authRepository.findAll().stream()
                .map(a -> a.getAuthCode())
                .collect(Collectors.toSet());
        Set<String> requested = new HashSet<>();
        requested.addAll(grants);
        requested.addAll(revokes);
        Set<String> invalid = requested.stream()
                .filter(c -> !validCodes.contains(c))
                .collect(Collectors.toSet());
        if (!invalid.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "유효하지 않은 AUTH_CODE: " + invalid);
        }

        userAuthRepository.deleteByUserId(userId);

        List<UserAuth> toSave = new ArrayList<>(grants.size() + revokes.size());
        for (String c : grants) {
            toSave.add(UserAuth.builder()
                    .userId(userId).authCode(c).grantType(UserAuth.TYPE_GRANT).build());
        }
        for (String c : revokes) {
            toSave.add(UserAuth.builder()
                    .userId(userId).authCode(c).grantType(UserAuth.TYPE_REVOKE).build());
        }
        userAuthRepository.saveAll(toSave);

        cache.invalidateUser(userId);
    }

    // ===================== Private =====================
    private AdminUser loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "관리자 없음: " + userId));
    }
}
