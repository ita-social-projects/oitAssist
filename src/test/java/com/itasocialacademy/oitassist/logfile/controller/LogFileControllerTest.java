package com.itasocialacademy.oitassist.logfile.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itasocialacademy.oitassist.core.web.AppExceptionHttpStatusMapper;
import com.itasocialacademy.oitassist.logfile.api.LogFileResponse;
import com.itasocialacademy.oitassist.logfile.api.PageResponse;
import com.itasocialacademy.oitassist.logfile.service.LogFileService;
import com.itasocialacademy.oitassist.security.jwt.JwtFilter;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = LogFileController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtFilter.class))
@Import(LogFileControllerTest.SecurityTestConfiguration.class)
class LogFileControllerTest {

    private static final String ENDPOINT =
        "/api/v1/admin/log-files";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LogFileController logFileController;

    @MockitoBean
    private LogFileService logFileService;

    @MockitoBean
    private AppExceptionHttpStatusMapper appExceptionHttpStatusMapper;

    @Test
    void shouldLoadControllerThroughMethodSecurityProxy() {
        assertThat(AopUtils.isAopProxy(logFileController))
            .isTrue();
    }

    @Test
    void shouldDenyAnonymousCaller() throws Exception {
        mockMvc.perform(get(ENDPOINT))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
            .andExpect(jsonPath("$.status").value(403));

        verifyNoInteractions(logFileService);
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldDenyNonAdminCaller() throws Exception {
        mockMvc.perform(get(ENDPOINT))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
            .andExpect(jsonPath("$.status").value(403));

        verifyNoInteractions(logFileService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnLogFilesWithDefaultPagination()
        throws Exception {

        Instant lastModified =
            Instant.parse("2026-07-24T12:00:00Z");

        PageResponse<LogFileResponse> response =
            new PageResponse<>(
                List.of(
                    new LogFileResponse(
                        "app.log",
                        1024L,
                        lastModified)),
                0,
                10,
                1,
                1);

        when(logFileService.getAll(any(Pageable.class)))
            .thenReturn(response);

        mockMvc.perform(get(ENDPOINT))
            .andExpect(status().isOk())
            .andExpect(
                content().contentTypeCompatibleWith(
                    MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(
                jsonPath("$.content[0].fileName")
                    .value("app.log"))
            .andExpect(
                jsonPath("$.content[0].size")
                    .value(1024))
            .andExpect(
                jsonPath("$.content[0].lastModified")
                    .value(lastModified.toString()))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1));

        Pageable pageable = capturePageable();

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(
            pageable.getSort().getOrderFor("lastModified")).isEqualTo(
                Sort.Order.desc("lastModified"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUseProvidedPaginationParameters()
        throws Exception {

        PageResponse<LogFileResponse> response =
            new PageResponse<>(
                List.of(),
                2,
                5,
                12,
                3);

        when(logFileService.getAll(any(Pageable.class)))
            .thenReturn(response);

        mockMvc.perform(
            get(ENDPOINT)
                .param("page", "2")
                .param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(2))
            .andExpect(jsonPath("$.size").value(5))
            .andExpect(jsonPath("$.totalElements").value(12))
            .andExpect(jsonPath("$.totalPages").value(3));

        Pageable pageable = capturePageable();

        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(
            pageable.getSort().getOrderFor("lastModified")).isEqualTo(
                Sort.Order.desc("lastModified"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnEmptyPageWhenNoLogFilesExist()
        throws Exception {

        PageResponse<LogFileResponse> response =
            new PageResponse<>(
                List.of(),
                0,
                10,
                0,
                0);

        when(logFileService.getAll(any(Pageable.class)))
            .thenReturn(response);

        mockMvc.perform(get(ENDPOINT))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.totalPages").value(0));

    }

    private Pageable capturePageable() {
        ArgumentCaptor<Pageable> pageableCaptor =
            ArgumentCaptor.forClass(Pageable.class);

        verify(logFileService)
            .getAll(pageableCaptor.capture());

        return pageableCaptor.getValue();
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebSecurity
    @EnableMethodSecurity
    static class SecurityTestConfiguration {

        @Bean
        SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {
            return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                    authorization -> authorization
                        .anyRequest()
                        .permitAll())
                .build();
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUseProvidedSortParameter() throws Exception {
        PageResponse<LogFileResponse> response =
            new PageResponse<>(
                List.of(),
                0,
                10,
                0,
                0);

        when(logFileService.getAll(any(Pageable.class)))
            .thenReturn(response);

        mockMvc.perform(
            get(ENDPOINT)
                .param("sort", "fileName,asc"))
            .andExpect(status().isOk());

        Pageable pageable = capturePageable();

        assertThat(
            pageable.getSort().getOrderFor("fileName")).isEqualTo(
                Sort.Order.asc("fileName"));
    }
}