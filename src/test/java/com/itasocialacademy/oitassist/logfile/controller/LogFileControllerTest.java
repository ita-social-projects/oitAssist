package com.itasocialacademy.oitassist.logfile.controller;

import com.itasocialacademy.oitassist.logfile.api.LogFileResponse;
import com.itasocialacademy.oitassist.logfile.api.PageResponse;
import com.itasocialacademy.oitassist.logfile.service.LogFileService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LogFileControllerTest {

    private static final String ENDPOINT =
        "/api/v1/admin/log-files";

    @Mock
    private LogFileService logFileService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LogFileController logFileController =
            new LogFileController(logFileService);

        mockMvc = MockMvcBuilders
            .standaloneSetup(logFileController)
            .build();
    }

    @Test
    void shouldUseDefaultPaginationParameters()
        throws Exception {

        PageResponse<LogFileResponse> response =
            new PageResponse<>(
                List.of(
                    new LogFileResponse(
                        "app.log",
                        1500,
                        Instant.parse(
                            "2026-07-22T12:00:00Z"))),
                0,
                20,
                1,
                1);

        when(logFileService.getAll(0, 10))
            .thenReturn(response);

        mockMvc.perform(get(ENDPOINT))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.content")
                    .isArray())
            .andExpect(
                jsonPath("$.content.length()")
                    .value(1))
            .andExpect(
                jsonPath("$.content[0].fileName")
                    .value("app.log"))
            .andExpect(
                jsonPath("$.content[0].size")
                    .value(1500))
            .andExpect(
                jsonPath("$.content[0].lastModified")
                    .value("2026-07-22T12:00:00Z"))
            .andExpect(
                jsonPath("$.page")
                    .value(0))
            .andExpect(
                jsonPath("$.size")
                    .value(20))
            .andExpect(
                jsonPath("$.totalElements")
                    .value(1))
            .andExpect(
                jsonPath("$.totalPages")
                    .value(1));

        verify(logFileService)
            .getAll(0, 10);
    }

    @Test
    void shouldPassProvidedPaginationParametersToService()
        throws Exception {

        PageResponse<LogFileResponse> response =
            new PageResponse<>(
                List.of(
                    new LogFileResponse(
                        "app.log.2026-07-21.log.gz",
                        500,
                        Instant.parse(
                            "2026-07-21T12:00:00Z"))),
                2,
                5,
                11,
                3);

        when(logFileService.getAll(2, 5))
            .thenReturn(response);

        mockMvc.perform(
            get(ENDPOINT)
                .param("page", "2")
                .param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.content.length()")
                    .value(1))
            .andExpect(
                jsonPath("$.content[0].fileName")
                    .value(
                        "app.log.2026-07-21.log.gz"))
            .andExpect(
                jsonPath("$.page")
                    .value(2))
            .andExpect(
                jsonPath("$.size")
                    .value(5))
            .andExpect(
                jsonPath("$.totalElements")
                    .value(11))
            .andExpect(
                jsonPath("$.totalPages")
                    .value(3));

        verify(logFileService)
            .getAll(2, 5);
    }

    @Test
    void shouldReturnEmptyContent()
        throws Exception {

        PageResponse<LogFileResponse> response =
            new PageResponse<>(
                List.of(),
                0,
                20,
                0,
                0);

        when(logFileService.getAll(0, 10))
            .thenReturn(response);

        mockMvc.perform(get(ENDPOINT))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.content")
                    .isArray())
            .andExpect(
                jsonPath("$.content")
                    .isEmpty())
            .andExpect(
                jsonPath("$.page")
                    .value(0))
            .andExpect(
                jsonPath("$.size")
                    .value(20))
            .andExpect(
                jsonPath("$.totalElements")
                    .value(0))
            .andExpect(
                jsonPath("$.totalPages")
                    .value(0));

        verify(logFileService)
            .getAll(0, 10);
    }

    @Test
    void shouldReturnMethodNotAllowedForPostRequest()
        throws Exception {

        mockMvc.perform(post(ENDPOINT))
            .andExpect(
                status().isMethodNotAllowed());
    }
}