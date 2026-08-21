package com.itasocialacademy.oitassist.version.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itasocialacademy.oitassist.core.web.AppExceptionHttpStatusMapper;
import com.itasocialacademy.oitassist.security.jwt.JwtFilter;
import com.itasocialacademy.oitassist.version.dao.dto.response.VersionResponse;
import com.itasocialacademy.oitassist.version.service.interfaces.VersionService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = VersionController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtFilter.class))
@Import(VersionControllerTest.SecurityTestConfiguration.class)
class VersionControllerTest {

    private static final String ENDPOINT = "/api/v1/version";
    private static final String COMMIT_ID = "3534bab24568a602605078b9264711223f218dd2";
    private static final String SHORT_COMMIT_ID = "3534bab";
    private static final String COMMIT_TIME = "2026-08-19T08:22:05Z";
    private static final String BUILD_TIME = "2026-08-19T19:55:29.490Z";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VersionService versionService;

    @MockitoBean
    private AppExceptionHttpStatusMapper appExceptionHttpStatusMapper;

    @Test
    void getVersion_ShouldReturnVersion_WhenCallerIsAnonymous() throws Exception {
        when(versionService.getVersion()).thenReturn(
            new VersionResponse(
                new VersionResponse.BackendVersion(
                    COMMIT_ID,
                    SHORT_COMMIT_ID,
                    Instant.parse(COMMIT_TIME),
                    "dev",
                    "0.0.1-SNAPSHOT"),
                Instant.parse(BUILD_TIME)));

        mockMvc.perform(get(ENDPOINT))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.backend.commitId").value(COMMIT_ID))
            .andExpect(jsonPath("$.backend.shortCommitId").value(SHORT_COMMIT_ID))
            .andExpect(jsonPath("$.backend.commitTime").value(COMMIT_TIME))
            .andExpect(jsonPath("$.backend.branch").value("dev"))
            .andExpect(jsonPath("$.backend.version").value("0.0.1-SNAPSHOT"))
            .andExpect(jsonPath("$.buildTime").value(BUILD_TIME));
    }

    @Test
    void getVersion_ShouldReturnOkWithEmptyValues_WhenBuildMetadataIsMissing() throws Exception {
        when(versionService.getVersion()).thenReturn(
            new VersionResponse(
                new VersionResponse.BackendVersion(null, null, null, null, null),
                null));

        mockMvc.perform(get(ENDPOINT))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.backend").exists())
            .andExpect(jsonPath("$.backend.commitId").isEmpty())
            .andExpect(jsonPath("$.backend.commitTime").isEmpty())
            .andExpect(jsonPath("$.backend.branch").isEmpty())
            .andExpect(jsonPath("$.buildTime").isEmpty());
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebSecurity
    static class SecurityTestConfiguration {
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) {
            return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorization -> authorization
                    .anyRequest()
                    .permitAll())
                .build();
        }
    }
}
