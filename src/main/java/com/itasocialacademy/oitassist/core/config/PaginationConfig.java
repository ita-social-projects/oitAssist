package com.itasocialacademy.oitassist.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

/**
 * Enforces a global maximum page size for all {@code Pageable} arguments
 * resolved across the application's MVC layer, preventing clients from
 * requesting arbitrarily large pages regardless of which endpoint they call.
 */
@Configuration
public class PaginationConfig {
    public static final int MAX_PAGE_SIZE = 100;

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer pageableMaxSizeCustomizer() {
        return resolver -> resolver.setMaxPageSize(MAX_PAGE_SIZE);
    }
}