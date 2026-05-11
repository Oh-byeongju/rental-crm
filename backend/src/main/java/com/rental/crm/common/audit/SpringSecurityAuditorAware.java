package com.rental.crm.common.audit;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * JPA Auditing 의 @CreatedBy / @LastModifiedBy 값 공급자.
 * 비인증 컨텍스트 (배치, 스케줄러, Kafka Consumer) → "SYSTEM".
 */
@Component
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    private static final String SYSTEM = "SYSTEM";
    private static final String ANONYMOUS = "anonymousUser";

    @Override
    public Optional<String> getCurrentAuditor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || ANONYMOUS.equals(auth.getPrincipal())) {
            return Optional.of(SYSTEM);
        }
        return Optional.of(auth.getName());
    }
}
