package com.itasocialacademy.oitassist.logfile.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.itasocialacademy.oitassist.core.web.AppExceptionHttpStatusMapper;
import com.itasocialacademy.oitassist.logfile.api.LogFileResponse;
import com.itasocialacademy.oitassist.logfile.api.PageResponse;
import com.itasocialacademy.oitassist.logfile.dao.model.LogFileDownloadResult;
import com.itasocialacademy.oitassist.logfile.exceptions.InvalidLogFileNameException;
import com.itasocialacademy.oitassist.logfile.exceptions.LogFileNotFoundException;
import com.itasocialacademy.oitassist.logfile.service.LogFileService;
import com.itasocialacademy.oitassist.security.jwt.JwtFilter;
import java.nio.charset.StandardCharsets;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
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
@Import({
    LogFileControllerTest.SecurityTestConfiguration.class,
    AppExceptionHttpStatusMapper.class
})
class LogFileControllerTest {

    private static final String ENDPOINT =
        "/api/v1/admin/log-files";
    private static final String SEARCH_ENDPOINT =
        "/api/v1/admin/log-files/search";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LogFileController logFileController;

    @MockitoBean
    private LogFileService logFileService;

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

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldSearchLogFilesByNameWithDefaultPagination()
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

        when(logFileService.searchByName(
            any(String.class),
            any(Pageable.class)))
            .thenReturn(response);

        mockMvc.perform(
            get(SEARCH_ENDPOINT)
                .param("name", "app"))
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

        ArgumentCaptor<String> nameCaptor =
            ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<Pageable> pageableCaptor =
            ArgumentCaptor.forClass(Pageable.class);

        verify(logFileService)
            .searchByName(
                nameCaptor.capture(),
                pageableCaptor.capture());

        assertThat(nameCaptor.getValue())
            .isEqualTo("app");

        Pageable pageable =
            pageableCaptor.getValue();

        assertThat(pageable.getPageNumber())
            .isZero();

        assertThat(pageable.getPageSize())
            .isEqualTo(10);

        assertThat(pageable.getSort())
            .isEqualTo(Sort.unsorted());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUseProvidedPaginationWhenSearchingByName()
        throws Exception {

        PageResponse<LogFileResponse> response =
            new PageResponse<>(
                List.of(),
                2,
                5,
                12,
                3);

        when(logFileService.searchByName(
            any(String.class),
            any(Pageable.class)))
            .thenReturn(response);

        mockMvc.perform(
            get(SEARCH_ENDPOINT)
                .param("name", "app")
                .param("page", "2")
                .param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(2))
            .andExpect(jsonPath("$.size").value(5))
            .andExpect(jsonPath("$.totalElements").value(12))
            .andExpect(jsonPath("$.totalPages").value(3));

        ArgumentCaptor<Pageable> pageableCaptor =
            ArgumentCaptor.forClass(Pageable.class);

        verify(logFileService)
            .searchByName(
                eq("app"),
                pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber())
            .isEqualTo(2);

        assertThat(pageable.getPageSize())
            .isEqualTo(5);

        assertThat(pageable.getSort())
            .isEqualTo(Sort.unsorted());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUseProvidedSortWhenSearchingByName()
        throws Exception {

        PageResponse<LogFileResponse> response =
            new PageResponse<>(
                List.of(),
                0,
                10,
                0,
                0);

        when(logFileService.searchByName(
            any(String.class),
            any(Pageable.class)))
            .thenReturn(response);

        mockMvc.perform(
            get(SEARCH_ENDPOINT)
                .param("name", "app")
                .param("sort", "fileName,asc"))
            .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor =
            ArgumentCaptor.forClass(Pageable.class);

        verify(logFileService)
            .searchByName(
                eq("app"),
                pageableCaptor.capture());

        Pageable pageable =
            pageableCaptor.getValue();

        assertThat(
            pageable.getSort().getOrderFor("fileName"))
            .isEqualTo(
                Sort.Order.asc("fileName"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnEmptyPageWhenNoLogFilesMatchName()
        throws Exception {

        PageResponse<LogFileResponse> response =
            new PageResponse<>(
                List.of(),
                0,
                10,
                0,
                0);

        when(logFileService.searchByName(
            any(String.class),
            any(Pageable.class)))
            .thenReturn(response);

        mockMvc.perform(
            get(SEARCH_ENDPOINT)
                .param("name", "unknown"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.totalPages").value(0));

        verify(logFileService)
            .searchByName(
                eq("unknown"),
                any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnBadRequestWhenSearchNameIsMissing()
        throws Exception {

        mockMvc.perform(get(SEARCH_ENDPOINT))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("COMMON_VALIDATION_FAILED"))
            .andExpect(
                jsonPath("$.status")
                    .value(400))
            .andExpect(
                jsonPath("$.details.parameter")
                    .value("name"));

        verifyNoInteractions(logFileService);
    }

    @Test
    void shouldDownloadLogFile() throws Exception {
        byte[] fileContent =
            "test log content"
                .getBytes(StandardCharsets.UTF_8);

        LogFileDownloadResult downloadResult =
            new LogFileDownloadResult(
                "app.log",
                new ByteArrayResource(fileContent));

        when(logFileService.downloadFile("app.log"))
            .thenReturn(downloadResult);

        mockMvc.perform(
            get("/api/v1/admin/log-files/app.log/download")
                .with(
                    user("admin")
                        .roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(
                content().contentType(
                    MediaType.APPLICATION_OCTET_STREAM))
            .andExpect(
                header().string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    containsString("attachment")))
            .andExpect(
                header().string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    containsString("app.log")))
            .andExpect(
                content().bytes(fileContent));

        verify(logFileService)
            .downloadFile("app.log");
    }

    @Test
    void shouldReturnBadRequestForInvalidDownloadFileName()
        throws Exception {

        when(logFileService.downloadFile("app..log"))
            .thenThrow(
                new InvalidLogFileNameException());

        mockMvc.perform(
            get("/api/v1/admin/log-files/app..log/download")
                .with(
                    user("admin")
                        .roles("ADMIN")))
            .andExpect(
                status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("INVALID_LOG_FILE_NAME"));

        verify(logFileService)
            .downloadFile("app..log");
    }

    @Test
    void shouldReturnNotFoundWhenDownloadFileDoesNotExist()
        throws Exception {

        when(logFileService.downloadFile("missing.log"))
            .thenThrow(
                new LogFileNotFoundException(
                    "missing.log"));

        mockMvc.perform(
            get("/api/v1/admin/log-files/missing.log/download")
                .with(
                    user("admin")
                        .roles("ADMIN")))
            .andExpect(
                status().isNotFound())
            .andExpect(
                jsonPath("$.code")
                    .value("LOG_FILE_NOT_FOUND"));

        verify(logFileService)
            .downloadFile("missing.log");
    }

}