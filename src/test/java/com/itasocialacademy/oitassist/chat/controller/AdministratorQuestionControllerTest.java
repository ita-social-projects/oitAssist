package com.itasocialacademy.oitassist.chat.controller;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.IN_REVIEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static com.itasocialacademy.oitassist.core.config.PaginationConfig.MAX_PAGE_SIZE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.chat.dao.dto.response.AdminQuestionInboxItemResponseDTO;
import com.itasocialacademy.oitassist.chat.service.interfaces.AdministratorQuestionService;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;

class AdministratorQuestionControllerTest
    extends ControllerUnitTest<AdministratorQuestionController> {

    private static final String INBOX_URL =
        "/api/v1/admin/questions/inbox";

    private static final String ASSIGNED_URL =
        "/api/v1/admin/questions/assigned-to-me";

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private static final Long QUESTION_ID = 10L;
    private static final Long TASK_ASSIGNMENT_ID = 20L;
    private static final Long AUTHOR_ID = 30L;
    private static final Long REVIEWER_ID = 40L;

    private static final Instant CREATED_AT =
        Instant.parse("2026-08-01T10:00:00Z");

    private static final Instant UPDATED_AT =
        Instant.parse("2026-08-01T10:15:00Z");

    @Mock
    private AdministratorQuestionService administratorQuestionService;

    @InjectMocks
    private AdministratorQuestionController administratorQuestionController;

    @Override
    protected AdministratorQuestionController getController() {

        return administratorQuestionController;
    }

    @Test
    void controller_shouldRequireGlobalAdministratorRole() {
        PreAuthorize annotation =
            AdministratorQuestionController.class
                .getAnnotation(
                    PreAuthorize.class);

        assertNotNull(annotation);

        assertEquals(
            "hasRole('ADMIN')",
            annotation.value());
    }

    @Test
    void getUnclaimedQuestions_defaultPagination_shouldReturnPage() throws Exception {
        Page<AdminQuestionInboxItemResponseDTO> page =
            new PageImpl<>(
                List.of(createResponse()),
                PageRequest.of(
                    DEFAULT_PAGE,
                    DEFAULT_SIZE),
                1);

        when(administratorQuestionService
            .getUnclaimedQuestions(
                DEFAULT_PAGE,
                DEFAULT_SIZE))
            .thenReturn(page);

        mockMvc.perform(
            get(INBOX_URL))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.content")
                    .isArray())
            .andExpect(
                jsonPath("$.content.length()")
                    .value(1))
            .andExpect(
                jsonPath("$.content[0].id")
                    .value(QUESTION_ID))
            .andExpect(
                jsonPath("$.content[0].taskAssignmentId")
                    .value(TASK_ASSIGNMENT_ID))
            .andExpect(
                jsonPath("$.content[0].authorId")
                    .value(AUTHOR_ID))
            .andExpect(
                jsonPath("$.content[0].assignedReviewerId")
                    .value(REVIEWER_ID))
            .andExpect(
                jsonPath("$.content[0].title")
                    .value("Question title"))
            .andExpect(
                jsonPath("$.content[0].status")
                    .value("IN_REVIEW"))
            .andExpect(
                jsonPath("$.content[0].state")
                    .value("OPEN"))
            .andExpect(
                jsonPath("$.content[0].visibility")
                    .value("PRIVATE"))
            .andExpect(
                jsonPath("$.content[0].version")
                    .value(2))
            .andExpect(
                jsonPath("$.pageNumber")
                    .value(DEFAULT_PAGE))
            .andExpect(
                jsonPath("$.pageSize")
                    .value(DEFAULT_SIZE))
            .andExpect(
                jsonPath("$.totalElements")
                    .value(1));

        verify(administratorQuestionService)
            .getUnclaimedQuestions(
                DEFAULT_PAGE,
                DEFAULT_SIZE);
    }

    @Test
    void getUnclaimedQuestions_explicitPagination_shouldDelegateExactValues()
        throws Exception {

        when(administratorQuestionService
            .getUnclaimedQuestions(
                2,
                15))
            .thenReturn(
                Page.empty(
                    PageRequest.of(
                        2,
                        15)));

        mockMvc.perform(
            get(INBOX_URL)
                .param("page", "2")
                .param("size", "15"))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.pageNumber")
                    .value(2))
            .andExpect(
                jsonPath("$.pageSize")
                    .value(15));

        verify(administratorQuestionService)
            .getUnclaimedQuestions(
                2,
                15);
    }

    @Test
    void getUnclaimedQuestions_emptyResult_shouldReturnEmptyPage()
        throws Exception {

        when(administratorQuestionService
            .getUnclaimedQuestions(
                DEFAULT_PAGE,
                DEFAULT_SIZE))
            .thenReturn(
                Page.empty(
                    PageRequest.of(
                        DEFAULT_PAGE,
                        DEFAULT_SIZE)));

        mockMvc.perform(
            get(INBOX_URL))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.content")
                    .isEmpty())
            .andExpect(
                jsonPath("$.totalElements")
                    .value(0))
            .andExpect(
                jsonPath("$.first")
                    .value(true))
            .andExpect(
                jsonPath("$.last")
                    .value(true));
    }

    @Test
    void getAssignedQuestions_withoutStatus_shouldDelegateNullFilter()
        throws Exception {

        when(administratorQuestionService
            .getAssignedQuestions(
                null,
                DEFAULT_PAGE,
                DEFAULT_SIZE))
            .thenReturn(
                Page.empty(
                    PageRequest.of(
                        DEFAULT_PAGE,
                        DEFAULT_SIZE)));

        mockMvc.perform(
            get(ASSIGNED_URL))
            .andExpect(status().isOk());

        verify(administratorQuestionService)
            .getAssignedQuestions(
                null,
                DEFAULT_PAGE,
                DEFAULT_SIZE);
    }

    @Test
    void getAssignedQuestions_validStatus_shouldDelegateExactFilter()
        throws Exception {

        Page<AdminQuestionInboxItemResponseDTO> page =
            new PageImpl<>(
                List.of(createResponse()),
                PageRequest.of(
                    1,
                    10),
                1);

        when(administratorQuestionService
            .getAssignedQuestions(
                IN_REVIEW,
                1,
                10))
            .thenReturn(page);

        mockMvc.perform(
            get(ASSIGNED_URL)
                .param(
                    "status",
                    "IN_REVIEW")
                .param(
                    "page",
                    "1")
                .param(
                    "size",
                    "10"))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.content[0].status")
                    .value("IN_REVIEW"))
            .andExpect(
                jsonPath("$.pageNumber")
                    .value(1))
            .andExpect(
                jsonPath("$.pageSize")
                    .value(10));

        verify(administratorQuestionService)
            .getAssignedQuestions(
                IN_REVIEW,
                1,
                10);
    }

    @Test
    void getAssignedQuestions_invalidStatus_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            get(ASSIGNED_URL)
                .param(
                    "status",
                    "INVALID"))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(
            administratorQuestionService);
    }

    @Test
    void getInbox_negativePage_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            get(INBOX_URL)
                .param(
                    "page",
                    "-1"))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(
            administratorQuestionService);
    }

    @Test
    void getAssignedQuestions_zeroSize_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            get(ASSIGNED_URL)
                .param(
                    "size",
                    "0"))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(
            administratorQuestionService);
    }

    @Test
    void getInbox_sizeAboveMaximum_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            get(INBOX_URL)
                .param(
                    "size",
                    String.valueOf(
                        MAX_PAGE_SIZE + 1)))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(
            administratorQuestionService);
    }

    @Test
    void getInbox_nonnumericPage_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            get(INBOX_URL)
                .param(
                    "page",
                    "invalid"))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(
            administratorQuestionService);
    }

    @Test
    void getAssignedQuestions_unauthenticated_shouldReturn401()
        throws Exception {

        when(administratorQuestionService
            .getAssignedQuestions(
                any(),
                eq(DEFAULT_PAGE),
                eq(DEFAULT_SIZE)))
            .thenThrow(
                authenticationException());

        mockMvc.perform(
            get(ASSIGNED_URL))
            .andExpect(status().isUnauthorized())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "AUTHENTICATION_REQUIRED"));
    }

    @Test
    void getInbox_nonAdministrator_shouldReturn403()
        throws Exception {

        when(administratorQuestionService
            .getUnclaimedQuestions(
                DEFAULT_PAGE,
                DEFAULT_SIZE))
            .thenThrow(
                authorizationException());

        mockMvc.perform(
            get(INBOX_URL))
            .andExpect(status().isForbidden())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "ACCESS_DENIED"));
    }

    @Test
    void getAssignedQuestions_orgWithoutAdmin_shouldReturn403()
        throws Exception {

        when(administratorQuestionService
            .getAssignedQuestions(
                any(),
                eq(DEFAULT_PAGE),
                eq(DEFAULT_SIZE)))
            .thenThrow(
                authorizationException());

        mockMvc.perform(
            get(ASSIGNED_URL)
                .param(
                    "status",
                    "NEW"))
            .andExpect(status().isForbidden())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "ACCESS_DENIED"));
    }

    private AdminQuestionInboxItemResponseDTO createResponse() {
        return new AdminQuestionInboxItemResponseDTO(
            QUESTION_ID,
            TASK_ASSIGNMENT_ID,
            AUTHOR_ID,
            REVIEWER_ID,
            "Question title",
            IN_REVIEW,
            OPEN,
            PRIVATE,
            2L,
            CREATED_AT,
            UPDATED_AT);
    }

    private AuthenticationException authenticationException() {
        return new AuthenticationException(
            "Authentication is required to access "
                + "the administrator question inbox",
            ErrorCode.AUTHENTICATION_REQUIRED);
    }

    private AuthorizationException authorizationException() {
        return new AuthorizationException(
            "Global administrator role is required "
                + "to access the question inbox",
            ErrorCode.ACCESS_DENIED);
    }
}