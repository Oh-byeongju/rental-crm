package com.rental.crm.admin.service;

import com.rental.crm.admin.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 권한 캐시 — ADR-010 §2-1, §2-2.
 *
 * <p>키: {@code permissions:user:{userId}}, value = JSON 배열, TTL = 30분.
 * Invalidation 트리거 — 권한 변경 시점에 명시적으로 호출.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PermissionCacheService {

    private static final String KEY_PREFIX_USER = "permissions:user:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final RedisTemplate<String, Object> redisTemplate;
    private final AdminUserRepository adminUserRepository;

    @SuppressWarnings("unchecked")
    public Set<String> get(Long userId) {
        Object cached = redisTemplate.opsForValue().get(keyForUser(userId));
        if (cached == null) return null;
        if (cached instanceof Collection<?> c) {
            Set<String> out = new HashSet<>();
            for (Object o : c) out.add(String.valueOf(o));
            return out;
        }
        return null;
    }

    public void put(Long userId, Set<String> codes) {
        redisTemplate.opsForValue().set(keyForUser(userId), new ArrayList<>(codes), TTL);
    }

    /** 단일 사용자 캐시 무효화 (사용자 GRANT/REVOKE 변경, 역할 변경 시). */
    public void invalidateUser(Long userId) {
        Boolean deleted = redisTemplate.delete(keyForUser(userId));
        log.debug("Invalidate user permission cache — userId={}, deleted={}", userId, deleted);
    }

    /**
     * 역할에 매핑된 모든 사용자 캐시 무효화 (CM_ROLE_AUTH 변경 시).
     * SCAN 대신 DB 조회 → 사용자 ID 셋으로 일괄 DEL (학습 환경 규모 가정).
     */
    public void invalidateByRole(Long roleId) {
        List<Long> userIds = adminUserRepository.findUserIdsByRoleId(roleId);
        if (userIds.isEmpty()) return;
        List<String> keys = new ArrayList<>(userIds.size());
        for (Long uid : userIds) keys.add(keyForUser(uid));
        Long deleted = redisTemplate.delete(keys);
        log.debug("Invalidate role={} permission caches — userIds={}, deleted={}",
                roleId, userIds.size(), deleted);
    }

    /** 전체 무효화 (CM_AUTH/CM_ROLE 삭제 등 광범위 변경 시). */
    public void invalidateAll() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX_USER + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.warn("Invalidate ALL permission caches — count={}", keys.size());
        }
    }

    private String keyForUser(Long userId) {
        return KEY_PREFIX_USER + userId;
    }
}
