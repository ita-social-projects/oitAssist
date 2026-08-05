package com.itasocialacademy.oitassist.chat.controller;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.CLOSED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.ANSWERED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.IN_REVIEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PUBLIC;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.chat.dao.dto.request.UpdateQuestionStateRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.request.UpdateQuestionStatusRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.request.UpdateQuestionVisibilityRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionState;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionVersionConflictException;
import com.itasocialacademy.oitassist.chat.service.interfaces.OrganizationQuestionService;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;

class OrganizationQuestionModerationControllerTest
    extends ControllerUnitTest<OrganizationQuestionController> {

    private static final Long QUESTION_ID =
        10L;

    private static final Long TASK_ASSIGNMENT_ID =
        20L;

    private static final Long AUTHOR_ID =
        30L;

    private static final Long RESPONDER_ID =
        40L;

    private static final Long VERSION =
        3L;

    private static final Long UPDATED_VERSION =
        VERSION + 1;

    private static final String VISIBILITY_URL =
        "/api/v1/org/questions/"
            + QUESTION_ID
            + "/visibility";

    private static final String STATUS_URL =
        "/api/v1/org/questions/"
            + QUESTION_ID
            + "/status";

    private static final String STATE_URL =
        "/api/v1/org/questions/"
            + QUESTION_ID
            + "/state";

    private static final Instant CREATED_AT =
        Instant.parse(
            "2026-08-05T10:00:00Z");

    private static final Instant UPDATED_AT =
        Instant.parse(
            "2026-08-05T10:20:00Z");

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

        assertNotNull(
            annotation);

        assertEquals(
            "hasRole('ORG')",
            annotation.value());
    }

    @Test
    void moderationRequestContracts_shouldContainOnlyFieldAndVersion() {

        assertAll(
            () -> assertEquals(
                List.of(
                    "visibility",
                    "version"),
                recordComponentNames(
                    UpdateQuestionVisibilityRequestDTO.class)),
            () -> assertEquals(
                List.of(
                    "status",
                    "version"),
                recordComponentNames(
                    UpdateQuestionStatusRequestDTO.class)),
            () -> assertEquals(
                List.of(
                    "state",
                    "version"),
                recordComponentNames(
                    UpdateQuestionStateRequestDTO.class)));
    }

    @Test
    void updateVisibility_validRequest_shouldReturnUpdatedQuestion()
        throws Exception {

        QuestionThreadResponseDTO response =
            createResponse(
                PUBLIC,
                IN_REVIEW,
                OPEN,
                UPDATED_VERSION);

        when(organizationQuestionService
            .updateVisibility(
                eq(QUESTION_ID),
                any(UpdateQuestionVisibilityRequestDTO.class)))
            .thenReturn(
                response);

        mockMvc.perform(
            patch(
                VISIBILITY_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "visibility": "PUBLIC",
                      "version": 3
                    }
                    """))
            .andExpect(
                status().isOk())
            .andExpect(
                content().contentTypeCompatibleWith(
                    MediaType.APPLICATION_JSON))
            .andExpect(
                jsonPath("$.id")
                    .value(
                        QUESTION_ID))
            .andExpect(
                jsonPath("$.taskAssignmentId")
                    .value(
                        TASK_ASSIGNMENT_ID))
            .andExpect(
                jsonPath("$.assignedReviewerId")
                    .value(
                        RESPONDER_ID))
            .andExpect(
                jsonPath("$.visibility")
                    .value(
                        "PUBLIC"))
            .andExpect(
                jsonPath("$.status")
                    .value(
                        "IN_REVIEW"))
            .andExpect(
                jsonPath("$.state")
                    .value(
                        "OPEN"))
            .andExpect(
                jsonPath("$.version")
                    .value(
                        UPDATED_VERSION));

        ArgumentCaptor<UpdateQuestionVisibilityRequestDTO> requestCaptor =
            ArgumentCaptor.forClass(
                UpdateQuestionVisibilityRequestDTO.class);

        verify(organizationQuestionService)
            .updateVisibility(
                eq(QUESTION_ID),
                requestCaptor.capture());

        assertAll(
            () -> assertEquals(
                PUBLIC,
                requestCaptor.getValue()
                    .visibility()),
            () -> assertEquals(
                VERSION,
                requestCaptor.getValue()
                    .version()));
    }

    @Test
    void updateStatus_validRequest_shouldReturnUpdatedQuestion()
        throws Exception {

        QuestionThreadResponseDTO response =
            createResponse(
                PRIVATE,
                ANSWERED,
                OPEN,
                UPDATED_VERSION);

        when(organizationQuestionService
            .updateStatus(
                eq(QUESTION_ID),
                any(UpdateQuestionStatusRequestDTO.class)))
            .thenReturn(
                response);

        mockMvc.perform(
            patch(
                STATUS_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "ANSWERED",
                      "version": 3
                    }
                    """))
            .andExpect(
                status().isOk())
            .andExpect(
                content().contentTypeCompatibleWith(
                    MediaType.APPLICATION_JSON))
            .andExpect(
                jsonPath("$.id")
                    .value(
                        QUESTION_ID))
            .andExpect(
                jsonPath("$.assignedReviewerId")
                    .value(
                        RESPONDER_ID))
            .andExpect(
                jsonPath("$.visibility")
                    .value(
                        "PRIVATE"))
            .andExpect(
                jsonPath("$.status")
                    .value(
                        "ANSWERED"))
            .andExpect(
                jsonPath("$.state")
                    .value(
                        "OPEN"))
            .andExpect(
                jsonPath("$.version")
                    .value(
                        UPDATED_VERSION));

        ArgumentCaptor<UpdateQuestionStatusRequestDTO> requestCaptor =
            ArgumentCaptor.forClass(
                UpdateQuestionStatusRequestDTO.class);

        verify(organizationQuestionService)
            .updateStatus(
                eq(QUESTION_ID),
                requestCaptor.capture());

        assertAll(
            () -> assertEquals(
                ANSWERED,
                requestCaptor.getValue()
                    .status()),
            () -> assertEquals(
                VERSION,
                requestCaptor.getValue()
                    .version()));
    }

    @Test
    void updateState_validRequest_shouldReturnUpdatedQuestion()
        throws Exception {

        QuestionThreadResponseDTO response =
            createResponse(
                PRIVATE,
                IN_REVIEW,
                CLOSED,
                UPDATED_VERSION);

        when(organizationQuestionService
            .updateState(
                eq(QUESTION_ID),
                any(UpdateQuestionStateRequestDTO.class)))
            .thenReturn(
                response);

        mockMvc.perform(
            patch(
                STATE_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "state": "CLOSED",
                      "version": 3
                    }
                    """))
            .andExpect(
                status().isOk())
            .andExpect(
                content().contentTypeCompatibleWith(
                    MediaType.APPLICATION_JSON))
            .andExpect(
                jsonPath("$.id")
                    .value(
                        QUESTION_ID))
            .andExpect(
                jsonPath("$.assignedReviewerId")
                    .value(
                        RESPONDER_ID))
            .andExpect(
                jsonPath("$.visibility")
                    .value(
                        "PRIVATE"))
            .andExpect(
                jsonPath("$.status")
                    .value(
                        "IN_REVIEW"))
            .andExpect(
                jsonPath("$.state")
                    .value(
                        "CLOSED"))
            .andExpect(
                jsonPath("$.version")
                    .value(
                        UPDATED_VERSION));

        ArgumentCaptor<UpdateQuestionStateRequestDTO> requestCaptor =
            ArgumentCaptor.forClass(
                UpdateQuestionStateRequestDTO.class);

        verify(organizationQuestionService)
            .updateState(
                eq(QUESTION_ID),
                requestCaptor.capture());

        assertAll(
            () -> assertEquals(
                CLOSED,
                requestCaptor.getValue()
                    .state()),
            () -> assertEquals(
                VERSION,
                requestCaptor.getValue()
                    .version()));
    }

    @Test
    void moderation_nonPositiveQuestionId_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            patch(
                "/api/v1/org/questions/0/visibility")
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "visibility": "PUBLIC",
                      "version": 3
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
    void moderation_nonnumericQuestionId_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            patch(
                "/api/v1/org/questions/invalid/status")
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "ANSWERED",
                      "version": 3
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
    void updateVisibility_invalidRequest_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            patch(
                VISIBILITY_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "version": 3
                    }
                    """))
            .andExpect(
                status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "COMMON_VALIDATION_FAILED"));

        mockMvc.perform(
            patch(
                VISIBILITY_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "visibility": "INVALID",
                      "version": 3
                    }
                    """))
            .andExpect(
                status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "COMMON_VALIDATION_FAILED"));

        mockMvc.perform(
            patch(
                VISIBILITY_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "visibility": "PUBLIC",
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
    void updateStatus_invalidRequest_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            patch(
                STATUS_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "version": 3
                    }
                    """))
            .andExpect(
                status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "COMMON_VALIDATION_FAILED"));

        mockMvc.perform(
            patch(
                STATUS_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "INVALID",
                      "version": 3
                    }
                    """))
            .andExpect(
                status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "COMMON_VALIDATION_FAILED"));

        mockMvc.perform(
            patch(
                STATUS_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "ANSWERED"
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
    void updateState_invalidRequest_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            patch(
                STATE_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "version": 3
                    }
                    """))
            .andExpect(
                status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "COMMON_VALIDATION_FAILED"));

        mockMvc.perform(
            patch(
                STATE_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "state": "INVALID",
                      "version": 3
                    }
                    """))
            .andExpect(
                status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "COMMON_VALIDATION_FAILED"));

        mockMvc.perform(
            patch(
                STATE_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "state": "CLOSED",
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
    void moderation_missingBody_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            patch(
                STATE_URL)
                .contentType(
                    MediaType.APPLICATION_JSON))
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
    void updateVisibility_unauthenticated_shouldReturn401()
        throws Exception {

        when(organizationQuestionService
            .updateVisibility(
                eq(QUESTION_ID),
                any(UpdateQuestionVisibilityRequestDTO.class)))
            .thenThrow(
                authenticationException());

        mockMvc.perform(
            patch(
                VISIBILITY_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "visibility": "PUBLIC",
                      "version": 3
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
    void updateStatus_nonOrgCaller_shouldReturn403()
        throws Exception {

        when(organizationQuestionService
            .updateStatus(
                eq(QUESTION_ID),
                any(UpdateQuestionStatusRequestDTO.class)))
            .thenThrow(
                authorizationException());

        mockMvc.perform(
            patch(
                STATUS_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "ANSWERED",
                      "version": 3
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
    void updateVisibility_inaccessibleQuestion_shouldReturn404()
        throws Exception {

        when(organizationQuestionService
            .updateVisibility(
                eq(QUESTION_ID),
                any(UpdateQuestionVisibilityRequestDTO.class)))
            .thenThrow(
                new QuestionNotFoundException(
                    QUESTION_ID));

        mockMvc.perform(
            patch(
                VISIBILITY_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "visibility": "PUBLIC",
                      "version": 3
                    }
                    """))
            .andExpect(
                status().isNotFound())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "QUESTION_NOT_FOUND"))
            .andExpect(
                jsonPath("$.assignedReviewerId")
                    .doesNotExist())
            .andExpect(
                jsonPath("$.content")
                    .doesNotExist());
    }

    @Test
    void updateStatus_inaccessibleQuestion_shouldReturn404()
        throws Exception {

        when(organizationQuestionService
            .updateStatus(
                eq(QUESTION_ID),
                any(UpdateQuestionStatusRequestDTO.class)))
            .thenThrow(
                new QuestionNotFoundException(
                    QUESTION_ID));

        mockMvc.perform(
            patch(
                STATUS_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "ANSWERED",
                      "version": 3
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
    void updateState_inaccessibleQuestion_shouldReturn404()
        throws Exception {

        when(organizationQuestionService
            .updateState(
                eq(QUESTION_ID),
                any(UpdateQuestionStateRequestDTO.class)))
            .thenThrow(
                new QuestionNotFoundException(
                    QUESTION_ID));

        mockMvc.perform(
            patch(
                STATE_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "state": "CLOSED",
                      "version": 3
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
    void updateVisibility_staleVersion_shouldReturn409()
        throws Exception {

        when(organizationQuestionService
            .updateVisibility(
                eq(QUESTION_ID),
                any(UpdateQuestionVisibilityRequestDTO.class)))
            .thenThrow(
                new QuestionVersionConflictException(
                    QUESTION_ID));

        mockMvc.perform(
            patch(
                VISIBILITY_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "visibility": "PUBLIC",
                      "version": 3
                    }
                    """))
            .andExpect(
                status().isConflict())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "QUESTION_VERSION_CONFLICT"));
    }

    @Test
    void updateStatus_staleVersion_shouldReturn409()
        throws Exception {

        when(organizationQuestionService
            .updateStatus(
                eq(QUESTION_ID),
                any(UpdateQuestionStatusRequestDTO.class)))
            .thenThrow(
                new QuestionVersionConflictException(
                    QUESTION_ID));

        mockMvc.perform(
            patch(
                STATUS_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "status": "ANSWERED",
                      "version": 3
                    }
                    """))
            .andExpect(
                status().isConflict())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "QUESTION_VERSION_CONFLICT"));
    }

    @Test
    void updateState_staleVersion_shouldReturn409()
        throws Exception {

        when(organizationQuestionService
            .updateState(
                eq(QUESTION_ID),
                any(UpdateQuestionStateRequestDTO.class)))
            .thenThrow(
                new QuestionVersionConflictException(
                    QUESTION_ID));

        mockMvc.perform(
            patch(
                STATE_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "state": "CLOSED",
                      "version": 3
                    }
                    """))
            .andExpect(
                status().isConflict())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "QUESTION_VERSION_CONFLICT"));
    }

    private List<String> recordComponentNames(
        Class<? extends Record> recordType) {

        return Arrays.stream(
            recordType.getRecordComponents())
            .map(component -> component.getName())
            .toList();
    }

    private QuestionThreadResponseDTO createResponse(
        QuestionVisibility visibility,
        QuestionStatus status,
        QuestionState state,
        Long version) {

        return new QuestionThreadResponseDTO(
            QUESTION_ID,
            TASK_ASSIGNMENT_ID,
            AUTHOR_ID,
            RESPONDER_ID,
            "Question title",
            "Question content",
            status,
            visibility,
            state,
            version,
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