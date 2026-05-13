package com.rental.batch.common.audit;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * batch 모듈은 인증 컨텍스트 없음 — 모든 INSERT/UPDATE 의 {@code FIRS/FINA_REG_USER_ID} 를 "SYSTEM" 으로 고정.
 *
 * <p>backoffice 의 {@code SpringSecurityAuditorAware} 와 동일한 역할이지만 spring-security 의존성 없음.
 */
@Component
public class BatchAuditorAware implements AuditorAware<String> {

    private static final String SYSTEM = "SYSTEM";

    @Override
    public Optional<String> getCurrentAuditor() {
        return Optional.of(SYSTEM);
    }
}
