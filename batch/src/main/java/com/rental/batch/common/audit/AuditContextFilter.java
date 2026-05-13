package com.rental.batch.common.audit;

import com.rental.domain.common.audit.AuditContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * HTTP 요청마다 {@link AuditContext} 에 pgm_id (URI) + client IP 세팅.
 * backoffice 의 동명 클래스와 동일한 역할 — batch 도 REST 진입점에서 감사 컬럼 채워야 함.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuditContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            AuditContext.set(new AuditContext.AuditInfo(req.getRequestURI(), resolveClientIp(req)));
            chain.doFilter(req, res);
        } finally {
            AuditContext.clear();
        }
    }

    private String resolveClientIp(HttpServletRequest req) {
        var xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        var real = req.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real.trim();
        return req.getRemoteAddr();
    }
}
