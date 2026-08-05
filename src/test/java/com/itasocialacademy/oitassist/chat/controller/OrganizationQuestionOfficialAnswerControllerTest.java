package com.itasocialacademy.oitassist.chat.controller;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType.OFFICIAL_ANSWER;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.CLOSED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.chat.dao.dto.request.CreateOfficialAnswerRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.exceptions.InvalidQuestionStateException;
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

class OrganizationQuestionOfficialAnswerControllerTest
    extends ControllerUnitTest<OrganizationQuestionController> {

    private static final Long QUESTION_ID =
        10L;

    private static final Long RESPONDER_ID =
        40L;

    private static final Long MESSAGE_ID =
        50L;

    private static final String ANSWER_CONTENT =
        "The time limit is measured independently for each test.";

    private static final String OFFICIAL_ANSWER_URL =
        "/api/v1/org/questions/"
            + QUESTION_ID
            + "/official-answers";

    private static final Instant CREATED_AT =
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
    void requestContract_shouldContainOnlyContent() {

        List<String> componentNames =
            Arrays.stream(
                CreateOfficialAnswerRequestDTO.class
                    .getRecordComponents())
                .map(component -> component.getName())
                .toList();

        assertEquals(
            List.of(
                "content"),
            componentNames);
    }

    @Test
    void publishOfficialAnswer_validRequest_shouldReturnCreatedMessage()
        throws Exception {

        QuestionMessageResponseDTO response =
            createResponse();

        when(organizationQuestionService
            .publishOfficialAnswer(
                eq(QUESTION_ID),
                any(CreateOfficialAnswerRequestDTO.class)))
            .thenReturn(
                response);

        mockMvc.perform(
            post(
                OFFICIAL_ANSWER_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "The time limit is measured independently for each test."
                    }
                    """))
            .andExpect(
                status().isCreated())
            .andExpect(
                content().contentTypeCompatibleWith(
                    MediaType.APPLICATION_JSON))
            .andExpect(
                jsonPath("$.id")
                    .value(
                        MESSAGE_ID))
            .andExpect(
                jsonPath("$.questionThreadId")
                    .value(
                        QUESTION_ID))
            .andExpect(
                jsonPath("$.authorId")
                    .value(
                        RESPONDER_ID))
            .andExpect(
                jsonPath("$.type")
                    .value(
                        "OFFICIAL_ANSWER"))
            .andExpect(
                jsonPath("$.content")
                    .value(
                        ANSWER_CONTENT))
            .andExpect(
                jsonPath("$.createdAt")
                    .value(
                        CREATED_AT.toString()));

        ArgumentCaptor<CreateOfficialAnswerRequestDTO> requestCaptor =
            ArgumentCaptor.forClass(
                CreateOfficialAnswerRequestDTO.class);

        verify(organizationQuestionService)
            .publishOfficialAnswer(
                eq(QUESTION_ID),
                requestCaptor.capture());

        assertEquals(
            ANSWER_CONTENT,
            requestCaptor.getValue()
                .content());
    }

    @Test
    void publishOfficialAnswer_nonPositiveQuestionId_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            post(
                "/api/v1/org/questions/0/official-answers")
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "Official answer"
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
    void publishOfficialAnswer_nonnumericQuestionId_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            post(
                "/api/v1/org/questions/invalid/official-answers")
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "Official answer"
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
    void publishOfficialAnswer_missingBody_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            post(
                OFFICIAL_ANSWER_URL)
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
    void publishOfficialAnswer_missingContent_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            post(
                OFFICIAL_ANSWER_URL)
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
    void publishOfficialAnswer_blankContent_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            post(
                OFFICIAL_ANSWER_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "content": "   "
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
    void publishOfficialAnswer_overlongContent_shouldReturn400WithoutService()
        throws Exception {

        String requestBody =
            objectMapper.writeValueAsString(
                new CreateOfficialAnswerRequestDTO(
                    "a".repeat(
                        10_001)));

        mockMvc.perform(
            post(
                OFFICIAL_ANSWER_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content(
                    requestBody))
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
    void publishOfficialAnswer_unauthenticated_shouldReturn401()
        throws Exception {

        when(organizationQuestionService
            .publishOfficialAnswer(
                eq(QUESTION_ID),
                any(CreateOfficialAnswerRequestDTO.class)))
            .thenThrow(
                authenticationException());

        mockMvc.perform(
            post(
                OFFICIAL_ANSWER_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content(
                    validRequestBody()))
            .andExpect(
                status().isUnauthorized())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "AUTHENTICATION_REQUIRED"));
    }

    @Test
    void publishOfficialAnswer_nonOrgCaller_shouldReturn403()
        throws Exception {

        when(organizationQuestionService
            .publishOfficialAnswer(
                eq(QUESTION_ID),
                any(CreateOfficialAnswerRequestDTO.class)))
            .thenThrow(
                authorizationException());

        mockMvc.perform(
            post(
                OFFICIAL_ANSWER_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content(
                    validRequestBody()))
            .andExpect(
                status().isForbidden())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "ACCESS_DENIED"));
    }

    @Test
    void publishOfficialAnswer_missingOrInaccessibleQuestion_shouldReturn404()
        throws Exception {

        when(organizationQuestionService
            .publishOfficialAnswer(
                eq(QUESTION_ID),
                any(CreateOfficialAnswerRequestDTO.class)))
            .thenThrow(
                new QuestionNotFoundException(
                    QUESTION_ID));

        mockMvc.perform(
            post(
                OFFICIAL_ANSWER_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content(
                    validRequestBody()))
            .andExpect(
                status().isNotFound())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "QUESTION_NOT_FOUND"))
            .andExpect(
                jsonPath("$.questionThreadId")
                    .doesNotExist())
            .andExpect(
                jsonPath("$.authorId")
                    .doesNotExist())
            .andExpect(
                jsonPath("$.type")
                    .doesNotExist())
            .andExpect(
                jsonPath("$.content")
                    .doesNotExist());
    }

    @Test
    void publishOfficialAnswer_closedQuestion_shouldReturn409()
        throws Exception {

        when(organizationQuestionService
            .publishOfficialAnswer(
                eq(QUESTION_ID),
                any(CreateOfficialAnswerRequestDTO.class)))
            .thenThrow(
                new InvalidQuestionStateException(
                    QUESTION_ID,
                    CLOSED,
                    "publish official answer"));

        mockMvc.perform(
            post(
                OFFICIAL_ANSWER_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content(
                    validRequestBody()))
            .andExpect(
                status().isConflict())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "QUESTION_INVALID_STATE"));
    }

    @Test
    void publishOfficialAnswer_concurrentLifecycleConflict_shouldReturn409()
        throws Exception {

        when(organizationQuestionService
            .publishOfficialAnswer(
                eq(QUESTION_ID),
                any(CreateOfficialAnswerRequestDTO.class)))
            .thenThrow(
                new QuestionVersionConflictException(
                    QUESTION_ID));

        mockMvc.perform(
            post(
                OFFICIAL_ANSWER_URL)
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content(
                    validRequestBody()))
            .andExpect(
                status().isConflict())
            .andExpect(
                jsonPath("$.code")
                    .value(
                        "QUESTION_VERSION_CONFLICT"));
    }

    private QuestionMessageResponseDTO createResponse() {

        return new QuestionMessageResponseDTO(
            MESSAGE_ID,
            QUESTION_ID,
            RESPONDER_ID,
            OFFICIAL_ANSWER,
            ANSWER_CONTENT,
            CREATED_AT);
    }

    private String validRequestBody() {

        return """
            {
              "content": "The time limit is measured independently for each test."
            }
            """;
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