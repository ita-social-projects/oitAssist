package com.itasocialacademy.oitassist.chat.controller;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType.COMMENT;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType.OFFICIAL_ANSWER;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.CLOSED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.ANSWERED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionForumAccessRestrictedException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import com.itasocialacademy.oitassist.chat.service.interfaces.ParticipantQuestionService;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class ParticipantQuestionControllerTest
    extends ControllerUnitTest<ParticipantQuestionController> {

    private static final String QUESTION_URL = "/api/v1/questions/{questionId}";
    private static final String MESSAGES_URL = "/api/v1/questions/{questionId}/messages";

    private static final Long QUESTION_ID = 11L;
    private static final Long TASK_ASSIGNMENT_ID = 1L;
    private static final Long AUTHOR_ID = 100L;
    private static final Long REVIEWER_ID = 200L;

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 50;

    private static final Instant CREATED_AT = Instant.parse("2026-07-24T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-24T11:00:00Z");

    @Mock
    private ParticipantQuestionService participantQuestionService;

    @InjectMocks
    private ParticipantQuestionController participantQuestionController;

    @Override
    protected ParticipantQuestionController getController() {
        return participantQuestionController;
    }

    @Test
    void getQuestionDetails_accessibleQuestion_shouldReturn200WithDetails() throws Exception {
        when(participantQuestionService.getQuestionDetails(QUESTION_ID))
            .thenReturn(createQuestionResponse());

        mockMvc.perform(get(QUESTION_URL, QUESTION_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(QUESTION_ID))
            .andExpect(jsonPath("$.taskAssignmentId").value(TASK_ASSIGNMENT_ID))
            .andExpect(jsonPath("$.authorId").value(AUTHOR_ID))
            .andExpect(jsonPath("$.assignedReviewerId").value(REVIEWER_ID))
            .andExpect(jsonPath("$.title").value("Clarification about input format"))
            .andExpect(jsonPath("$.content").value("May the input contain duplicate values?"))
            .andExpect(jsonPath("$.status").value("ANSWERED"))
            .andExpect(jsonPath("$.state").value("CLOSED"))
            .andExpect(jsonPath("$.visibility").value("PRIVATE"))
            .andExpect(jsonPath("$.version").value(2))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists());

        verify(participantQuestionService).getQuestionDetails(QUESTION_ID);
    }

    @Test
    void getQuestionDetails_maskedQuestion_shouldReturn404() throws Exception {
        when(participantQuestionService.getQuestionDetails(QUESTION_ID))
            .thenThrow(new QuestionNotFoundException(QUESTION_ID));

        mockMvc.perform(get(QUESTION_URL, QUESTION_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("QUESTION_NOT_FOUND"))
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getQuestionDetails_restrictedForum_shouldReturn403() throws Exception {
        when(participantQuestionService.getQuestionDetails(QUESTION_ID))
            .thenThrow(new QuestionForumAccessRestrictedException(TASK_ASSIGNMENT_ID));

        mockMvc.perform(get(QUESTION_URL, QUESTION_ID))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("QUESTION_ACCESS_RESTRICTED"));
    }

    @Test
    void getQuestionDetails_unauthenticated_shouldReturn401() throws Exception {
        when(participantQuestionService.getQuestionDetails(QUESTION_ID))
            .thenThrow(new AuthenticationException(
                "Authentication is required to access the question forum",
                ErrorCode.AUTHENTICATION_REQUIRED));

        mockMvc.perform(get(QUESTION_URL, QUESTION_ID))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void getQuestionMessages_withoutPaginationParameters_shouldUseDefaultPageAndSize() throws Exception {
        when(participantQuestionService.getQuestionMessages(QUESTION_ID, DEFAULT_PAGE, DEFAULT_SIZE))
            .thenReturn(Page.empty(PageRequest.of(DEFAULT_PAGE, DEFAULT_SIZE)));

        mockMvc.perform(get(MESSAGES_URL, QUESTION_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty())
            .andExpect(jsonPath("$.pageNumber").value(DEFAULT_PAGE))
            .andExpect(jsonPath("$.pageSize").value(DEFAULT_SIZE))
            .andExpect(jsonPath("$.totalElements").value(0));

        verify(participantQuestionService).getQuestionMessages(QUESTION_ID, DEFAULT_PAGE, DEFAULT_SIZE);
    }

    @Test
    void getQuestionMessages_validRequest_shouldReturnMessagePage() throws Exception {
        QuestionMessageResponseDTO comment = new QuestionMessageResponseDTO(
            21L,
            QUESTION_ID,
            AUTHOR_ID,
            COMMENT,
            "Does the limit include the output buffer?",
            CREATED_AT);
        QuestionMessageResponseDTO officialAnswer = new QuestionMessageResponseDTO(
            22L,
            QUESTION_ID,
            REVIEWER_ID,
            OFFICIAL_ANSWER,
            "Yes, both input and output buffers are included.",
            UPDATED_AT);

        when(participantQuestionService.getQuestionMessages(QUESTION_ID, 0, 2))
            .thenReturn(new PageImpl<>(
                List.of(comment, officialAnswer),
                PageRequest.of(0, 2),
                3));

        mockMvc.perform(
            get(MESSAGES_URL, QUESTION_ID)
                .param("page", "0")
                .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].id").value(21))
            .andExpect(jsonPath("$.content[0].authorId").value(AUTHOR_ID))
            .andExpect(jsonPath("$.content[0].type").value("COMMENT"))
            .andExpect(jsonPath("$.content[0].content").value("Does the limit include the output buffer?"))
            .andExpect(jsonPath("$.content[0].createdAt").exists())
            .andExpect(jsonPath("$.content[1].id").value(22))
            .andExpect(jsonPath("$.content[1].authorId").value(REVIEWER_ID))
            .andExpect(jsonPath("$.content[1].type").value("OFFICIAL_ANSWER"))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void getQuestionMessages_invalidPageSize_shouldReturn400() throws Exception {
        when(participantQuestionService.getQuestionMessages(QUESTION_ID, DEFAULT_PAGE, 0))
            .thenThrow(new ValidationException(
                "Page size must be between 1 and 100",
                ErrorCode.COMMON_VALIDATION_FAILED));

        mockMvc.perform(
            get(MESSAGES_URL, QUESTION_ID)
                .param("size", "0"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void getQuestionMessages_maskedQuestion_shouldReturn404() throws Exception {
        when(participantQuestionService.getQuestionMessages(QUESTION_ID, DEFAULT_PAGE, DEFAULT_SIZE))
            .thenThrow(new QuestionNotFoundException(QUESTION_ID));

        mockMvc.perform(get(MESSAGES_URL, QUESTION_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("QUESTION_NOT_FOUND"));
    }

    private QuestionThreadResponseDTO createQuestionResponse() {
        return new QuestionThreadResponseDTO(
            QUESTION_ID,
            TASK_ASSIGNMENT_ID,
            AUTHOR_ID,
            REVIEWER_ID,
            "Clarification about input format",
            "May the input contain duplicate values?",
            ANSWERED,
            PRIVATE,
            CLOSED,
            2L,
            CREATED_AT,
            UPDATED_AT);
    }
}
