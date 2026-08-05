package com.itasocialacademy.oitassist.chat.controller;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.IN_REVIEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.NEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static com.itasocialacademy.oitassist.core.config.PaginationConfig.MAX_PAGE_SIZE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionAlreadyClaimedException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import org.springframework.http.MediaType;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionReviewInboxItemResponseDTO;
import com.itasocialacademy.oitassist.chat.service.interfaces.OrganizationQuestionService;
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

class OrganizationQuestionControllerTest
    extends ControllerUnitTest<OrganizationQuestionController> {

    private static final String INBOX_URL =
        "/api/v1/org/questions/inbox";

    private static final String ASSIGNED_URL =
        "/api/v1/org/questions/assigned-to-me";

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private static final Long QUESTION_ID = 10L;
    private static final Long TASK_ASSIGNMENT_ID = 20L;
    private static final Long AUTHOR_ID = 30L;
    private static final Long RESPONDER_ID = 40L;

    private static final Long EXPECTED_VERSION =
        2L;

    private static final String CLAIM_URL =
        "/api/v1/org/questions/"
            + QUESTION_ID
            + "/claim";

    private static final Instant CREATED_AT =
        Instant.parse("2026-08-05T10:00:00Z");

    private static final Instant UPDATED_AT =
        Instant.parse("2026-08-05T10:15:00Z");

    @Mock
    private OrganizationQuestionService organizationQuestionService;

    @InjectMocks
    private OrganizationQuestionController organizationQuestionController;

    @Override
    protected OrganizationQuestionController getController() {

        return organizationQuestionController;
    }

    @Test
    void controller_shouldRequireGlobalOrganizationRole() {

        PreAuthorize annotation =
            OrganizationQuestionController.class
                .getAnnotation(
                    PreAuthorize.class);

        assertNotNull(annotation);

        assertEquals(
            "hasRole('ORG')",
            annotation.value());
    }

    @Test
    void getResponderInbox_defaultPagination_shouldReturnPage()
        throws Exception {

        QuestionReviewInboxItemResponseDTO response =
            createResponse(
                null,
                NEW);

        Page<QuestionReviewInboxItemResponseDTO> page =
            new PageImpl<>(
                List.of(response),
                PageRequest.of(
                    DEFAULT_PAGE,
                    DEFAULT_SIZE),
                1);

        when(organizationQuestionService
            .getResponderInbox(
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
                jsonPath("$.content[0].title")
                    .value("Question title"))
            .andExpect(
                jsonPath("$.content[0].status")
                    .value("NEW"))
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
                jsonPath("$.content[0].createdAt")
                    .value(CREATED_AT.toString()))
            .andExpect(
                jsonPath("$.content[0].updatedAt")
                    .value(UPDATED_AT.toString()))
            .andExpect(
                jsonPath("$.content[0].content")
                    .doesNotExist())
            .andExpect(
                jsonPath("$.pageNumber")
                    .value(DEFAULT_PAGE))
            .andExpect(
                jsonPath("$.pageSize")
                    .value(DEFAULT_SIZE))
            .andExpect(
                jsonPath("$.totalElements")
                    .value(1))
            .andExpect(
                jsonPath("$.first")
                    .value(true))
            .andExpect(
                jsonPath("$.last")
                    .value(true));

        verify(organizationQuestionService)
            .getResponderInbox(
                DEFAULT_PAGE,
                DEFAULT_SIZE);
    }

    @Test
    void getResponderInbox_explicitPagination_shouldDelegateExactValues()
        throws Exception {

        when(organizationQuestionService
            .getResponderInbox(
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

        verify(organizationQuestionService)
            .getResponderInbox(
                2,
                15);
    }

    @Test
    void getResponderInbox_emptyResult_shouldReturnEmptyPage()
        throws Exception {

        when(organizationQuestionService
            .getResponderInbox(
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
    void getAssignedToCurrentResponder_withoutStatus_shouldDelegateNullFilter()
        throws Exception {

        when(organizationQuestionService
            .getAssignedToCurrentResponder(
                isNull(),
                eq(DEFAULT_PAGE),
                eq(DEFAULT_SIZE)))
            .thenReturn(
                Page.empty(
                    PageRequest.of(
                        DEFAULT_PAGE,
                        DEFAULT_SIZE)));

        mockMvc.perform(
            get(ASSIGNED_URL))
            .andExpect(status().isOk());

        verify(organizationQuestionService)
            .getAssignedToCurrentResponder(
                null,
                DEFAULT_PAGE,
                DEFAULT_SIZE);
    }

    @Test
    void getAssignedToCurrentResponder_validStatus_shouldReturnPage()
        throws Exception {

        QuestionReviewInboxItemResponseDTO response =
            createResponse(
                RESPONDER_ID,
                IN_REVIEW);

        Page<QuestionReviewInboxItemResponseDTO> page =
            new PageImpl<>(
                List.of(response),
                PageRequest.of(
                    1,
                    10),
                1);

        when(organizationQuestionService
            .getAssignedToCurrentResponder(
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
                jsonPath("$.content[0].assignedReviewerId")
                    .value(RESPONDER_ID))
            .andExpect(
                jsonPath("$.content[0].status")
                    .value("IN_REVIEW"))
            .andExpect(
                jsonPath("$.pageNumber")
                    .value(1))
            .andExpect(
                jsonPath("$.pageSize")
                    .value(10));

        verify(organizationQuestionService)
            .getAssignedToCurrentResponder(
                IN_REVIEW,
                1,
                10);
    }

    @Test
    void claimQuestion_validRequest_shouldReturnUpdatedQuestion()
        throws Exception {

        QuestionThreadResponseDTO response =
            createClaimedResponse();

        when(organizationQuestionService
            .claimQuestion(
                QUESTION_ID,
                EXPECTED_VERSION))
            .thenReturn(
                response);

        mockMvc.perform(
            post(CLAIM_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "version": 2
                    }
                    """))
            .andExpect(
                status().isOk())
            .andExpect(
                content().contentTypeCompatibleWith(
                    MediaType.APPLICATION_JSON))
            .andExpect(
                jsonPath("$.id")
                    .value(QUESTION_ID))
            .andExpect(
                jsonPath("$.taskAssignmentId")
                    .value(TASK_ASSIGNMENT_ID))
            .andExpect(
                jsonPath("$.authorId")
                    .value(AUTHOR_ID))
            .andExpect(
                jsonPath("$.assignedReviewerId")
                    .value(RESPONDER_ID))
            .andExpect(
                jsonPath("$.title")
                    .value("Question title"))
            .andExpect(
                jsonPath("$.content")
                    .value("Question content"))
            .andExpect(
                jsonPath("$.status")
                    .value("IN_REVIEW"))
            .andExpect(
                jsonPath("$.state")
                    .value("OPEN"))
            .andExpect(
                jsonPath("$.visibility")
                    .value("PRIVATE"))
            .andExpect(
                jsonPath("$.version")
                    .value(
                        EXPECTED_VERSION + 1));

        verify(organizationQuestionService)
            .claimQuestion(
                QUESTION_ID,
                EXPECTED_VERSION);
    }

    @Test
    void claimQuestion_nonPositiveQuestionId_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            post(
                "/api/v1/org/questions/0/claim")
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "version": 2
                    }
                    """))
            .andExpect(
                status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(
            organizationQuestionService);
    }

    @Test
    void claimQuestion_missingVersion_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            post(CLAIM_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(
                status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(
            organizationQuestionService);
    }

    @Test
    void claimQuestion_negativeVersion_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            post(CLAIM_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "version": -1
                    }
                    """))
            .andExpect(
                status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(
            organizationQuestionService);
    }

    @Test
    void claimQuestion_unauthenticated_shouldReturn401()
        throws Exception {

        when(organizationQuestionService
            .claimQuestion(
                QUESTION_ID,
                EXPECTED_VERSION))
            .thenThrow(
                authenticationException());

        mockMvc.perform(
            post(CLAIM_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "version": 2
                    }
                    """))
            .andExpect(
                status().isUnauthorized())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "AUTHENTICATION_REQUIRED"));
    }

    @Test
    void claimQuestion_nonOrgCaller_shouldReturn403()
        throws Exception {

        when(organizationQuestionService
            .claimQuestion(
                QUESTION_ID,
                EXPECTED_VERSION))
            .thenThrow(
                authorizationException());

        mockMvc.perform(
            post(CLAIM_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "version": 2
                    }
                    """))
            .andExpect(
                status().isForbidden())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "ACCESS_DENIED"));
    }

    @Test
    void claimQuestion_missingOrInaccessibleQuestion_shouldReturn404()
        throws Exception {

        when(organizationQuestionService
            .claimQuestion(
                QUESTION_ID,
                EXPECTED_VERSION))
            .thenThrow(
                new QuestionNotFoundException(
                    QUESTION_ID));

        mockMvc.perform(
            post(CLAIM_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "version": 2
                    }
                    """))
            .andExpect(
                status().isNotFound())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "QUESTION_NOT_FOUND"));
    }

    @Test
    void claimQuestion_alreadyClaimed_shouldReturn409()
        throws Exception {

        when(organizationQuestionService
            .claimQuestion(
                QUESTION_ID,
                EXPECTED_VERSION))
            .thenThrow(
                new QuestionAlreadyClaimedException(
                    QUESTION_ID));

        mockMvc.perform(
            post(CLAIM_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "version": 2
                    }
                    """))
            .andExpect(
                status().isConflict())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "QUESTION_ALREADY_CLAIMED"));
    }

    @Test
    void getAssignedToCurrentResponder_invalidStatus_shouldReturn400WithoutService()
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
            organizationQuestionService);
    }

    @Test
    void getResponderInbox_negativePage_shouldReturn400WithoutService()
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
            organizationQuestionService);
    }

    @Test
    void getAssignedToCurrentResponder_zeroSize_shouldReturn400WithoutService()
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
            organizationQuestionService);
    }

    @Test
    void getResponderInbox_sizeAboveMaximum_shouldReturn400WithoutService()
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
            organizationQuestionService);
    }

    @Test
    void getResponderInbox_nonnumericPage_shouldReturn400WithoutService()
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
            organizationQuestionService);
    }

    @Test
    void getAssignedToCurrentResponder_nonnumericSize_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            get(ASSIGNED_URL)
                .param(
                    "size",
                    "invalid"))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(
            organizationQuestionService);
    }

    @Test
    void getResponderInbox_unauthenticated_shouldReturn401()
        throws Exception {

        when(organizationQuestionService
            .getResponderInbox(
                DEFAULT_PAGE,
                DEFAULT_SIZE))
            .thenThrow(
                authenticationException());

        mockMvc.perform(
            get(INBOX_URL))
            .andExpect(status().isUnauthorized())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "AUTHENTICATION_REQUIRED"));
    }

    @Test
    void getResponderInbox_nonOrgCaller_shouldReturn403()
        throws Exception {

        when(organizationQuestionService
            .getResponderInbox(
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
    void getAssignedToCurrentResponder_unauthenticated_shouldReturn401()
        throws Exception {

        when(organizationQuestionService
            .getAssignedToCurrentResponder(
                isNull(),
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
    void getAssignedToCurrentResponder_adminWithoutOrg_shouldReturn403()
        throws Exception {

        when(organizationQuestionService
            .getAssignedToCurrentResponder(
                IN_REVIEW,
                DEFAULT_PAGE,
                DEFAULT_SIZE))
            .thenThrow(
                authorizationException());

        mockMvc.perform(
            get(ASSIGNED_URL)
                .param(
                    "status",
                    "IN_REVIEW"))
            .andExpect(status().isForbidden())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "ACCESS_DENIED"));
    }

    private QuestionThreadResponseDTO createClaimedResponse() {

        return new QuestionThreadResponseDTO(
            QUESTION_ID,
            TASK_ASSIGNMENT_ID,
            AUTHOR_ID,
            RESPONDER_ID,
            "Question title",
            "Question content",
            IN_REVIEW,
            PRIVATE,
            OPEN,
            EXPECTED_VERSION + 1,
            CREATED_AT,
            UPDATED_AT);
    }

    private QuestionReviewInboxItemResponseDTO createResponse(
        Long assignedReviewerId,
        com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus status) {

        return new QuestionReviewInboxItemResponseDTO(
            QUESTION_ID,
            TASK_ASSIGNMENT_ID,
            AUTHOR_ID,
            assignedReviewerId,
            "Question title",
            status,
            OPEN,
            PRIVATE,
            2L,
            CREATED_AT,
            UPDATED_AT);
    }

    private AuthenticationException authenticationException() {

        return new AuthenticationException(
            "Authentication is required to access "
                + "organizing committee question queues",
            ErrorCode.AUTHENTICATION_REQUIRED);
    }

    private AuthorizationException authorizationException() {

        return new AuthorizationException(
            "Global ORG role is required to access "
                + "organizing committee question queues",
            ErrorCode.ACCESS_DENIED);
    }
}