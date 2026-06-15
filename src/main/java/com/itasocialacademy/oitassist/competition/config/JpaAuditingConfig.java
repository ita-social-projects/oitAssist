package com.itasocialacademy.oitassist.competition.config;

import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {
    @Bean
    public AuditorAware<Long> auditorProvider(SecurityFacade securityFacade) {
        return securityFacade::getCurrentUserId;
    }
}