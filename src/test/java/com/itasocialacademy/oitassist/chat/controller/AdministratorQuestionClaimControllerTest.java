package com.itasocialacademy.oitassist.chat.controller;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.CLOSED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.ANSWERED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.IN_REVIEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.exceptions.InvalidQuestionStateException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionAlreadyClaimedException;
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

class AdministratorQuestionClaimControllerTest
    extends ControllerUnitTest<AdministratorQuestionController> {

    private static final String CLAIM_URL =
        "/api/v1/admin/questions/{questionId}/claim";

    private static final Long QUESTION_ID = 10L;
    private static final Long ADMINISTRATOR_ID = 20L;
    private static final Long EXPECTED_VERSION = 3L;

    @Mock
    private AdministratorQuestionService administratorQuestionService;

    @InjectMocks
    private AdministratorQuestionController administratorQuestionController;

    @Override
    protected AdministratorQuestionController getController() {

        return administratorQuestionController;
    }

    @Test
    void claimQuestion_validRequest_shouldReturnUpdatedQuestion()
        throws Exception {

        when(administratorQuestionService.claimQuestion(
            QUESTION_ID,
            EXPECTED_VERSION))
            .thenReturn(claimedResponse());

        mockMvc.perform(
            post(CLAIM_URL, QUESTION_ID)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "version": 3
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.id")
                    .value(QUESTION_ID))
            .andExpect(
                jsonPath("$.assignedReviewerId")
                    .value(ADMINISTRATOR_ID))
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
                    .value(EXPECTED_VERSION + 1));

        verify(administratorQuestionService)
            .claimQuestion(
                QUESTION_ID,
                EXPECTED_VERSION);
    }

    @Test
    void claimQuestion_protectedFields_shouldNotInfluenceDelegation()
        throws Exception {

        when(administratorQuestionService.claimQuestion(
            QUESTION_ID,
            EXPECTED_VERSION))
            .thenReturn(claimedResponse());

        mockMvc.perform(
            post(CLAIM_URL, QUESTION_ID)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "version": 3,
                      "administratorId": 999,
                      "assignedReviewerId": 999,
                      "status": "ANSWERED",
                      "state": "CLOSED",
                      "visibility": "PUBLIC"
                    }
                    """))
            .andExpect(status().isOk());

        verify(administratorQuestionService)
            .claimQuestion(
                QUESTION_ID,
                EXPECTED_VERSION);
    }

    @Test
    void claimQuestion_zeroVersion_shouldBeAccepted()
        throws Exception {

        when(administratorQuestionService.claimQuestion(
            QUESTION_ID,
            0L))
            .thenReturn(claimedResponse());

        mockMvc.perform(
            post(CLAIM_URL, QUESTION_ID)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "version": 0
                    }
                    """))
            .andExpect(status().isOk());

        verify(administratorQuestionService)
            .claimQuestion(
                QUESTION_ID,
                0L);
    }

    @Test
    void claimQuestion_missingBody_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            post(CLAIM_URL, QUESTION_ID)
                .contentType(APPLICATION_JSON))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(
            administratorQuestionService);
    }

    @Test
    void claimQuestion_malformedBody_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            post(CLAIM_URL, QUESTION_ID)
                .contentType(APPLICATION_JSON)
                .content("{"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(
            administratorQuestionService);
    }

    @Test
    void claimQuestion_missingVersion_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            post(CLAIM_URL, QUESTION_ID)
                .contentType(APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(
            administratorQuestionService);
    }

    @Test
    void claimQuestion_nullVersion_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            post(CLAIM_URL, QUESTION_ID)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "version": null
                    }
                    """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(
            administratorQuestionService);
    }

    @Test
    void claimQuestion_negativeVersion_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            post(CLAIM_URL, QUESTION_ID)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "version": -1
                    }
                    """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(
            administratorQuestionService);
    }

    @Test
    void claimQuestion_nonnumericQuestionId_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            post(CLAIM_URL, "invalid")
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "version": 3
                    }
                    """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(
            administratorQuestionService);
    }

    @Test
    void claimQuestion_zeroQuestionId_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            post(CLAIM_URL, 0)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "version": 3
                    }
                    """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(
            administratorQuestionService);
    }

    @Test
    void claimQuestion_negativeQuestionId_shouldReturn400WithoutService()
        throws Exception {

        mockMvc.perform(
            post(CLAIM_URL, -1)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "version": 3
                    }
                    """))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(
            administratorQuestionService);
    }

    @Test
    void claimQuestion_unauthenticated_shouldReturn401()
        throws Exception {

        when(administratorQuestionService.claimQuestion(
            QUESTION_ID,
            EXPECTED_VERSION))
            .thenThrow(
                new AuthenticationException(
                    "Authentication is required",
                    ErrorCode.AUTHENTICATION_REQUIRED));

        performValidClaim()
            .andExpect(status().isUnauthorized())
            .andExpect(
                jsonPath("$.code")
                    .value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void claimQuestion_nonAdministrator_shouldReturn403()
        throws Exception {

        when(administratorQuestionService.claimQuestion(
            QUESTION_ID,
            EXPECTED_VERSION))
            .thenThrow(
                new AuthorizationException(
                    "Administrator role is required",
                    ErrorCode.ACCESS_DENIED));

        performValidClaim()
            .andExpect(status().isForbidden())
            .andExpect(
                jsonPath("$.code")
                    .value("ACCESS_DENIED"));
    }

    @Test
    void claimQuestion_missingQuestion_shouldReturn404()
        throws Exception {

        when(administratorQuestionService.claimQuestion(
            QUESTION_ID,
            EXPECTED_VERSION))
            .thenThrow(
                new QuestionNotFoundException(
                    QUESTION_ID));

        performValidClaim()
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.code")
                    .value("QUESTION_NOT_FOUND"));
    }

    @Test
    void claimQuestion_alreadyClaimed_shouldReturn409()
        throws Exception {

        when(administratorQuestionService.claimQuestion(
            QUESTION_ID,
            EXPECTED_VERSION))
            .thenThrow(
                new QuestionAlreadyClaimedException(
                    QUESTION_ID));

        performValidClaim()
            .andExpect(status().isConflict())
            .andExpect(
                jsonPath("$.code")
                    .value("QUESTION_ALREADY_CLAIMED"));
    }

    @Test
    void claimQuestion_closedQuestion_shouldReturn409()
        throws Exception {

        when(administratorQuestionService.claimQuestion(
            QUESTION_ID,
            EXPECTED_VERSION))
            .thenThrow(
                new InvalidQuestionStateException(
                    QUESTION_ID,
                    CLOSED,
                    "claim for review"));

        performValidClaim()
            .andExpect(status().isConflict())
            .andExpect(
                jsonPath("$.code")
                    .value("QUESTION_INVALID_STATE"));
    }

    @Test
    void claimQuestion_answeredQuestion_shouldReturn409()
        throws Exception {

        when(administratorQuestionService.claimQuestion(
            QUESTION_ID,
            EXPECTED_VERSION))
            .thenThrow(
                new InvalidQuestionStateException(
                    QUESTION_ID,
                    ANSWERED,
                    "claim for review"));

        performValidClaim()
            .andExpect(status().isConflict())
            .andExpect(
                jsonPath("$.code")
                    .value("QUESTION_INVALID_STATE"));
    }

    @Test
    void claimQuestion_staleVersion_shouldReturn409()
        throws Exception {

        when(administratorQuestionService.claimQuestion(
            QUESTION_ID,
            EXPECTED_VERSION))
            .thenThrow(
                new QuestionVersionConflictException(
                    QUESTION_ID));

        performValidClaim()
            .andExpect(status().isConflict())
            .andExpect(
                jsonPath("$.code")
                    .value("QUESTION_VERSION_CONFLICT"));
    }

    private org.springframework.test.web.servlet.ResultActions performValidClaim()
        throws Exception {

        return mockMvc.perform(
            post(CLAIM_URL, QUESTION_ID)
                .contentType(APPLICATION_JSON)
                .content("""
                    {
                      "version": 3
                    }
                    """));
    }

    private QuestionThreadResponseDTO claimedResponse() {
        return new QuestionThreadResponseDTO(
            QUESTION_ID,
            100L,
            200L,
            ADMINISTRATOR_ID,
            "Question title",
            "Question content",
            IN_REVIEW,
            PRIVATE,
            OPEN,
            EXPECTED_VERSION + 1,
            Instant.parse(
                "2026-08-01T10:00:00Z"),
            Instant.parse(
                "2026-08-01T10:15:00Z"));
    }
}