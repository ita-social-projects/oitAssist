package com.itasocialacademy.oitassist.chat.controller;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.NEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PUBLIC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadSummaryResponseDTO;
import com.itasocialacademy.oitassist.chat.service.interfaces.ParticipantForumService;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.task.exceptions.TaskNotFoundException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import com.itasocialacademy.oitassist.chat.dao.dto.request.CreateQuestionRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class ParticipantForumControllerTest
    extends ControllerUnitTest<ParticipantForumController> {

    private static final Long TASK_ID = 1L;
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;

    private static final Long USER_ID = 100L;
    private static final Long QUESTION_ID = 11L;

    private static final String QUESTION_TITLE = "Clarification about input format";

    private static final String QUESTION_CONTENT = "May the input contain duplicate values?";

    private static final Instant CREATED_AT = Instant.parse("2026-07-24T10:00:00Z");

    private static final Instant UPDATED_AT = Instant.parse("2026-07-24T10:05:00Z");

    @Mock
    private ParticipantForumService participantForumService;

    @InjectMocks
    private ParticipantForumController participantForumController;

    @Override
    protected ParticipantForumController getController() {
        return participantForumController;
    }

    @Test
    void getParticipantForum_validRequest_shouldReturn200()
        throws Exception {

        QuestionThreadSummaryResponseDTO response =
            createQuestionSummaryResponse();

        Page<QuestionThreadSummaryResponseDTO> page =
            new PageImpl<>(
                List.of(response),
                PageRequest.of(DEFAULT_PAGE, DEFAULT_SIZE),
                1);

        when(participantForumService.getForumQuestions(
            TASK_ID,
            DEFAULT_PAGE,
            DEFAULT_SIZE)).thenReturn(page);

        mockMvc.perform(
            get("/api/v1/tasks/{taskId}/questions", TASK_ID)
                .param("page", String.valueOf(DEFAULT_PAGE))
                .param("size", String.valueOf(DEFAULT_SIZE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].id").value(11L))
            .andExpect(jsonPath("$.content[0].taskId").value(TASK_ID))
            .andExpect(jsonPath("$.content[0].authorId").value(100L))
            .andExpect(
                jsonPath("$.content[0].title")
                    .value("How should I submit the solution?"))
            .andExpect(jsonPath("$.content[0].status").value("NEW"))
            .andExpect(jsonPath("$.content[0].state").value("OPEN"))
            .andExpect(
                jsonPath("$.content[0].visibility").value("PUBLIC"));

        verify(participantForumService).getForumQuestions(
            TASK_ID,
            DEFAULT_PAGE,
            DEFAULT_SIZE);
    }

    @Test
    void getParticipantForum_shouldReturnPageMetadata()
        throws Exception {

        int pageNumber = 2;
        int pageSize = 5;
        long totalElements = 11;

        Page<QuestionThreadSummaryResponseDTO> page =
            new PageImpl<>(
                List.of(createQuestionSummaryResponse()),
                PageRequest.of(pageNumber, pageSize),
                totalElements);

        when(participantForumService.getForumQuestions(
            TASK_ID,
            pageNumber,
            pageSize)).thenReturn(page);

        mockMvc.perform(
            get("/api/v1/tasks/{taskId}/questions", TASK_ID)
                .param("page", String.valueOf(pageNumber))
                .param("size", String.valueOf(pageSize)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pageNumber").value(pageNumber))
            .andExpect(jsonPath("$.pageSize").value(pageSize))
            .andExpect(jsonPath("$.totalPages").value(3))
            .andExpect(jsonPath("$.totalElements").value(totalElements))
            .andExpect(jsonPath("$.first").value(false))
            .andExpect(jsonPath("$.last").value(true));

        verify(participantForumService).getForumQuestions(
            TASK_ID,
            pageNumber,
            pageSize);
    }

    @Test
    void getParticipantForum_withoutPagination_shouldUseDefaults()
        throws Exception {

        when(participantForumService.getForumQuestions(
            TASK_ID,
            DEFAULT_PAGE,
            DEFAULT_SIZE)).thenReturn(
                Page.empty(
                    PageRequest.of(DEFAULT_PAGE, DEFAULT_SIZE)));

        mockMvc.perform(
            get("/api/v1/tasks/{taskId}/questions", TASK_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pageNumber").value(DEFAULT_PAGE))
            .andExpect(jsonPath("$.pageSize").value(DEFAULT_SIZE));

        verify(participantForumService).getForumQuestions(
            TASK_ID,
            DEFAULT_PAGE,
            DEFAULT_SIZE);
    }

    @Test
    void getParticipantForum_emptyPage_shouldReturn200()
        throws Exception {

        Page<QuestionThreadSummaryResponseDTO> emptyPage =
            Page.empty(
                PageRequest.of(DEFAULT_PAGE, DEFAULT_SIZE));

        when(participantForumService.getForumQuestions(
            TASK_ID,
            DEFAULT_PAGE,
            DEFAULT_SIZE)).thenReturn(emptyPage);

        mockMvc.perform(
            get("/api/v1/tasks/{taskId}/questions", TASK_ID)
                .param("page", String.valueOf(DEFAULT_PAGE))
                .param("size", String.valueOf(DEFAULT_SIZE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty())
            .andExpect(jsonPath("$.pageNumber").value(DEFAULT_PAGE))
            .andExpect(jsonPath("$.pageSize").value(DEFAULT_SIZE))
            .andExpect(jsonPath("$.totalPages").value(0))
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(true));

        verify(participantForumService).getForumQuestions(
            TASK_ID,
            DEFAULT_PAGE,
            DEFAULT_SIZE);
    }

    @Test
    void getParticipantForum_invalidTaskId_shouldReturn400()
        throws Exception {

        Long invalidTaskId = 0L;

        when(participantForumService.getForumQuestions(
            invalidTaskId,
            DEFAULT_PAGE,
            DEFAULT_SIZE)).thenThrow(validationException(
                "Task id must be a positive number"));

        mockMvc.perform(
            get(
                "/api/v1/tasks/{taskId}/questions",
                invalidTaskId)
                .param("page", String.valueOf(DEFAULT_PAGE))
                .param("size", String.valueOf(DEFAULT_SIZE)))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("COMMON_VALIDATION_FAILED"));

        verify(participantForumService).getForumQuestions(
            invalidTaskId,
            DEFAULT_PAGE,
            DEFAULT_SIZE);
    }

    @Test
    void getParticipantForum_negativePage_shouldReturn400()
        throws Exception {

        int invalidPage = -1;

        when(participantForumService.getForumQuestions(
            TASK_ID,
            invalidPage,
            DEFAULT_SIZE)).thenThrow(validationException(
                "Page number must not be negative"));

        mockMvc.perform(
            get("/api/v1/tasks/{taskId}/questions", TASK_ID)
                .param("page", String.valueOf(invalidPage))
                .param("size", String.valueOf(DEFAULT_SIZE)))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("COMMON_VALIDATION_FAILED"));

        verify(participantForumService).getForumQuestions(
            TASK_ID,
            invalidPage,
            DEFAULT_SIZE);
    }

    @Test
    void getParticipantForum_invalidSize_shouldReturn400()
        throws Exception {

        int zeroSize = 0;
        int excessiveSize = 101;

        when(participantForumService.getForumQuestions(
            TASK_ID,
            DEFAULT_PAGE,
            zeroSize)).thenThrow(validationException(
                "Page size must be between 1 and 100"));

        when(participantForumService.getForumQuestions(
            TASK_ID,
            DEFAULT_PAGE,
            excessiveSize)).thenThrow(validationException(
                "Page size must be between 1 and 100"));

        mockMvc.perform(
            get("/api/v1/tasks/{taskId}/questions", TASK_ID)
                .param("page", String.valueOf(DEFAULT_PAGE))
                .param("size", String.valueOf(zeroSize)))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("COMMON_VALIDATION_FAILED"));

        mockMvc.perform(
            get("/api/v1/tasks/{taskId}/questions", TASK_ID)
                .param("page", String.valueOf(DEFAULT_PAGE))
                .param("size", String.valueOf(excessiveSize)))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("COMMON_VALIDATION_FAILED"));

        verify(participantForumService).getForumQuestions(
            TASK_ID,
            DEFAULT_PAGE,
            zeroSize);

        verify(participantForumService).getForumQuestions(
            TASK_ID,
            DEFAULT_PAGE,
            excessiveSize);
    }

    @Test
    void getParticipantForum_nonNumericTaskId_shouldReturn400()
        throws Exception {

        mockMvc.perform(
            get(
                "/api/v1/tasks/{taskId}/questions",
                "not-a-number"))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(participantForumService);
    }

    @Test
    void getParticipantForum_nonNumericPage_shouldReturn400()
        throws Exception {

        mockMvc.perform(
            get("/api/v1/tasks/{taskId}/questions", TASK_ID)
                .param("page", "not-a-number")
                .param("size", String.valueOf(DEFAULT_SIZE)))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(participantForumService);
    }

    @Test
    void getParticipantForum_unauthenticated_shouldReturn401()
        throws Exception {

        when(participantForumService.getForumQuestions(
            TASK_ID,
            DEFAULT_PAGE,
            DEFAULT_SIZE)).thenThrow(new AuthenticationException(
                "Authentication is required to access the question forum",
                ErrorCode.AUTHENTICATION_REQUIRED));

        mockMvc.perform(
            get("/api/v1/tasks/{taskId}/questions", TASK_ID)
                .param("page", String.valueOf(DEFAULT_PAGE))
                .param("size", String.valueOf(DEFAULT_SIZE)))
            .andExpect(status().isUnauthorized())
            .andExpect(
                jsonPath("$.code")
                    .value("AUTHENTICATION_REQUIRED"))
            .andExpect(
                jsonPath("$.message")
                    .value(
                        "Authentication is required to access "
                            + "the question forum"));

        verify(participantForumService).getForumQuestions(
            TASK_ID,
            DEFAULT_PAGE,
            DEFAULT_SIZE);
    }

    @Test
    void getParticipantForum_missingTask_shouldReturn404()
        throws Exception {

        Long missingTaskId = 999L;

        when(participantForumService.getForumQuestions(
            missingTaskId,
            DEFAULT_PAGE,
            DEFAULT_SIZE)).thenThrow(new TaskNotFoundException(missingTaskId));

        mockMvc.perform(
            get(
                "/api/v1/tasks/{taskId}/questions",
                missingTaskId)
                .param("page", String.valueOf(DEFAULT_PAGE))
                .param("size", String.valueOf(DEFAULT_SIZE)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"))
            .andExpect(
                jsonPath("$.message")
                    .value(
                        "Task with id %s was not found"
                            .formatted(missingTaskId)));

        verify(participantForumService).getForumQuestions(
            missingTaskId,
            DEFAULT_PAGE,
            DEFAULT_SIZE);
    }

    @Test
    void createQuestion_validRequest_shouldReturn201() throws Exception {
        CreateQuestionRequestDTO request = createQuestionRequest();

        when(participantForumService.createQuestion(
            eq(TASK_ID),
            any(CreateQuestionRequestDTO.class))).thenReturn(createQuestionResponse());

        mockMvc.perform(post(
            "/api/v1/tasks/{taskId}/questions",
            TASK_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        verify(participantForumService).createQuestion(
            eq(TASK_ID),
            any(CreateQuestionRequestDTO.class));
    }

    @Test
    void createQuestion_validRequest_shouldReturnCreatedQuestion()
        throws Exception {

        CreateQuestionRequestDTO request = createQuestionRequest();

        when(participantForumService.createQuestion(
            eq(TASK_ID),
            any(CreateQuestionRequestDTO.class))).thenReturn(createQuestionResponse());

        mockMvc.perform(post(
            "/api/v1/tasks/{taskId}/questions",
            TASK_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(QUESTION_ID))
            .andExpect(jsonPath("$.taskId").value(TASK_ID))
            .andExpect(jsonPath("$.authorId").value(USER_ID))
            .andExpect(jsonPath("$.assignedReviewerId").value(nullValue()))
            .andExpect(jsonPath("$.title").value(QUESTION_TITLE))
            .andExpect(jsonPath("$.content").value(QUESTION_CONTENT))
            .andExpect(jsonPath("$.status").value("NEW"))
            .andExpect(jsonPath("$.state").value("OPEN"))
            .andExpect(jsonPath("$.visibility").value("PRIVATE"))
            .andExpect(jsonPath("$.version").value(0))
            .andExpect(
                jsonPath("$.createdAt")
                    .value(CREATED_AT.toString()))
            .andExpect(
                jsonPath("$.updatedAt")
                    .value(UPDATED_AT.toString()));
    }

    @Test
    void createQuestion_validRequest_shouldDelegateTaskIdAndRequest()
        throws Exception {

        CreateQuestionRequestDTO request = createQuestionRequest();

        when(participantForumService.createQuestion(
            eq(TASK_ID),
            any(CreateQuestionRequestDTO.class))).thenReturn(createQuestionResponse());

        mockMvc.perform(post(
            "/api/v1/tasks/{taskId}/questions",
            TASK_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        ArgumentCaptor<CreateQuestionRequestDTO> requestCaptor =
            ArgumentCaptor.forClass(CreateQuestionRequestDTO.class);

        verify(participantForumService).createQuestion(
            eq(TASK_ID),
            requestCaptor.capture());

        CreateQuestionRequestDTO capturedRequest =
            requestCaptor.getValue();

        assertEquals(
            QUESTION_TITLE,
            capturedRequest.title());
        assertEquals(
            QUESTION_CONTENT,
            capturedRequest.content());
    }

    @Test
    void createQuestion_blankTitle_shouldReturn400()
        throws Exception {

        CreateQuestionRequestDTO request =
            new CreateQuestionRequestDTO(
                "   ",
                QUESTION_CONTENT);

        mockMvc.perform(post(
            "/api/v1/tasks/{taskId}/questions",
            TASK_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("COMMON_VALIDATION_FAILED"))
            .andExpect(
                jsonPath("$.details.errors.title").exists());

        verifyNoInteractions(participantForumService);
    }

    @Test
    void createQuestion_missingTitle_shouldReturn400()
        throws Exception {

        String request = """
            {
              "content": "%s"
            }
            """.formatted(QUESTION_CONTENT);

        mockMvc.perform(post(
            "/api/v1/tasks/{taskId}/questions",
            TASK_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(request))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("COMMON_VALIDATION_FAILED"))
            .andExpect(
                jsonPath("$.details.errors.title").exists());

        verifyNoInteractions(participantForumService);
    }

    @Test
    void createQuestion_overlongTitle_shouldReturn400()
        throws Exception {

        CreateQuestionRequestDTO request =
            new CreateQuestionRequestDTO(
                "a".repeat(201),
                QUESTION_CONTENT);

        mockMvc.perform(post(
            "/api/v1/tasks/{taskId}/questions",
            TASK_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("COMMON_VALIDATION_FAILED"))
            .andExpect(
                jsonPath("$.details.errors.title").exists());

        verifyNoInteractions(participantForumService);
    }

    @Test
    void createQuestion_blankContent_shouldReturn400()
        throws Exception {

        CreateQuestionRequestDTO request =
            new CreateQuestionRequestDTO(
                QUESTION_TITLE,
                "   ");

        mockMvc.perform(post(
            "/api/v1/tasks/{taskId}/questions",
            TASK_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("COMMON_VALIDATION_FAILED"))
            .andExpect(
                jsonPath("$.details.errors.content").exists());

        verifyNoInteractions(participantForumService);
    }

    @Test
    void createQuestion_missingContent_shouldReturn400()
        throws Exception {

        String request = """
            {
              "title": "%s"
            }
            """.formatted(QUESTION_TITLE);

        mockMvc.perform(post(
            "/api/v1/tasks/{taskId}/questions",
            TASK_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(request))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("COMMON_VALIDATION_FAILED"))
            .andExpect(
                jsonPath("$.details.errors.content").exists());

        verifyNoInteractions(participantForumService);
    }

    @Test
    void createQuestion_overlongContent_shouldReturn400()
        throws Exception {

        CreateQuestionRequestDTO request =
            new CreateQuestionRequestDTO(
                QUESTION_TITLE,
                "a".repeat(10_001));

        mockMvc.perform(post(
            "/api/v1/tasks/{taskId}/questions",
            TASK_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("COMMON_VALIDATION_FAILED"))
            .andExpect(
                jsonPath("$.details.errors.content").exists());

        verifyNoInteractions(participantForumService);
    }

    @Test
    void createQuestion_missingBody_shouldReturn400()
        throws Exception {

        mockMvc.perform(post(
            "/api/v1/tasks/{taskId}/questions",
            TASK_ID)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("COMMON_VALIDATION_FAILED"))
            .andExpect(
                jsonPath("$.message")
                    .value("Request body is missing or malformed"));

        verifyNoInteractions(participantForumService);
    }

    @Test
    void createQuestion_malformedBody_shouldReturn400()
        throws Exception {

        String malformedJson = """
            {
              "title": "Question",
              "content":
            }
            """;

        mockMvc.perform(post(
            "/api/v1/tasks/{taskId}/questions",
            TASK_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(malformedJson))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("COMMON_VALIDATION_FAILED"))
            .andExpect(
                jsonPath("$.message")
                    .value("Request body is missing or malformed"));

        verifyNoInteractions(participantForumService);
    }

    @Test
    void createQuestion_invalidTaskId_shouldReturn400()
        throws Exception {

        Long invalidTaskId = 0L;
        CreateQuestionRequestDTO request = createQuestionRequest();

        when(participantForumService.createQuestion(
            invalidTaskId,
            request)).thenThrow(new ValidationException(
                "Task id must be a positive number",
                ErrorCode.COMMON_VALIDATION_FAILED));

        mockMvc.perform(post(
            "/api/v1/tasks/{taskId}/questions",
            invalidTaskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("COMMON_VALIDATION_FAILED"));

        verify(participantForumService).createQuestion(
            invalidTaskId,
            request);
    }

    @Test
    void createQuestion_nonNumericTaskId_shouldReturn400()
        throws Exception {

        CreateQuestionRequestDTO request = createQuestionRequest();

        mockMvc.perform(post(
            "/api/v1/tasks/{taskId}/questions",
            "not-a-number")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.code")
                    .value("COMMON_VALIDATION_FAILED"));

        verifyNoInteractions(participantForumService);
    }

    @Test
    void createQuestion_unauthenticated_shouldReturn401()
        throws Exception {

        CreateQuestionRequestDTO request = createQuestionRequest();

        when(participantForumService.createQuestion(
            TASK_ID,
            request)).thenThrow(new AuthenticationException(
                "Authentication is required to access the question forum",
                ErrorCode.AUTHENTICATION_REQUIRED));

        mockMvc.perform(post(
            "/api/v1/tasks/{taskId}/questions",
            TASK_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(
                jsonPath("$.code")
                    .value("AUTHENTICATION_REQUIRED"))
            .andExpect(
                jsonPath("$.message")
                    .value(
                        "Authentication is required to access "
                            + "the question forum"));

        verify(participantForumService).createQuestion(
            TASK_ID,
            request);
    }

    @Test
    void createQuestion_missingTask_shouldReturn404()
        throws Exception {

        Long missingTaskId = 999L;
        CreateQuestionRequestDTO request = createQuestionRequest();

        when(participantForumService.createQuestion(
            missingTaskId,
            request)).thenThrow(new TaskNotFoundException(missingTaskId));

        mockMvc.perform(post(
            "/api/v1/tasks/{taskId}/questions",
            missingTaskId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.code").value("TASK_NOT_FOUND"))
            .andExpect(
                jsonPath("$.message")
                    .value(
                        "Task with id %s was not found"
                            .formatted(missingTaskId)));

        verify(participantForumService).createQuestion(
            missingTaskId,
            request);
    }

    @Test
    void createQuestion_protectedFields_shouldNotOverrideServerValues()
        throws Exception {

        String requestBody = """
            {
              "title": "%s",
              "content": "%s",
              "taskId": 999,
              "authorId": 999,
              "assignedReviewerId": 777,
              "status": "ANSWERED",
              "state": "CLOSED",
              "visibility": "PUBLIC",
              "version": 50,
              "createdAt": "2020-01-01T00:00:00Z",
              "updatedAt": "2020-01-01T00:00:00Z"
            }
            """.formatted(
            QUESTION_TITLE,
            QUESTION_CONTENT);

        when(participantForumService.createQuestion(
            eq(TASK_ID),
            any(CreateQuestionRequestDTO.class))).thenReturn(createQuestionResponse());

        mockMvc.perform(post(
            "/api/v1/tasks/{taskId}/questions",
            TASK_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.taskId").value(TASK_ID))
            .andExpect(jsonPath("$.authorId").value(USER_ID))
            .andExpect(jsonPath("$.assignedReviewerId").value(nullValue()))
            .andExpect(jsonPath("$.status").value("NEW"))
            .andExpect(jsonPath("$.state").value("OPEN"))
            .andExpect(jsonPath("$.visibility").value("PRIVATE"))
            .andExpect(jsonPath("$.version").value(0));

        ArgumentCaptor<CreateQuestionRequestDTO> requestCaptor =
            ArgumentCaptor.forClass(CreateQuestionRequestDTO.class);

        verify(participantForumService).createQuestion(
            eq(TASK_ID),
            requestCaptor.capture());

        CreateQuestionRequestDTO capturedRequest =
            requestCaptor.getValue();

        assertEquals(
            QUESTION_TITLE,
            capturedRequest.title());
        assertEquals(
            QUESTION_CONTENT,
            capturedRequest.content());
    }

    private CreateQuestionRequestDTO createQuestionRequest() {
        return new CreateQuestionRequestDTO(
            QUESTION_TITLE,
            QUESTION_CONTENT);
    }

    private QuestionThreadResponseDTO createQuestionResponse() {
        return new QuestionThreadResponseDTO(
            QUESTION_ID,
            TASK_ID,
            USER_ID,
            null,
            QUESTION_TITLE,
            QUESTION_CONTENT,
            NEW,
            PRIVATE,
            OPEN,
            0L,
            CREATED_AT,
            UPDATED_AT);
    }

    private QuestionThreadSummaryResponseDTO createQuestionSummaryResponse() {

        return new QuestionThreadSummaryResponseDTO(
            11L,
            TASK_ID,
            100L,
            "How should I submit the solution?",
            NEW,
            PUBLIC,
            OPEN,
            Instant.parse("2026-07-24T10:00:00Z"),
            Instant.parse("2026-07-24T10:15:00Z"));
    }

    private ValidationException validationException(
        String message) {
        return new ValidationException(
            message,
            ErrorCode.COMMON_VALIDATION_FAILED);
    }
}