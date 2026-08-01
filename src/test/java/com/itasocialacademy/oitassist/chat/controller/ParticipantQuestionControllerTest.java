package com.itasocialacademy.oitassist.chat.controller;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType.COMMENT;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType.OFFICIAL_ANSWER;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.CLOSED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.ANSWERED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import com.itasocialacademy.oitassist.chat.dao.dto.request.CreateCommentRequestDTO;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionInvalidStateException;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType;
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

    private static final Long QUESTION_ID = 10L;
    private static final Long TASK_ASSIGNMENT_ID = 20L;
    private static final Long AUTHOR_ID = 30L;

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 50;

    private static final Long COMMENT_ID = 40L;

    private static final String COMMENT_CONTENT =
            "Could you also clarify the memory limit?";

    private static final String COMMENT_URL =
            "/api/v1/questions/{questionId}/comments";

    private static final String QUESTION_URL =
        "/api/v1/questions/{questionId}";

    private static final String MESSAGE_URL =
        "/api/v1/questions/{questionId}/messages";

    private static final Instant CREATED_AT =
        Instant.parse("2026-07-27T10:00:00Z");

    private static final Instant UPDATED_AT =
        Instant.parse("2026-07-27T10:05:00Z");

    @Mock
    private ParticipantQuestionService participantQuestionService;

    @InjectMocks
    private ParticipantQuestionController participantQuestionController;

    @Override
    protected ParticipantQuestionController getController() {
        return participantQuestionController;
    }

    @Test
    void getQuestionDetails_accessibleClosedQuestion_shouldReturnCompleteDto()
        throws Exception {

        when(participantQuestionService
            .getQuestionDetails(QUESTION_ID))
            .thenReturn(createQuestionResponse());

        mockMvc.perform(
            get(QUESTION_URL, QUESTION_ID))
            .andExpect(status().isOk())
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
                    .value(nullValue()))
            .andExpect(
                jsonPath("$.title")
                    .value("Question title"))
            .andExpect(
                jsonPath("$.content")
                    .value("Question content"))
            .andExpect(
                jsonPath("$.status")
                    .value("ANSWERED"))
            .andExpect(
                jsonPath("$.visibility")
                    .value("PRIVATE"))
            .andExpect(
                jsonPath("$.state")
                    .value("CLOSED"))
            .andExpect(
                jsonPath("$.version")
                    .value(2))
            .andExpect(
                jsonPath("$.createdAt")
                    .exists())
            .andExpect(
                jsonPath("$.updatedAt")
                    .exists());

        verify(participantQuestionService)
            .getQuestionDetails(QUESTION_ID);
    }

    @Test
    void getQuestionDetails_missingQuestion_shouldReturn404()
        throws Exception {

        when(participantQuestionService
            .getQuestionDetails(QUESTION_ID))
            .thenThrow(
                new QuestionNotFoundException(
                    QUESTION_ID));

        mockMvc.perform(
            get(QUESTION_URL, QUESTION_ID))
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.code")
                    .value("QUESTION_NOT_FOUND"));
    }

    @Test
    void getQuestionDetails_maskedPrivateQuestion_shouldReturn404WithoutContent()
        throws Exception {

        when(participantQuestionService
            .getQuestionDetails(QUESTION_ID))
            .thenThrow(
                new QuestionNotFoundException(
                    QUESTION_ID));

        mockMvc.perform(
            get(QUESTION_URL, QUESTION_ID))
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.code")
                    .value("QUESTION_NOT_FOUND"))
            .andExpect(
                jsonPath("$.title")
                    .doesNotExist())
            .andExpect(
                jsonPath("$.content")
                    .doesNotExist());
    }

    @Test
    void getQuestionDetails_assignmentAccessFailure_shouldReturn403()
        throws Exception {

        when(participantQuestionService
            .getQuestionDetails(QUESTION_ID))
            .thenThrow(
                new QuestionForumAccessRestrictedException(
                    TASK_ASSIGNMENT_ID));

        mockMvc.perform(
            get(QUESTION_URL, QUESTION_ID))
            .andExpect(status().isForbidden())
            .andExpect(
                jsonPath("$.code")
                    .value("QUESTION_ACCESS_RESTRICTED"));
    }

    @Test
    void getQuestionDetails_unauthenticated_shouldReturn401()
        throws Exception {

        when(participantQuestionService
            .getQuestionDetails(QUESTION_ID))
            .thenThrow(authenticationException());

        mockMvc.perform(
            get(QUESTION_URL, QUESTION_ID))
            .andExpect(status().isUnauthorized())
            .andExpect(
                jsonPath("$.code")
                    .value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void getQuestionDetails_nonnumericQuestionId_shouldReturn400()
        throws Exception {

        mockMvc.perform(
            get(QUESTION_URL, "invalid"))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(participantQuestionService);
    }

    @Test
    void getQuestionDetails_zeroQuestionId_shouldReturn400()
        throws Exception {

        when(participantQuestionService
            .getQuestionDetails(0L))
            .thenThrow(validationException(
                "Question id must be a positive number"));

        mockMvc.perform(
            get(QUESTION_URL, 0))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void getQuestionDetails_negativeQuestionId_shouldReturn400()
        throws Exception {

        when(participantQuestionService
            .getQuestionDetails(-1L))
            .thenThrow(validationException(
                "Question id must be a positive number"));

        mockMvc.perform(
            get(QUESTION_URL, -1))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void getQuestionMessages_defaultPagination_shouldReturnMessagesInServiceOrder()
        throws Exception {

        QuestionMessageResponseDTO comment =
            createMessageResponse(
                1L,
                COMMENT,
                "Comment",
                CREATED_AT);

        QuestionMessageResponseDTO answer =
            createMessageResponse(
                2L,
                OFFICIAL_ANSWER,
                "Official answer",
                UPDATED_AT);

        Page<QuestionMessageResponseDTO> page =
            new PageImpl<>(
                List.of(comment, answer),
                PageRequest.of(
                    DEFAULT_PAGE,
                    DEFAULT_SIZE),
                2);

        when(participantQuestionService
            .getQuestionMessages(
                QUESTION_ID,
                DEFAULT_PAGE,
                DEFAULT_SIZE))
            .thenReturn(page);

        mockMvc.perform(
            get(MESSAGE_URL, QUESTION_ID))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.content")
                    .isArray())
            .andExpect(
                jsonPath("$.content.length()")
                    .value(2))
            .andExpect(
                jsonPath("$.content[0].id")
                    .value(1L))
            .andExpect(
                jsonPath("$.content[0].type")
                    .value("COMMENT"))
            .andExpect(
                jsonPath("$.content[1].id")
                    .value(2L))
            .andExpect(
                jsonPath("$.content[1].type")
                    .value("OFFICIAL_ANSWER"))
            .andExpect(
                jsonPath("$.pageNumber")
                    .value(DEFAULT_PAGE))
            .andExpect(
                jsonPath("$.pageSize")
                    .value(DEFAULT_SIZE))
            .andExpect(
                jsonPath("$.totalElements")
                    .value(2));

        verify(participantQuestionService)
            .getQuestionMessages(
                QUESTION_ID,
                DEFAULT_PAGE,
                DEFAULT_SIZE);
    }

    @Test
    void getQuestionMessages_explicitPagination_shouldDelegateExactValues()
        throws Exception {

        int pageNumber = 2;
        int pageSize = 10;

        when(participantQuestionService
            .getQuestionMessages(
                QUESTION_ID,
                pageNumber,
                pageSize))
            .thenReturn(
                Page.empty(
                    PageRequest.of(
                        pageNumber,
                        pageSize)));

        mockMvc.perform(
            get(MESSAGE_URL, QUESTION_ID)
                .param(
                    "page",
                    String.valueOf(pageNumber))
                .param(
                    "size",
                    String.valueOf(pageSize)))
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.pageNumber")
                    .value(pageNumber))
            .andExpect(
                jsonPath("$.pageSize")
                    .value(pageSize));

        verify(participantQuestionService)
            .getQuestionMessages(
                QUESTION_ID,
                pageNumber,
                pageSize);
    }

    @Test
    void getQuestionMessages_emptyHistory_shouldReturnEmptyPage()
        throws Exception {

        when(participantQuestionService
            .getQuestionMessages(
                QUESTION_ID,
                DEFAULT_PAGE,
                DEFAULT_SIZE))
            .thenReturn(
                Page.empty(
                    PageRequest.of(
                        DEFAULT_PAGE,
                        DEFAULT_SIZE)));

        mockMvc.perform(
            get(MESSAGE_URL, QUESTION_ID))
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
    void getQuestionMessages_missingQuestion_shouldReturn404()
        throws Exception {

        when(participantQuestionService
            .getQuestionMessages(
                QUESTION_ID,
                DEFAULT_PAGE,
                DEFAULT_SIZE))
            .thenThrow(
                new QuestionNotFoundException(
                    QUESTION_ID));

        mockMvc.perform(
            get(MESSAGE_URL, QUESTION_ID))
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.code")
                    .value("QUESTION_NOT_FOUND"));
    }

    @Test
    void getQuestionMessages_maskedPrivateQuestion_shouldReturn404WithoutContent()
        throws Exception {

        when(participantQuestionService
            .getQuestionMessages(
                QUESTION_ID,
                DEFAULT_PAGE,
                DEFAULT_SIZE))
            .thenThrow(
                new QuestionNotFoundException(
                    QUESTION_ID));

        mockMvc.perform(
            get(MESSAGE_URL, QUESTION_ID))
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.code")
                    .value("QUESTION_NOT_FOUND"))
            .andExpect(
                jsonPath("$.content")
                    .doesNotExist());
    }

    @Test
    void getQuestionMessages_assignmentAccessFailure_shouldReturn403()
        throws Exception {

        when(participantQuestionService
            .getQuestionMessages(
                QUESTION_ID,
                DEFAULT_PAGE,
                DEFAULT_SIZE))
            .thenThrow(
                new QuestionForumAccessRestrictedException(
                    TASK_ASSIGNMENT_ID));

        mockMvc.perform(
            get(MESSAGE_URL, QUESTION_ID))
            .andExpect(status().isForbidden())
            .andExpect(
                jsonPath("$.code")
                    .value("QUESTION_ACCESS_RESTRICTED"));
    }

    @Test
    void getQuestionMessages_unauthenticated_shouldReturn401()
        throws Exception {

        when(participantQuestionService
            .getQuestionMessages(
                QUESTION_ID,
                DEFAULT_PAGE,
                DEFAULT_SIZE))
            .thenThrow(authenticationException());

        mockMvc.perform(
            get(MESSAGE_URL, QUESTION_ID))
            .andExpect(status().isUnauthorized())
            .andExpect(
                jsonPath("$.code")
                    .value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void getQuestionMessages_nonnumericQuestionId_shouldReturn400()
        throws Exception {

        mockMvc.perform(
            get(MESSAGE_URL, "invalid"))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(participantQuestionService);
    }

    @Test
    void getQuestionMessages_zeroQuestionId_shouldReturn400()
        throws Exception {

        when(participantQuestionService
            .getQuestionMessages(
                0L,
                DEFAULT_PAGE,
                DEFAULT_SIZE))
            .thenThrow(validationException(
                "Question id must be a positive number"));

        mockMvc.perform(
            get(MESSAGE_URL, 0))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void getQuestionMessages_negativeQuestionId_shouldReturn400()
        throws Exception {

        when(participantQuestionService
            .getQuestionMessages(
                -1L,
                DEFAULT_PAGE,
                DEFAULT_SIZE))
            .thenThrow(validationException(
                "Question id must be a positive number"));

        mockMvc.perform(
            get(MESSAGE_URL, -1))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void getQuestionMessages_negativePage_shouldReturn400()
        throws Exception {

        when(participantQuestionService
            .getQuestionMessages(
                QUESTION_ID,
                -1,
                DEFAULT_SIZE))
            .thenThrow(validationException(
                "Page number must not be negative"));

        mockMvc.perform(
            get(MESSAGE_URL, QUESTION_ID)
                .param("page", "-1"))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void getQuestionMessages_zeroSize_shouldReturn400()
        throws Exception {

        when(participantQuestionService
            .getQuestionMessages(
                QUESTION_ID,
                DEFAULT_PAGE,
                0))
            .thenThrow(validationException(
                "Page size must be between 1 and 100"));

        mockMvc.perform(
            get(MESSAGE_URL, QUESTION_ID)
                .param("size", "0"))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void getQuestionMessages_sizeAboveMaximum_shouldReturn400()
        throws Exception {

        when(participantQuestionService
            .getQuestionMessages(
                QUESTION_ID,
                DEFAULT_PAGE,
                101))
            .thenThrow(validationException(
                "Page size must be between 1 and 100"));

        mockMvc.perform(
            get(MESSAGE_URL, QUESTION_ID)
                .param("size", "101"))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void getQuestionMessages_nonnumericPage_shouldReturn400()
        throws Exception {

        mockMvc.perform(
            get(MESSAGE_URL, QUESTION_ID)
                .param("page", "invalid"))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(participantQuestionService);
    }

    @Test
    void getQuestionMessages_nonnumericSize_shouldReturn400()
        throws Exception {

        mockMvc.perform(
            get(MESSAGE_URL, QUESTION_ID)
                .param("size", "invalid"))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(participantQuestionService);
    }

    @Test
    void addComment_validRequest_shouldReturn201WithCreatedComment()
            throws Exception {

        QuestionMessageResponseDTO response =
                createCommentResponse();

        when(participantQuestionService.addComment(
                eq(QUESTION_ID),
                any(CreateCommentRequestDTO.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post(COMMENT_URL, QUESTION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                {
                  "content": "Could you also clarify the memory limit?"
                }
                """))
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.id")
                                .value(COMMENT_ID))
                .andExpect(
                        jsonPath("$.questionThreadId")
                                .value(QUESTION_ID))
                .andExpect(
                        jsonPath("$.authorId")
                                .value(AUTHOR_ID))
                .andExpect(
                        jsonPath("$.type")
                                .value("COMMENT"))
                .andExpect(
                        jsonPath("$.content")
                                .value(COMMENT_CONTENT))
                .andExpect(
                        jsonPath("$.createdAt")
                                .exists());

        ArgumentCaptor<CreateCommentRequestDTO> requestCaptor =
                ArgumentCaptor.forClass(
                        CreateCommentRequestDTO.class);

        verify(participantQuestionService)
                .addComment(
                        eq(QUESTION_ID),
                        requestCaptor.capture());

        assertEquals(
                COMMENT_CONTENT,
                requestCaptor.getValue().content());
    }

    @Test
    void addComment_protectedFields_shouldNotOverrideRequestContract()
            throws Exception {

        QuestionMessageResponseDTO response =
                createCommentResponse();

        when(participantQuestionService.addComment(
                eq(QUESTION_ID),
                any(CreateCommentRequestDTO.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post(COMMENT_URL, QUESTION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                {
                  "content": "Could you also clarify the memory limit?",
                  "questionThreadId": 999,
                  "questionId": 999,
                  "authorId": 999,
                  "type": "OFFICIAL_ANSWER",
                  "createdAt": "2020-01-01T00:00:00Z"
                }
                """))
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.questionThreadId")
                                .value(QUESTION_ID))
                .andExpect(
                        jsonPath("$.authorId")
                                .value(AUTHOR_ID))
                .andExpect(
                        jsonPath("$.type")
                                .value("COMMENT"));

        ArgumentCaptor<CreateCommentRequestDTO> requestCaptor =
                ArgumentCaptor.forClass(
                        CreateCommentRequestDTO.class);

        verify(participantQuestionService)
                .addComment(
                        eq(QUESTION_ID),
                        requestCaptor.capture());

        assertEquals(
                COMMENT_CONTENT,
                requestCaptor.getValue().content());
    }

    @Test
    void addComment_missingRequestBody_shouldReturn400()
            throws Exception {

        mockMvc.perform(
                        post(COMMENT_URL, QUESTION_ID)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(
                participantQuestionService);
    }

    @Test
    void addComment_missingContent_shouldReturn400()
            throws Exception {

        mockMvc.perform(
                        post(COMMENT_URL, QUESTION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(
                participantQuestionService);
    }

    @Test
    void addComment_blankContent_shouldReturn400()
            throws Exception {

        mockMvc.perform(
                        post(COMMENT_URL, QUESTION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                {
                  "content": "   "
                }
                """))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(
                participantQuestionService);
    }

    @Test
    void addComment_overlongContent_shouldReturn400()
            throws Exception {

        String requestBody =
                objectMapper.writeValueAsString(
                        new CreateCommentRequestDTO(
                                "a".repeat(10_001)));

        mockMvc.perform(
                        post(COMMENT_URL, QUESTION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(
                participantQuestionService);
    }

    @Test
    void addComment_malformedBody_shouldReturn400()
            throws Exception {

        mockMvc.perform(
                        post(COMMENT_URL, QUESTION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                {
                  "content":
                }
                """))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(
                participantQuestionService);
    }

    @Test
    void addComment_nonnumericQuestionId_shouldReturn400()
            throws Exception {

        mockMvc.perform(
                        post(COMMENT_URL, "invalid")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                {
                  "content": "Comment"
                }
                """))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(
                participantQuestionService);
    }

    @Test
    void addComment_zeroQuestionId_shouldReturn400()
            throws Exception {

        CreateCommentRequestDTO request =
                new CreateCommentRequestDTO(
                        COMMENT_CONTENT);

        when(participantQuestionService.addComment(
                0L,
                request))
                .thenThrow(validationException(
                        "Question id must be a positive number"));

        mockMvc.perform(
                        post(COMMENT_URL, 0)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                request)))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void addComment_negativeQuestionId_shouldReturn400()
            throws Exception {

        CreateCommentRequestDTO request =
                new CreateCommentRequestDTO(
                        COMMENT_CONTENT);

        when(participantQuestionService.addComment(
                -1L,
                request))
                .thenThrow(validationException(
                        "Question id must be a positive number"));

        mockMvc.perform(
                        post(COMMENT_URL, -1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                request)))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("COMMON_VALIDATION_FAILED"));
    }

    @Test
    void addComment_authenticationFailure_shouldReturn401()
            throws Exception {

        when(participantQuestionService.addComment(
                eq(QUESTION_ID),
                any(CreateCommentRequestDTO.class)))
                .thenThrow(authenticationException());

        mockMvc.perform(
                        post(COMMENT_URL, QUESTION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                {
                  "content": "Comment"
                }
                """))
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.code")
                                .value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void addComment_assignmentAccessFailure_shouldReturn403()
            throws Exception {

        when(participantQuestionService.addComment(
                eq(QUESTION_ID),
                any(CreateCommentRequestDTO.class)))
                .thenThrow(
                        new QuestionForumAccessRestrictedException(
                                TASK_ASSIGNMENT_ID));

        mockMvc.perform(
                        post(COMMENT_URL, QUESTION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                {
                  "content": "Comment"
                }
                """))
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.code")
                                .value("QUESTION_ACCESS_RESTRICTED"))
                .andExpect(
                        jsonPath("$.content")
                                .doesNotExist());
    }

    @Test
    void addComment_maskedPrivateQuestion_shouldReturn404()
            throws Exception {

        when(participantQuestionService.addComment(
                eq(QUESTION_ID),
                any(CreateCommentRequestDTO.class)))
                .thenThrow(
                        new QuestionNotFoundException(
                                QUESTION_ID));

        mockMvc.perform(
                        post(COMMENT_URL, QUESTION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                {
                  "content": "Comment"
                }
                """))
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.code")
                                .value("QUESTION_NOT_FOUND"))
                .andExpect(
                        jsonPath("$.content")
                                .doesNotExist());
    }

    @Test
    void addComment_closedQuestion_shouldReturn409()
            throws Exception {

        when(participantQuestionService.addComment(
                eq(QUESTION_ID),
                any(CreateCommentRequestDTO.class)))
                .thenThrow(
                        new QuestionInvalidStateException(
                                QUESTION_ID,
                                OPEN,
                                CLOSED));

        mockMvc.perform(
                        post(COMMENT_URL, QUESTION_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                {
                  "content": "Comment"
                }
                """))
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.code")
                                .value("QUESTION_INVALID_STATE"))
                .andExpect(
                        jsonPath("$.content")
                                .doesNotExist());
    }

    private QuestionMessageResponseDTO
    createCommentResponse() {

        return new QuestionMessageResponseDTO(
                COMMENT_ID,
                QUESTION_ID,
                AUTHOR_ID,
                COMMENT,
                COMMENT_CONTENT,
                CREATED_AT);
    }

    private QuestionThreadResponseDTO createQuestionResponse() {
        return new QuestionThreadResponseDTO(
            QUESTION_ID,
            TASK_ASSIGNMENT_ID,
            AUTHOR_ID,
            null,
            "Question title",
            "Question content",
            ANSWERED,
            PRIVATE,
            CLOSED,
            2L,
            CREATED_AT,
            UPDATED_AT);
    }

    private QuestionMessageResponseDTO createMessageResponse(
        Long id,
        QuestionMessageType type,
        String content,
        Instant createdAt) {

        return new QuestionMessageResponseDTO(
            id,
            QUESTION_ID,
            AUTHOR_ID,
            type,
            content,
            createdAt);
    }

    private ValidationException validationException(
        String message) {

        return new ValidationException(
            message,
            ErrorCode.COMMON_VALIDATION_FAILED);
    }

    private AuthenticationException authenticationException() {

        return new AuthenticationException(
            "Authentication is required to access the question forum",
            ErrorCode.AUTHENTICATION_REQUIRED);
    }
}