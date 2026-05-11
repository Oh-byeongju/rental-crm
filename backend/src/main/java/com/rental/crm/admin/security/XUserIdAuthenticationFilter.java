package com.rental.crm.admin.security;

import com.rental.crm.admin.service.PermissionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 임시 인증 시뮬레이션 — ADR-010 §2-8.
 *
 * <p>요청 헤더 {@code X-User-Id: {userId}} 가 있으면 PermissionService 로 권한 셋 조회 →
 * SecurityContext 에 `UsernamePasswordAuthenticationToken` 주입.
 *
 * <p>JWT 도입 사이클에서 제거 예정. 운영 진입 금지.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class XUserIdAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-User-Id";

    private final PermissionService permissionService;

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader(HEADER);
        if (header != null && !header.isBlank()) {
            try {
                Long userId = Long.parseLong(header.trim());
                Set<String> authCodes = permissionService.getEffectiveAuthCodes(userId);
                List<GrantedAuthority> authorities = new ArrayList<>(authCodes.size());
                for (String code : authCodes) {
                    authorities.add(new SimpleGrantedAuthority(code));
                }
                var token = new UsernamePasswordAuthenticationToken(
                        String.valueOf(userId), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(token);
                log.debug("X-User-Id simulation: userId={}, auths={}", userId, authCodes.size());
            } catch (NumberFormatException e) {
                log.warn("Invalid X-User-Id header: {}", header);
            }
        }
        chain.doFilter(req, res);
    }
}
