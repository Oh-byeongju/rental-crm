package com.rental.backoffice.common.config;

import com.rental.backoffice.admin.security.XUserIdAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 개발용 Security 설정. 모든 요청 permitAll.
 * X-User-Id 헤더 (ADR-010 §2-8) 가 있으면 임시 인증 시뮬레이션.
 *
 * TODO (JWT 도입 사이클):
 *  - JWT Bearer Token 검증
 *  - {@link XUserIdAuthenticationFilter} 제거
 *  - JTI 블랙리스트 (강제 로그아웃, ADR-010 §2-4)
 *  - 5회 실패 잠금 wire-up (CM_USER.LOGIN_FAIL_CNT)
 *  - Refresh Token 회전 (Redis 저장)
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final XUserIdAuthenticationFilter xUserIdAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()    // 개발 단계 — JWT 사이클에서 교체
                )
                // 임시 — X-User-Id 헤더 인증 시뮬레이션 (ADR-010 §2-8)
                .addFilterBefore(xUserIdAuthenticationFilter,
                                 UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
