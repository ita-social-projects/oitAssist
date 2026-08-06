package com.itasocialacademy.oitassist.chat.service;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.CLOSED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.ANSWERED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.NEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PUBLIC;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.itasocialacademy.oitassist.chat.dao.dto.request.CreateQuestionRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadSummaryResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionState;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import com.itasocialacademy.oitassist.chat.mapper.QuestionThreadMapper;
import com.itasocialacademy.oitassist.chat.utils.QuestionAccessPolicy;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.taskassignment.exceptions.TaskAssignmentNotFoundException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class ParticipantForumServiceImplTest {

    private static final Long TASK_ASSIGNMENT_ID = 1L;
    private static final Long OTHER_TASK_ASSIGNMENT_ID = 2L;
    private static final Long USER_ID = 100L;
    private static final Long OTHER_USER_ID = 200L;

    private static final int PAGE = 0;
    private static final int SIZE = 20;

    private static final Long CREATED_QUESTION_ID = 11L;

    private static final String QUESTION_TITLE = "Clarification about input format";

    private static final String QUESTION_CONTENT = "May the input contain duplicate values?";

    private static final Instant CREATED_AT = Instant.parse("2026-07-24T10:00:00Z");

    private static final Instant UPDATED_AT = Instant.parse("2026-07-24T10:00:00Z");

    @Mock
    private QuestionThreadRepository questionThreadRepository;

    @Mock
    private QuestionThreadMapper questionThreadMapper;

    @Mock
    private QuestionAccessPolicy questionAccessPolicy;

    @InjectMocks
    private ParticipantForumServiceImpl participantForumService;

    @Test
    void getForumQuestions_accessibleTaskAssignment_shouldReturnMappedPage() {
        QuestionThread publicQuestion = createPublicQuestion();
        QuestionThread privateQuestion = createPrivateQuestion();

        QuestionThreadSummaryResponseDTO publicResponse =
            createPublicQuestionResponse();
        QuestionThreadSummaryResponseDTO privateResponse =
            createPrivateQuestionResponse();

        Page<QuestionThread> repositoryPage = new PageImpl<>(
            List.of(publicQuestion, privateQuestion),
            PageRequest.of(PAGE, SIZE),
            2);

        when(questionAccessPolicy.requireTaskAssignmentForumAccess(TASK_ASSIGNMENT_ID))
            .thenReturn(USER_ID);

        when(questionThreadRepository.findParticipantVisibleQuestions(
            eq(TASK_ASSIGNMENT_ID),
            eq(USER_ID),
            any(Pageable.class))).thenReturn(repositoryPage);

        when(questionThreadMapper.toSummaryResponse(publicQuestion))
            .thenReturn(publicResponse);
        when(questionThreadMapper.toSummaryResponse(privateQuestion))
            .thenReturn(privateResponse);

        Page<QuestionThreadSummaryResponseDTO> result =
            participantForumService.getForumQuestions(
                TASK_ASSIGNMENT_ID,
                PAGE,
                SIZE);

        assertAll(
            () -> assertEquals(
                List.of(publicResponse, privateResponse),
                result.getContent()),
            () -> assertEquals(2, result.getNumberOfElements()),
            () -> assertEquals(2, result.getTotalElements()),
            () -> assertEquals(PAGE, result.getNumber()),
            () -> assertEquals(SIZE, result.getSize()),
            () -> assertTrue(result.isFirst()),
            () -> assertTrue(result.isLast()));

        verify(questionAccessPolicy)
            .requireTaskAssignmentForumAccess(TASK_ASSIGNMENT_ID);
        verify(questionThreadMapper)
            .toSummaryResponse(publicQuestion);
        verify(questionThreadMapper)
            .toSummaryResponse(privateQuestion);
    }

    @Test
    void getForumQuestions_shouldPassCurrentUserIdToRepository() {
        stubAccessibleEmptyForum();

        participantForumService.getForumQuestions(
            TASK_ASSIGNMENT_ID,
            PAGE,
            SIZE);

        verify(questionThreadRepository)
            .findParticipantVisibleQuestions(
                eq(TASK_ASSIGNMENT_ID),
                eq(USER_ID),
                any(Pageable.class));
    }

    @Test
    void getForumQuestions_shouldUseCreatedAtAndIdDescendingSort() {
        stubAccessibleEmptyForum();

        participantForumService.getForumQuestions(
            TASK_ASSIGNMENT_ID,
            PAGE,
            SIZE);

        ArgumentCaptor<Pageable> pageableCaptor =
            ArgumentCaptor.forClass(Pageable.class);

        verify(questionThreadRepository)
            .findParticipantVisibleQuestions(
                eq(TASK_ASSIGNMENT_ID),
                eq(USER_ID),
                pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        List<Sort.Order> orders =
            pageable.getSort().stream().toList();

        assertAll(
            () -> assertEquals(PAGE, pageable.getPageNumber()),
            () -> assertEquals(SIZE, pageable.getPageSize()),
            () -> assertEquals(2, orders.size()),
            () -> assertEquals(
                "createdAt",
                orders.get(0).getProperty()),
            () -> assertEquals(
                Sort.Direction.DESC,
                orders.get(0).getDirection()),
            () -> assertEquals(
                "id",
                orders.get(1).getProperty()),
            () -> assertEquals(
                Sort.Direction.DESC,
                orders.get(1).getDirection()));
    }

    @Test
    void getForumQuestions_emptyResult_shouldReturnEmptyPage() {
        Page<QuestionThread> emptyPage = new PageImpl<>(
            List.of(),
            PageRequest.of(PAGE, SIZE),
            0);

        when(questionAccessPolicy.requireTaskAssignmentForumAccess(TASK_ASSIGNMENT_ID))
            .thenReturn(USER_ID);

        when(questionThreadRepository.findParticipantVisibleQuestions(
            eq(TASK_ASSIGNMENT_ID),
            eq(USER_ID),
            any(Pageable.class))).thenReturn(emptyPage);

        Page<QuestionThreadSummaryResponseDTO> result =
            participantForumService.getForumQuestions(
                TASK_ASSIGNMENT_ID,
                PAGE,
                SIZE);

        assertAll(
            () -> assertTrue(result.isEmpty()),
            () -> assertEquals(0, result.getTotalElements()),
            () -> assertEquals(0, result.getTotalPages()),
            () -> assertEquals(PAGE, result.getNumber()),
            () -> assertEquals(SIZE, result.getSize()));

        verifyNoInteractions(questionThreadMapper);
    }

    @Test
    void getForumQuestions_unauthenticated_shouldNotQueryRepository() {
        AuthenticationException exception =
            new AuthenticationException(
                "Authentication is required to access the question forum",
                ErrorCode.AUTHENTICATION_REQUIRED);

        when(questionAccessPolicy.requireTaskAssignmentForumAccess(TASK_ASSIGNMENT_ID))
            .thenThrow(exception);

        assertThrows(
            AuthenticationException.class,
            () -> participantForumService.getForumQuestions(
                TASK_ASSIGNMENT_ID,
                PAGE,
                SIZE));

        verify(questionAccessPolicy)
            .requireTaskAssignmentForumAccess(TASK_ASSIGNMENT_ID);
        verifyNoInteractions(
            questionThreadRepository,
            questionThreadMapper);
    }

    @Test
    void getForumQuestions_missingTaskAssignment_shouldNotQueryRepository() {
        when(questionAccessPolicy.requireTaskAssignmentForumAccess(TASK_ASSIGNMENT_ID))
            .thenThrow(new TaskAssignmentNotFoundException(TASK_ASSIGNMENT_ID));

        assertThrows(
            TaskAssignmentNotFoundException.class,
            () -> participantForumService.getForumQuestions(
                TASK_ASSIGNMENT_ID,
                PAGE,
                SIZE));

        verify(questionAccessPolicy)
            .requireTaskAssignmentForumAccess(TASK_ASSIGNMENT_ID);
        verifyNoInteractions(
            questionThreadRepository,
            questionThreadMapper);
    }

    @Test
    void getForumQuestions_invalidTaskAssignmentId_shouldRejectBeforeAccessCheck() {
        assertAll(
            () -> assertThrows(
                ValidationException.class,
                () -> participantForumService.getForumQuestions(
                    null,
                    PAGE,
                    SIZE)),
            () -> assertThrows(
                ValidationException.class,
                () -> participantForumService.getForumQuestions(
                    0L,
                    PAGE,
                    SIZE)),
            () -> assertThrows(
                ValidationException.class,
                () -> participantForumService.getForumQuestions(
                    -1L,
                    PAGE,
                    SIZE)));

        verifyNoInteractions(
            questionAccessPolicy,
            questionThreadRepository,
            questionThreadMapper);
    }

    @Test
    void getForumQuestions_negativePage_shouldRejectBeforeAccessCheck() {
        assertThrows(
            ValidationException.class,
            () -> participantForumService.getForumQuestions(
                TASK_ASSIGNMENT_ID,
                -1,
                SIZE));

        verifyNoInteractions(
            questionAccessPolicy,
            questionThreadRepository,
            questionThreadMapper);
    }

    @Test
    void getForumQuestions_invalidSize_shouldRejectBeforeAccessCheck() {
        assertAll(
            () -> assertThrows(
                ValidationException.class,
                () -> participantForumService.getForumQuestions(
                    TASK_ASSIGNMENT_ID,
                    PAGE,
                    0)),
            () -> assertThrows(
                ValidationException.class,
                () -> participantForumService.getForumQuestions(
                    TASK_ASSIGNMENT_ID,
                    PAGE,
                    101)));

        verifyNoInteractions(
            questionAccessPolicy,
            questionThreadRepository,
            questionThreadMapper);
    }

    @Test
    void createQuestion_accessibleTaskAssignment_shouldSaveAndReturnMappedResponse() {
        CreateQuestionRequestDTO request = createQuestionRequest();
        QuestionThread mappedQuestion = createMappedQuestion();
        QuestionThread savedQuestion = createSavedQuestion();
        QuestionThreadResponseDTO expectedResponse =
            createQuestionResponse();

        when(questionAccessPolicy.requireTaskAssignmentQuestionCreationAccess(TASK_ASSIGNMENT_ID))
            .thenReturn(USER_ID);
        when(questionThreadMapper.toEntity(request))
            .thenReturn(mappedQuestion);
        when(questionThreadRepository.save(mappedQuestion))
            .thenReturn(savedQuestion);
        when(questionThreadMapper.toResponse(savedQuestion))
            .thenReturn(expectedResponse);

        QuestionThreadResponseDTO result =
            participantForumService.createQuestion(
                TASK_ASSIGNMENT_ID,
                request);

        assertSame(expectedResponse, result);

        InOrder inOrder = inOrder(
            questionAccessPolicy,
            questionThreadMapper,
            questionThreadRepository);

        inOrder.verify(questionAccessPolicy)
            .requireTaskAssignmentQuestionCreationAccess(TASK_ASSIGNMENT_ID);
        inOrder.verify(questionThreadMapper)
            .toEntity(request);
        inOrder.verify(questionThreadRepository)
            .save(mappedQuestion);
        inOrder.verify(questionThreadMapper)
            .toResponse(savedQuestion);
    }

    @Test
    void createQuestion_shouldUseTaskAssignmentIdFromPath() {
        CreateQuestionRequestDTO request = createQuestionRequest();
        QuestionThread mappedQuestion = createMappedQuestion();

        stubSuccessfulCreation(
            request,
            mappedQuestion,
            createSavedQuestion(),
            createQuestionResponse());

        participantForumService.createQuestion(TASK_ASSIGNMENT_ID, request);

        ArgumentCaptor<QuestionThread> captor =
            ArgumentCaptor.forClass(QuestionThread.class);

        verify(questionThreadRepository).save(captor.capture());

        assertEquals(TASK_ASSIGNMENT_ID, captor.getValue().getTaskAssignmentId());
    }

    @Test
    void createQuestion_shouldUseCurrentUserAsAuthor() {
        CreateQuestionRequestDTO request = createQuestionRequest();
        QuestionThread mappedQuestion = createMappedQuestion();

        stubSuccessfulCreation(
            request,
            mappedQuestion,
            createSavedQuestion(),
            createQuestionResponse());

        participantForumService.createQuestion(TASK_ASSIGNMENT_ID, request);

        ArgumentCaptor<QuestionThread> captor =
            ArgumentCaptor.forClass(QuestionThread.class);

        verify(questionThreadRepository).save(captor.capture());

        assertEquals(USER_ID, captor.getValue().getAuthorId());
    }

    @Test
    void createQuestion_shouldApplyServerControlledDefaults() {
        CreateQuestionRequestDTO request = createQuestionRequest();
        QuestionThread mappedQuestion = createMappedQuestion();

        stubSuccessfulCreation(
            request,
            mappedQuestion,
            createSavedQuestion(),
            createQuestionResponse());

        participantForumService.createQuestion(TASK_ASSIGNMENT_ID, request);

        ArgumentCaptor<QuestionThread> captor =
            ArgumentCaptor.forClass(QuestionThread.class);

        verify(questionThreadRepository).save(captor.capture());

        QuestionThread persistedQuestion = captor.getValue();

        assertEquals(
            QuestionStatus.NEW,
            persistedQuestion.getStatus());
        assertEquals(
            QuestionState.OPEN,
            persistedQuestion.getState());
        assertEquals(
            QuestionVisibility.PRIVATE,
            persistedQuestion.getVisibility());
        assertEquals(0L, persistedQuestion.getVersion());
    }

    @Test
    void createQuestion_shouldLeaveReviewerUnassigned() {
        CreateQuestionRequestDTO request = createQuestionRequest();
        QuestionThread mappedQuestion = createMappedQuestion();

        mappedQuestion.setAssignedReviewerId(999L);

        stubSuccessfulCreation(
            request,
            mappedQuestion,
            createSavedQuestion(),
            createQuestionResponse());

        participantForumService.createQuestion(TASK_ASSIGNMENT_ID, request);

        ArgumentCaptor<QuestionThread> captor =
            ArgumentCaptor.forClass(QuestionThread.class);

        verify(questionThreadRepository).save(captor.capture());

        assertNull(
            captor.getValue().getAssignedReviewerId());
    }

    @Test
    void createQuestion_shouldMapSavedEntityToResponse() {
        CreateQuestionRequestDTO request = createQuestionRequest();
        QuestionThread mappedQuestion = createMappedQuestion();
        QuestionThread savedQuestion = createSavedQuestion();
        QuestionThreadResponseDTO expectedResponse =
            createQuestionResponse();

        stubSuccessfulCreation(
            request,
            mappedQuestion,
            savedQuestion,
            expectedResponse);

        QuestionThreadResponseDTO result =
            participantForumService.createQuestion(
                TASK_ASSIGNMENT_ID,
                request);

        verify(questionThreadMapper).toResponse(savedQuestion);
        assertSame(expectedResponse, result);
    }

    @Test
    void createQuestion_unauthenticated_shouldNotPersist() {
        CreateQuestionRequestDTO request = createQuestionRequest();

        when(questionAccessPolicy.requireTaskAssignmentQuestionCreationAccess(TASK_ASSIGNMENT_ID))
            .thenThrow(new AuthenticationException(
                "Authentication is required to access the question forum",
                ErrorCode.AUTHENTICATION_REQUIRED));

        assertThrows(
            AuthenticationException.class,
            () -> participantForumService.createQuestion(
                TASK_ASSIGNMENT_ID,
                request));

        verify(questionAccessPolicy)
            .requireTaskAssignmentQuestionCreationAccess(TASK_ASSIGNMENT_ID);

        verifyNoInteractions(
            questionThreadRepository,
            questionThreadMapper);
    }

    @Test
    void createQuestion_missingTaskAssignment_shouldNotPersist() {
        CreateQuestionRequestDTO request = createQuestionRequest();

        when(questionAccessPolicy.requireTaskAssignmentQuestionCreationAccess(TASK_ASSIGNMENT_ID))
            .thenThrow(new TaskAssignmentNotFoundException(TASK_ASSIGNMENT_ID));

        assertThrows(
            TaskAssignmentNotFoundException.class,
            () -> participantForumService.createQuestion(
                TASK_ASSIGNMENT_ID,
                request));

        verify(questionAccessPolicy)
            .requireTaskAssignmentQuestionCreationAccess(TASK_ASSIGNMENT_ID);

        verifyNoInteractions(
            questionThreadRepository,
            questionThreadMapper);
    }

    @Test
    void createQuestion_invalidTaskAssignmentId_shouldRejectBeforeAccessCheck() {
        CreateQuestionRequestDTO request = createQuestionRequest();

        assertThrows(
            ValidationException.class,
            () -> participantForumService.createQuestion(
                null,
                request));

        assertThrows(
            ValidationException.class,
            () -> participantForumService.createQuestion(
                0L,
                request));

        assertThrows(
            ValidationException.class,
            () -> participantForumService.createQuestion(
                -1L,
                request));

        verifyNoInteractions(
            questionAccessPolicy,
            questionThreadRepository,
            questionThreadMapper);
    }

    @Test
    void createQuestion_repositoryFailure_shouldPropagateException() {
        CreateQuestionRequestDTO request = createQuestionRequest();
        QuestionThread mappedQuestion = createMappedQuestion();

        RuntimeException repositoryFailure =
            new RuntimeException("Database failure");

        when(questionAccessPolicy.requireTaskAssignmentQuestionCreationAccess(TASK_ASSIGNMENT_ID))
            .thenReturn(USER_ID);
        when(questionThreadMapper.toEntity(request))
            .thenReturn(mappedQuestion);
        when(questionThreadRepository.save(mappedQuestion))
            .thenThrow(repositoryFailure);

        RuntimeException result = assertThrows(
            RuntimeException.class,
            () -> participantForumService.createQuestion(
                TASK_ASSIGNMENT_ID,
                request));

        assertSame(repositoryFailure, result);

        verify(questionThreadRepository).save(mappedQuestion);
        verify(questionThreadMapper, never())
            .toResponse(any(QuestionThread.class));
    }

    @Test
    void getForumQuestions_questionsFromAnotherAssignment_shouldNotBeRequested() {
        Pageable expectedPageable = PageRequest.of(
            PAGE,
            SIZE,
            Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")));

        when(questionAccessPolicy.requireTaskAssignmentForumAccess(
            TASK_ASSIGNMENT_ID)).thenReturn(USER_ID);

        when(questionThreadRepository.findParticipantVisibleQuestions(
            TASK_ASSIGNMENT_ID,
            USER_ID,
            expectedPageable)).thenReturn(Page.empty(expectedPageable));

        participantForumService.getForumQuestions(
            TASK_ASSIGNMENT_ID,
            PAGE,
            SIZE);

        verify(questionThreadRepository)
            .findParticipantVisibleQuestions(
                TASK_ASSIGNMENT_ID,
                USER_ID,
                expectedPageable);

        verify(questionThreadRepository, never())
            .findParticipantVisibleQuestions(
                eq(OTHER_TASK_ASSIGNMENT_ID),
                anyLong(),
                any(Pageable.class));
    }

    private CreateQuestionRequestDTO createQuestionRequest() {
        return new CreateQuestionRequestDTO(
            QUESTION_TITLE,
            QUESTION_CONTENT);
    }

    private QuestionThread createMappedQuestion() {
        return QuestionThread.builder()
            .title(QUESTION_TITLE)
            .content(QUESTION_CONTENT)
            .build();
    }

    private QuestionThread createSavedQuestion() {
        return QuestionThread.builder()
            .id(CREATED_QUESTION_ID)
            .taskAssignmentId(TASK_ASSIGNMENT_ID)
            .authorId(USER_ID)
            .assignedReviewerId(null)
            .title(QUESTION_TITLE)
            .content(QUESTION_CONTENT)
            .status(QuestionStatus.NEW)
            .state(QuestionState.OPEN)
            .visibility(QuestionVisibility.PRIVATE)
            .version(0L)
            .createdAt(CREATED_AT)
            .updatedAt(UPDATED_AT)
            .build();
    }

    private QuestionThreadResponseDTO createQuestionResponse() {
        return new QuestionThreadResponseDTO(
            CREATED_QUESTION_ID,
            TASK_ASSIGNMENT_ID,
            USER_ID,
            null,
            QUESTION_TITLE,
            QUESTION_CONTENT,
            QuestionStatus.NEW,
            QuestionVisibility.PRIVATE,
            QuestionState.OPEN,
            0L,
            CREATED_AT,
            UPDATED_AT);
    }

    private void stubSuccessfulCreation(
        CreateQuestionRequestDTO request,
        QuestionThread mappedQuestion,
        QuestionThread savedQuestion,
        QuestionThreadResponseDTO response) {
        when(questionAccessPolicy.requireTaskAssignmentQuestionCreationAccess(TASK_ASSIGNMENT_ID))
            .thenReturn(USER_ID);
        when(questionThreadMapper.toEntity(request))
            .thenReturn(mappedQuestion);
        when(questionThreadRepository.save(mappedQuestion))
            .thenReturn(savedQuestion);
        when(questionThreadMapper.toResponse(savedQuestion))
            .thenReturn(response);
    }

    private void stubAccessibleEmptyForum() {
        when(questionAccessPolicy.requireTaskAssignmentForumAccess(TASK_ASSIGNMENT_ID))
            .thenReturn(USER_ID);

        when(questionThreadRepository.findParticipantVisibleQuestions(
            eq(TASK_ASSIGNMENT_ID),
            eq(USER_ID),
            any(Pageable.class))).thenReturn(Page.empty());
    }

    private QuestionThread createPublicQuestion() {
        Instant createdAt =
            Instant.parse("2026-07-24T10:00:00Z");
        Instant updatedAt =
            Instant.parse("2026-07-24T10:15:00Z");

        return QuestionThread.builder()
            .id(11L)
            .taskAssignmentId(TASK_ASSIGNMENT_ID)
            .authorId(OTHER_USER_ID)
            .title("Public question")
            .content("Public question content")
            .status(NEW)
            .state(OPEN)
            .visibility(PUBLIC)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }

    private QuestionThread createPrivateQuestion() {
        Instant createdAt =
            Instant.parse("2026-07-24T11:00:00Z");
        Instant updatedAt =
            Instant.parse("2026-07-24T11:30:00Z");

        return QuestionThread.builder()
            .id(12L)
            .taskAssignmentId(TASK_ASSIGNMENT_ID)
            .authorId(USER_ID)
            .title("Private question")
            .content("Private question content")
            .status(ANSWERED)
            .state(CLOSED)
            .visibility(PRIVATE)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    }

    private QuestionThreadSummaryResponseDTO createPublicQuestionResponse() {

        return new QuestionThreadSummaryResponseDTO(
            11L,
            TASK_ASSIGNMENT_ID,
            OTHER_USER_ID,
            "Public question",
            NEW,
            PUBLIC,
            OPEN,
            Instant.parse("2026-07-24T10:00:00Z"),
            Instant.parse("2026-07-24T10:15:00Z"));
    }

    private QuestionThreadSummaryResponseDTO createPrivateQuestionResponse() {

        return new QuestionThreadSummaryResponseDTO(
            12L,
            TASK_ASSIGNMENT_ID,
            USER_ID,
            "Private question",
            ANSWERED,
            PRIVATE,
            CLOSED,
            Instant.parse("2026-07-24T11:00:00Z"),
            Instant.parse("2026-07-24T11:30:00Z"));
    }
}