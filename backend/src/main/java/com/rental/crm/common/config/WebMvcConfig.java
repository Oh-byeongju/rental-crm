package com.rental.crm.common.config;

import com.rental.crm.admin.security.SidebarMenuInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 설정 — 사이드바 메뉴 주입 인터셉터.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final SidebarMenuInterceptor sidebarMenuInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sidebarMenuInterceptor)
                .addPathPatterns("/", "/admin/**", "/customer-portal/**")
                .excludePathPatterns(
                        "/api/**",
                        "/css/**", "/js/**", "/img/**", "/webjars/**",
                        "/favicon.ico",
                        "/swagger-ui/**", "/api-docs/**", "/actuator/**"
                );
    }
}
