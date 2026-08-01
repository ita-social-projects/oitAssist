package com.itasocialacademy.oitassist.chat.controller;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType.OFFICIAL_ANSWER;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.CLOSED;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.chat.dao.dto.request.CreateOfficialAnswerRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.exceptions.InvalidQuestionStateException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionVersionConflictException;
import com.itasocialacademy.oitassist.chat.service.interfaces.AdministratorQuestionService;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class AdministratorOfficialAnswerControllerTest
    extends ControllerUnitTest<AdministratorQuestionController> {

    private static final String OFFICIAL_ANSWER_URL =
        "/api/v1/admin/questions/{questionId}/official-answers";

    private static final Long QUESTION_ID = 10L;
    private static final Long ADMINISTRATOR_ID = 20L;
    private static final Long MESSAGE_ID = 30L;

    private static final String CONTENT =
        "The memory limit includes the input and output buffers.";

    private static final Instant CREATED_AT =
        Instant.parse("2026-08-01T10:00:00Z");

    @Mock
    private AdministratorQuestionService administratorQuestionService;

    @InjectMocks
    private AdministratorQuestionController administratorQuestionController;

    @Override
    protected AdministratorQuestionController getController() {
        return administratorQuestionController;
    }

    @Test
    void publishOfficialAnswer_validRequest_shouldReturn201()
        throws Exception {

        when(administratorQuestionService.publishOfficialAnswer(
            QUESTION_ID,
            new CreateOfficialAnswerRequestDTO(CONTENT)))
            .thenReturn(response());

        mockMvc.perform(
            post(OFFICIAL_ANSWER_URL, QUESTION_ID)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "content":
                        "The memory limit includes the input and output buffers."
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(
                jsonPath("$.id")
                    .value(MESSAGE_ID))
            .andExpect(
                jsonPath("$.questionThreadId")
                    .value(QUESTION_ID))
            .andExpect(
                jsonPath("$.authorId")
                    .value(ADMINISTRATOR_ID))
            .andExpect(
                jsonPath("$.type")
                    .value("OFFICIAL_ANSWER"))
            .andExpect(
                jsonPath("$.content")
                    .value(CONTENT));

        verify(administratorQuestionService)
            .publishOfficialAnswer(
                QUESTION_ID,
                new CreateOfficialAnswerRequestDTO(CONTENT));
    }

    @Test
    void publishOfficialAnswer_protectedFields_shouldNotInfluenceRequest()
        throws Exception {

        when(administratorQuestionService.publishOfficialAnswer(
            QUESTION_ID,
            new CreateOfficialAnswerRequestDTO(CONTENT)))
            .thenReturn(response());

        mockMvc.perform(
            post(OFFICIAL_ANSWER_URL, QUESTION_ID)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "content":
                        "The memory limit includes the input and output buffers.",
                      "id": 999,
                      "questionThreadId": 999,
                      "authorId": 999,
                      "type": "COMMENT",
                      "createdAt": "2020-01-01T00:00:00Z"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(
                jsonPath("$.authorId")
                    .value(ADMINISTRATOR_ID))
            .andExpect(
                jsonPath("$.type")
                    .value("OFFICIAL_ANSWER"));

        verify(administratorQuestionService)
            .publishOfficialAnswer(
                QUESTION_ID,
                new CreateOfficialAnswerRequestDTO(CONTENT));
    }

    @Test
    void publishOfficialAnswer_missingBody_shouldReturn400()
        throws Exception {

        mockMvc.perform(
            post(OFFICIAL_ANSWER_URL, QUESTION_ID)
                .contentType(APPLICATION_JSON))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(
            administratorQuestionService);
    }

    @Test
    void publishOfficialAnswer_missingContent_shouldReturn400()
        throws Exception {

        mockMvc.perform(
            post(OFFICIAL_ANSWER_URL, QUESTION_ID)
                .contentType(APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(
            administratorQuestionService);
    }

    @Test
    void publishOfficialAnswer_nullContent_shouldReturn400()
        throws Exception {

        mockMvc.perform(
            post(OFFICIAL_ANSWER_URL, QUESTION_ID)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "content": null
                    }
                    """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(
            administratorQuestionService);
    }

    @Test
    void publishOfficialAnswer_blankContent_shouldReturn400()
        throws Exception {

        mockMvc.perform(
            post(OFFICIAL_ANSWER_URL, QUESTION_ID)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "content": "   "
                    }
                    """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(
            administratorQuestionService);
    }

    @Test
    void publishOfficialAnswer_oversizedContent_shouldReturn400()
        throws Exception {

        String oversizedContent =
            "a".repeat(10_001);

        mockMvc.perform(
            post(OFFICIAL_ANSWER_URL, QUESTION_ID)
                .contentType(APPLICATION_JSON)
                .content(
                    """
                        {"content":"%s"}
                        """.formatted(oversizedContent)))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(
            administratorQuestionService);
    }

    @Test
    void publishOfficialAnswer_malformedBody_shouldReturn400()
        throws Exception {

        mockMvc.perform(
            post(OFFICIAL_ANSWER_URL, QUESTION_ID)
                .contentType(APPLICATION_JSON)
                .content("{"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(
            administratorQuestionService);
    }

    @Test
    void publishOfficialAnswer_zeroQuestionId_shouldReturn400()
        throws Exception {

        performValidRequest(0L)
            .andExpect(status().isBadRequest());

        verifyNoInteractions(
            administratorQuestionService);
    }

    @Test
    void publishOfficialAnswer_negativeQuestionId_shouldReturn400()
        throws Exception {

        performValidRequest(-1L)
            .andExpect(status().isBadRequest());

        verifyNoInteractions(
            administratorQuestionService);
    }

    @Test
    void publishOfficialAnswer_nonnumericQuestionId_shouldReturn400()
        throws Exception {

        performValidRequest("invalid")
            .andExpect(status().isBadRequest());

        verifyNoInteractions(
            administratorQuestionService);
    }

    @Test
    void publishOfficialAnswer_unauthenticated_shouldReturn401()
        throws Exception {

        when(administratorQuestionService.publishOfficialAnswer(
            QUESTION_ID,
            new CreateOfficialAnswerRequestDTO(CONTENT)))
            .thenThrow(new AuthenticationException(
                "Authentication is required",
                ErrorCode.AUTHENTICATION_REQUIRED));

        performValidRequest(QUESTION_ID)
            .andExpect(status().isUnauthorized())
            .andExpect(
                jsonPath("$.code")
                    .value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void publishOfficialAnswer_nonAdministrator_shouldReturn403()
        throws Exception {

        when(administratorQuestionService.publishOfficialAnswer(
            QUESTION_ID,
            new CreateOfficialAnswerRequestDTO(CONTENT)))
            .thenThrow(new AuthorizationException(
                "Administrator role is required",
                ErrorCode.ACCESS_DENIED));

        performValidRequest(QUESTION_ID)
            .andExpect(status().isForbidden())
            .andExpect(
                jsonPath("$.code")
                    .value("ACCESS_DENIED"));
    }

    @Test
    void publishOfficialAnswer_missingQuestion_shouldReturn404()
        throws Exception {

        when(administratorQuestionService.publishOfficialAnswer(
            QUESTION_ID,
            new CreateOfficialAnswerRequestDTO(CONTENT)))
            .thenThrow(new QuestionNotFoundException(
                QUESTION_ID));

        performValidRequest(QUESTION_ID)
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.code")
                    .value("QUESTION_NOT_FOUND"));
    }

    @Test
    void publishOfficialAnswer_closedQuestion_shouldReturn409()
        throws Exception {

        when(administratorQuestionService.publishOfficialAnswer(
            QUESTION_ID,
            new CreateOfficialAnswerRequestDTO(CONTENT)))
            .thenThrow(new InvalidQuestionStateException(
                QUESTION_ID,
                CLOSED,
                "publish official answer"));

        performValidRequest(QUESTION_ID)
            .andExpect(status().isConflict())
            .andExpect(
                jsonPath("$.code")
                    .value("QUESTION_INVALID_STATE"));
    }

    @Test
    void publishOfficialAnswer_concurrentLifecycleConflict_shouldReturn409()
        throws Exception {

        when(administratorQuestionService.publishOfficialAnswer(
            QUESTION_ID,
            new CreateOfficialAnswerRequestDTO(CONTENT)))
            .thenThrow(new QuestionVersionConflictException(
                QUESTION_ID));

        performValidRequest(QUESTION_ID)
            .andExpect(status().isConflict())
            .andExpect(
                jsonPath("$.code")
                    .value("QUESTION_VERSION_CONFLICT"));
    }

    private org.springframework.test.web.servlet.ResultActions performValidRequest(Object questionId)
        throws Exception {

        return mockMvc.perform(
            post(OFFICIAL_ANSWER_URL, questionId)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "content":
                        "The memory limit includes the input and output buffers."
                    }
                    """));
    }

    private QuestionMessageResponseDTO response() {
        return new QuestionMessageResponseDTO(
            MESSAGE_ID,
            QUESTION_ID,
            ADMINISTRATOR_ID,
            OFFICIAL_ANSWER,
            CONTENT,
            CREATED_AT);
    }
}