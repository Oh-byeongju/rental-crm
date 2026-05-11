package com.rental.crm.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 개발용 Security 설정. 현재는 모든 요청 permitAll.
 *
 * TODO (별도 ADR 예정):
 *  - JWT Filter 추가 (Bearer Token 검증)
 *  - 백오피스 vs 포털 분리 (/api/portal/** 별도 인증 흐름)
 *  - 5회 실패 잠금 (CM_USER.LOGIN_FAIL_CNT) — 04 §12-2
 *  - Refresh Token 회전 (Redis 저장)
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()    // 개발 단계 — 추후 JWT 인증으로 교체
                );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
