package com.rental.batch.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 활성화. {@code BatchAuditorAware} 가 SYSTEM 고정 공급.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "batchAuditorAware")
public class JpaAuditingConfig {
}
