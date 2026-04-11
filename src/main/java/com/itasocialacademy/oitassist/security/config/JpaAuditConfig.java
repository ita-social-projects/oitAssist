package com.itasocialacademy.oitassist.security.config;

import com.itasocialacademy.oitassist.security.api.interfaces.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuration for JPA Auditing. Enables automatic population of auditing
 * fields like createdBy and lastModifiedBy.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@RequiredArgsConstructor
public class JpaAuditConfig {
    private final SecurityService securityService;

    /**
     * Provides the current auditor's ID (the currently authenticated user).
     *
     * @return an AuditorAware implementation that uses SecurityService.
     */
    @Bean
    public AuditorAware<Long> auditorProvider() {
        return securityService::getCurrentUserId;
    }
}
