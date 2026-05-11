package com.rental.crm.common.audit;

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
 * 모든 HTTP 요청마다 {@link AuditContext} 에 pgm_id (URI) + client IP 세팅.
 * finally 절에서 반드시 clear (ThreadLocal 누수 방지).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuditContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            var pgmId = req.getRequestURI();
            var ip = resolveClientIp(req);
            AuditContext.set(new AuditContext.AuditInfo(pgmId, ip));
            chain.doFilter(req, res);
        } finally {
            AuditContext.clear();
        }
    }

    private String resolveClientIp(HttpServletRequest req) {
        var xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        var real = req.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) {
            return real.trim();
        }
        return req.getRemoteAddr();
    }
}
