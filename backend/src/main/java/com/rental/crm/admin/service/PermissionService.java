package com.rental.crm.admin.service;

import com.rental.crm.admin.entity.UserAuth;
import com.rental.crm.admin.repository.AdminUserRepository;
import com.rental.crm.admin.repository.UserAuthRepository;
import com.rental.crm.auth.entity.Role;
import com.rental.crm.auth.repository.RoleAuthRepository;
import com.rental.crm.auth.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * 권한 판정 — ADR-008/009/010.
 *
 * <p>최종 권한 = (역할 권한) ∪ (사용자 GRANT) − (사용자 REVOKE)
 *
 * <p>SUPER_ADMIN 은 모든 AUTH 자동 부여 (역할에 이미 매핑돼 있고, GRANT/REVOKE 도 무시).
 * Redis 캐시 wrapping 은 {@link PermissionCacheService}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionService {

    private final AdminUserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final RoleRepository roleRepository;
    private final RoleAuthRepository roleAuthRepository;
    private final PermissionCacheService cache;

    /**
     * 사용자의 최종 권한 셋. Redis 캐시 hit → 즉시 반환, miss → DB 조회 후 캐시.
     */
    public Set<String> getEffectiveAuthCodes(Long userId) {
        Set<String> cached = cache.get(userId);
        if (cached != null) return cached;
        Set<String> computed = computeFromDb(userId);
        cache.put(userId, computed);
        return computed;
    }

    public boolean has(Long userId, String authCode) {
        return getEffectiveAuthCodes(userId).contains(authCode);
    }

    private Set<String> computeFromDb(Long userId) {
        Long roleId = userRepository.findRoleIdByUserId(userId).orElse(null);
        Set<String> result = new HashSet<>();
        if (roleId != null) {
            // 역할 권한
            result.addAll(roleAuthRepository.findAuthCodesByRoleId(roleId));
            // SUPER_ADMIN 단축 — DB 시드 그대로 신뢰. 추가 GRANT/REVOKE 도 무시.
            boolean superAdmin = roleRepository.findById(roleId)
                    .map(Role::isSuperAdmin)
                    .orElse(false);
            if (superAdmin) {
                return result;
            }
        }
        // 사용자 GRANT
        result.addAll(userAuthRepository
                .findAuthCodesByUserIdAndGrantType(userId, UserAuth.TYPE_GRANT));
        // 사용자 REVOKE
        Set<String> revokes = new HashSet<>(userAuthRepository
                .findAuthCodesByUserIdAndGrantType(userId, UserAuth.TYPE_REVOKE));
        result.removeAll(revokes);
        return result;
    }
}
