package com.rental.batch.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 공유 시크릿 (X-Internal-Token) 기반 호출 차단 (ADR-014 Step 5).
 *
 * <p>대상: {@code /internal/**} 경로. 그 외(actuator 등) 는 통과.
 * <p>실패 시 401 + JSON {@code {"error":"invalid token"}}.
 *
 * <p>운영 단계 업그레이드 후보: mTLS / OAuth2 client_credentials / IP allow-list 결합.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)   // AuditContextFilter(+10) 다음
public class InternalTokenFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Token";
    private static final String PROTECTED_PREFIX = "/internal/";

    private final String expectedToken;

    public InternalTokenFilter(@Value("${app.internal.batch.token}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        var path = req.getRequestURI();
        if (!path.startsWith(PROTECTED_PREFIX)) {
            chain.doFilter(req, res);
            return;
        }
        var token = req.getHeader(HEADER);
        if (token == null || !token.equals(expectedToken)) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"error\":\"invalid token\"}");
            return;
        }
        chain.doFilter(req, res);
    }
}
