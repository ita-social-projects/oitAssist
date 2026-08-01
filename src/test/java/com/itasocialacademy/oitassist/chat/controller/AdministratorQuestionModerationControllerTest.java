package com.itasocialacademy.oitassist.chat.controller;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.CLOSED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.ANSWERED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.IN_REVIEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PUBLIC;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.itasocialacademy.oitassist.chat.service.interfaces.AdministratorQuestionService;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class AdministratorQuestionModerationControllerTest
    extends ControllerUnitTest<AdministratorQuestionController> {

    private static final String VISIBILITY_URL =
        "/api/v1/admin/questions/{questionId}/visibility";

    private static final String STATUS_URL =
        "/api/v1/admin/questions/{questionId}/status";

    private static final String STATE_URL =
        "/api/v1/admin/questions/{questionId}/state";

    private static final Long QUESTION_ID = 10L;
    private static final Long VERSION = 3L;

    private static final Instant CREATED_AT =
        Instant.parse("2026-08-01T10:00:00Z");

    private static final Instant UPDATED_AT =
        Instant.parse("2026-08-01T11:00:00Z");

    @Mock
    private AdministratorQuestionService administratorQuestionService;

    @InjectMocks
    private AdministratorQuestionController administratorQuestionController;

    @Override
    protected AdministratorQuestionController getController() {
        return administratorQuestionController;
    }

    @ParameterizedTest
    @EnumSource(QuestionVisibility.class)
    void updateVisibility_supportedValue_shouldReturn200(
        QuestionVisibility visibility)
        throws Exception {

        UpdateQuestionVisibilityRequestDTO request =
            new UpdateQuestionVisibilityRequestDTO(
                visibility,
                VERSION);

        when(administratorQuestionService
            .updateVisibility(
                QUESTION_ID,
                request))
            .thenReturn(response(
                visibility,
                ANSWERED,
                OPEN));

        mockMvc.perform(
            patch(VISIBILITY_URL, QUESTION_ID)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "visibility": "%s",
                      "version": 3
                    }
                    """.formatted(visibility)))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.visibility")
                    .value(visibility.name()))
            .andExpect(
                jsonPath("$.status")
                    .value("ANSWERED"))
            .andExpect(
                jsonPath("$.state")
                    .value("OPEN"))
            .andExpect(
                jsonPath("$.version")
                    .value(4));

        verify(administratorQuestionService)
            .updateVisibility(
                QUESTION_ID,
                request);
    }

    @ParameterizedTest
    @EnumSource(QuestionStatus.class)
    void updateStatus_supportedValue_shouldReturn200(
        QuestionStatus questionStatus)
        throws Exception {

        UpdateQuestionStatusRequestDTO request =
            new UpdateQuestionStatusRequestDTO(
                questionStatus,
                VERSION);

        when(administratorQuestionService
            .updateStatus(
                QUESTION_ID,
                request))
            .thenReturn(response(
                PRIVATE,
                questionStatus,
                CLOSED));

        mockMvc.perform(
            patch(STATUS_URL, QUESTION_ID)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "status": "%s",
                      "version": 3
                    }
                    """.formatted(questionStatus)))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.status")
                    .value(questionStatus.name()))
            .andExpect(
                jsonPath("$.visibility")
                    .value("PRIVATE"))
            .andExpect(
                jsonPath("$.state")
                    .value("CLOSED"));

        verify(administratorQuestionService)
            .updateStatus(
                QUESTION_ID,
                request);
    }

    @ParameterizedTest
    @EnumSource(QuestionState.class)
    void updateState_supportedValue_shouldReturn200(
        QuestionState questionState)
        throws Exception {

        UpdateQuestionStateRequestDTO request =
            new UpdateQuestionStateRequestDTO(
                questionState,
                VERSION);

        when(administratorQuestionService
            .updateState(
                QUESTION_ID,
                request))
            .thenReturn(response(
                PUBLIC,
                IN_REVIEW,
                questionState));

        mockMvc.perform(
            patch(STATE_URL, QUESTION_ID)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "state": "%s",
                      "version": 3
                    }
                    """.formatted(questionState)))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.state")
                    .value(questionState.name()))
            .andExpect(
                jsonPath("$.visibility")
                    .value("PUBLIC"))
            .andExpect(
                jsonPath("$.status")
                    .value("IN_REVIEW"));

        verify(administratorQuestionService)
            .updateState(
                QUESTION_ID,
                request);
    }

    @ParameterizedTest
    @MethodSource("invalidBodies")
    void moderation_invalidBody_shouldReturn400WithoutDelegation(
        String url,
        String body)
        throws Exception {

        var requestBuilder =
            patch(url, QUESTION_ID)
                .contentType(APPLICATION_JSON);

        if (body != null) {
            requestBuilder.content(body);
        }

        mockMvc.perform(requestBuilder)
            .andExpect(status().isBadRequest());

        verifyNoInteractions(
            administratorQuestionService);
    }

    @ParameterizedTest
    @MethodSource("invalidIds")
    void moderation_invalidQuestionId_shouldReturn400(
        String url,
        Object questionId,
        String validBody)
        throws Exception {

        mockMvc.perform(
            patch(url, questionId)
                .contentType(APPLICATION_JSON)
                .content(validBody))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(
            administratorQuestionService);
    }

    @ParameterizedTest
    @EnumSource(ModerationEndpoint.class)
    void moderation_missingQuestion_shouldReturn404(
        ModerationEndpoint endpoint)
        throws Exception {

        stubFailure(
            endpoint,
            new QuestionNotFoundException(
                QUESTION_ID));

        performValidRequest(endpoint)
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.code")
                    .value("QUESTION_NOT_FOUND"));
    }

    @ParameterizedTest
    @EnumSource(ModerationEndpoint.class)
    void moderation_staleVersion_shouldReturn409(
        ModerationEndpoint endpoint)
        throws Exception {

        stubFailure(
            endpoint,
            new QuestionVersionConflictException(
                QUESTION_ID));

        performValidRequest(endpoint)
            .andExpect(status().isConflict())
            .andExpect(
                jsonPath("$.code")
                    .value("QUESTION_VERSION_CONFLICT"));
    }

    @ParameterizedTest
    @EnumSource(ModerationEndpoint.class)
    void moderation_unauthenticated_shouldReturn401(
        ModerationEndpoint endpoint)
        throws Exception {

        stubFailure(
            endpoint,
            new AuthenticationException(
                "Authentication is required",
                ErrorCode.AUTHENTICATION_REQUIRED));

        performValidRequest(endpoint)
            .andExpect(status().isUnauthorized())
            .andExpect(
                jsonPath("$.code")
                    .value("AUTHENTICATION_REQUIRED"));
    }

    @ParameterizedTest
    @EnumSource(ModerationEndpoint.class)
    void moderation_nonAdministratorIncludingOrg_shouldReturn403(
        ModerationEndpoint endpoint)
        throws Exception {

        stubFailure(
            endpoint,
            new AuthorizationException(
                "Global administrator role is required",
                ErrorCode.ACCESS_DENIED));

        performValidRequest(endpoint)
            .andExpect(status().isForbidden())
            .andExpect(
                jsonPath("$.code")
                    .value("ACCESS_DENIED"));
    }

    private org.springframework.test.web.servlet.ResultActions performValidRequest(
        ModerationEndpoint endpoint)
        throws Exception {

        return switch (endpoint) {
            case VISIBILITY ->
                mockMvc.perform(
                    patch(VISIBILITY_URL, QUESTION_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                            {
                              "visibility": "PUBLIC",
                              "version": 3
                            }
                            """));
            case STATUS ->
                mockMvc.perform(
                    patch(STATUS_URL, QUESTION_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                            {
                              "status": "IN_REVIEW",
                              "version": 3
                            }
                            """));
            case STATE ->
                mockMvc.perform(
                    patch(STATE_URL, QUESTION_ID)
                        .contentType(APPLICATION_JSON)
                        .content("""
                            {
                              "state": "CLOSED",
                              "version": 3
                            }
                            """));
        };
    }

    private void stubFailure(
        ModerationEndpoint endpoint,
        RuntimeException exception) {

        switch (endpoint) {
            case VISIBILITY ->
                when(administratorQuestionService
                    .updateVisibility(
                        QUESTION_ID,
                        new UpdateQuestionVisibilityRequestDTO(
                            PUBLIC,
                            VERSION)))
                    .thenThrow(exception);
            case STATUS ->
                when(administratorQuestionService
                    .updateStatus(
                        QUESTION_ID,
                        new UpdateQuestionStatusRequestDTO(
                            IN_REVIEW,
                            VERSION)))
                    .thenThrow(exception);
            case STATE ->
                when(administratorQuestionService
                    .updateState(
                        QUESTION_ID,
                        new UpdateQuestionStateRequestDTO(
                            CLOSED,
                            VERSION)))
                    .thenThrow(exception);
        }
    }

    private QuestionThreadResponseDTO response(
        QuestionVisibility visibility,
        QuestionStatus questionStatus,
        QuestionState questionState) {

        return new QuestionThreadResponseDTO(
            QUESTION_ID,
            20L,
            30L,
            40L,
            "Question title",
            "Question content",
            questionStatus,
            visibility,
            questionState,
            4L,
            CREATED_AT,
            UPDATED_AT);
    }

    private static Stream<Arguments> invalidBodies() {
        return Stream.of(
            Arguments.of(VISIBILITY_URL, null),
            Arguments.of(VISIBILITY_URL, "{"),
            Arguments.of(VISIBILITY_URL, "{}"),
            Arguments.of(
                VISIBILITY_URL,
                """
                    {"visibility": null, "version": 3}
                    """),
            Arguments.of(
                VISIBILITY_URL,
                """
                    {"visibility": "INVALID", "version": 3}
                    """),
            Arguments.of(
                VISIBILITY_URL,
                """
                    {"visibility": "PUBLIC"}
                    """),
            Arguments.of(
                VISIBILITY_URL,
                """
                    {"visibility": "PUBLIC", "version": null}
                    """),
            Arguments.of(
                VISIBILITY_URL,
                """
                    {"visibility": "PUBLIC", "version": -1}
                    """),

            Arguments.of(STATUS_URL, null),
            Arguments.of(STATUS_URL, "{"),
            Arguments.of(STATUS_URL, "{}"),
            Arguments.of(
                STATUS_URL,
                """
                    {"status": null, "version": 3}
                    """),
            Arguments.of(
                STATUS_URL,
                """
                    {"status": "INVALID", "version": 3}
                    """),
            Arguments.of(
                STATUS_URL,
                """
                    {"status": "IN_REVIEW"}
                    """),
            Arguments.of(
                STATUS_URL,
                """
                    {"status": "IN_REVIEW", "version": null}
                    """),
            Arguments.of(
                STATUS_URL,
                """
                    {"status": "IN_REVIEW", "version": -1}
                    """),

            Arguments.of(STATE_URL, null),
            Arguments.of(STATE_URL, "{"),
            Arguments.of(STATE_URL, "{}"),
            Arguments.of(
                STATE_URL,
                """
                    {"state": null, "version": 3}
                    """),
            Arguments.of(
                STATE_URL,
                """
                    {"state": "INVALID", "version": 3}
                    """),
            Arguments.of(
                STATE_URL,
                """
                    {"state": "CLOSED"}
                    """),
            Arguments.of(
                STATE_URL,
                """
                    {"state": "CLOSED", "version": null}
                    """),
            Arguments.of(
                STATE_URL,
                """
                    {"state": "CLOSED", "version": -1}
                    """));
    }

    private static Stream<Arguments> invalidIds() {
        return Stream.of(
            Arguments.of(
                VISIBILITY_URL,
                0L,
                """
                    {"visibility": "PUBLIC", "version": 3}
                    """),
            Arguments.of(
                VISIBILITY_URL,
                -1L,
                """
                    {"visibility": "PUBLIC", "version": 3}
                    """),
            Arguments.of(
                VISIBILITY_URL,
                "invalid",
                """
                    {"visibility": "PUBLIC", "version": 3}
                    """),
            Arguments.of(
                STATUS_URL,
                0L,
                """
                    {"status": "IN_REVIEW", "version": 3}
                    """),
            Arguments.of(
                STATUS_URL,
                -1L,
                """
                    {"status": "IN_REVIEW", "version": 3}
                    """),
            Arguments.of(
                STATUS_URL,
                "invalid",
                """
                    {"status": "IN_REVIEW", "version": 3}
                    """),
            Arguments.of(
                STATE_URL,
                0L,
                """
                    {"state": "CLOSED", "version": 3}
                    """),
            Arguments.of(
                STATE_URL,
                -1L,
                """
                    {"state": "CLOSED", "version": 3}
                    """),
            Arguments.of(
                STATE_URL,
                "invalid",
                """
                    {"state": "CLOSED", "version": 3}
                    """));
    }

    private enum ModerationEndpoint {
        VISIBILITY,
        STATUS,
        STATE
    }
}