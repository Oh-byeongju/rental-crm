package com.rental.crm.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 활성화.
 * auditorAwareRef 는 빈 이름 = "springSecurityAuditorAware" (클래스명 + 첫글자 소문자).
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "springSecurityAuditorAware")
public class JpaAuditingConfig {
}
