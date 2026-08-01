package com.itasocialacademy.oitassist.chat.service;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType.COMMENT;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType.OFFICIAL_ANSWER;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.CLOSED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.ANSWERED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.chat.dao.dto.request.CreateCommentRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionMessage;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionMessageRepository;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import com.itasocialacademy.oitassist.chat.exceptions.InvalidQuestionStateException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import com.itasocialacademy.oitassist.chat.mapper.QuestionMessageMapper;
import com.itasocialacademy.oitassist.chat.mapper.QuestionThreadMapper;
import com.itasocialacademy.oitassist.chat.utils.QuestionAccessPolicy;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
class ParticipantQuestionServiceImplTest {

    private static final Long QUESTION_ID = 1L;
    private static final Long TASK_ASSIGNMENT_ID = 2L;
    private static final Long AUTHOR_ID = 3L;
    private static final Long REVIEWER_ID = 4L;

    private static final int PAGE = 0;
    private static final int SIZE = 50;

    private static final Long CURRENT_USER_ID = 5L;
    private static final Long COMMENT_ID = 10L;

    private static final String COMMENT_CONTENT =
        "Could you also clarify the memory limit?";

    private static final Instant CREATED_AT =
        Instant.parse("2026-07-27T10:00:00Z");

    private static final Instant UPDATED_AT =
        Instant.parse("2026-07-27T10:05:00Z");

    @Mock
    private QuestionThreadRepository questionThreadRepository;

    @Mock
    private QuestionMessageRepository questionMessageRepository;

    @Mock
    private QuestionThreadMapper questionThreadMapper;

    @Mock
    private QuestionMessageMapper questionMessageMapper;

    @Mock
    private QuestionAccessPolicy questionAccessPolicy;

    @InjectMocks
    private ParticipantQuestionServiceImpl participantQuestionService;

    @Test
    void getQuestionDetails_accessibleQuestion_shouldReturnMappedResponse() {
        QuestionThread question = createQuestion();
        QuestionThreadResponseDTO response =
            createQuestionResponse();

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(question));

        when(questionThreadMapper.toResponse(question))
            .thenReturn(response);

        QuestionThreadResponseDTO result =
            participantQuestionService
                .getQuestionDetails(QUESTION_ID);

        assertSame(response, result);

        InOrder order = inOrder(
            questionThreadRepository,
            questionAccessPolicy,
            questionThreadMapper);

        order.verify(questionThreadRepository)
            .findById(QUESTION_ID);

        order.verify(questionAccessPolicy)
            .requireQuestionViewAccess(question);

        order.verify(questionThreadMapper)
            .toResponse(question);
    }

    @Test
    void getQuestionDetails_missingQuestion_shouldThrowNotFound() {
        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.empty());

        assertThrows(
            QuestionNotFoundException.class,
            () -> participantQuestionService
                .getQuestionDetails(QUESTION_ID));

        verifyNoInteractions(
            questionAccessPolicy,
            questionThreadMapper,
            questionMessageRepository,
            questionMessageMapper);
    }

    @Test
    void getQuestionDetails_accessFailure_shouldNotMapQuestion() {
        QuestionThread question = createQuestion();

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(question));

        doThrow(new QuestionNotFoundException(QUESTION_ID))
            .when(questionAccessPolicy)
            .requireQuestionViewAccess(question);

        assertThrows(
            QuestionNotFoundException.class,
            () -> participantQuestionService
                .getQuestionDetails(QUESTION_ID));

        verify(questionThreadMapper, never())
            .toResponse(any(QuestionThread.class));

        verifyNoInteractions(
            questionMessageRepository,
            questionMessageMapper);
    }

    @Test
    void getQuestionDetails_invalidId_shouldRejectBeforeRepository() {
        assertAll(
            () -> assertThrows(
                ValidationException.class,
                () -> participantQuestionService
                    .getQuestionDetails(null)),
            () -> assertThrows(
                ValidationException.class,
                () -> participantQuestionService
                    .getQuestionDetails(0L)),
            () -> assertThrows(
                ValidationException.class,
                () -> participantQuestionService
                    .getQuestionDetails(-1L)));

        verifyNoInteractions(
            questionThreadRepository,
            questionMessageRepository,
            questionAccessPolicy,
            questionThreadMapper,
            questionMessageMapper);
    }

    @Test
    void getQuestionMessages_accessibleQuestion_shouldReturnMappedPage() {
        QuestionThread question = createQuestion();

        QuestionMessage comment = createMessage(
            10L,
            COMMENT,
            "Participant comment",
            CREATED_AT);

        QuestionMessage officialAnswer = createMessage(
            11L,
            OFFICIAL_ANSWER,
            "Official answer",
            UPDATED_AT);

        QuestionMessageResponseDTO commentResponse =
            createMessageResponse(comment);

        QuestionMessageResponseDTO answerResponse =
            createMessageResponse(officialAnswer);

        Page<QuestionMessage> repositoryPage =
            new PageImpl<>(
                List.of(comment, officialAnswer),
                PageRequest.of(PAGE, SIZE),
                2);

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(question));

        when(questionMessageRepository
            .findAllByQuestionThreadId(
                eq(QUESTION_ID),
                any(Pageable.class)))
            .thenReturn(repositoryPage);

        when(questionMessageMapper.toResponse(comment))
            .thenReturn(commentResponse);

        when(questionMessageMapper.toResponse(officialAnswer))
            .thenReturn(answerResponse);

        Page<QuestionMessageResponseDTO> result =
            participantQuestionService
                .getQuestionMessages(
                    QUESTION_ID,
                    PAGE,
                    SIZE);

        assertAll(
            () -> assertEquals(
                List.of(
                    commentResponse,
                    answerResponse),
                result.getContent()),
            () -> assertEquals(
                COMMENT,
                result.getContent().get(0).type()),
            () -> assertEquals(
                OFFICIAL_ANSWER,
                result.getContent().get(1).type()),
            () -> assertEquals(
                PAGE,
                result.getNumber()),
            () -> assertEquals(
                SIZE,
                result.getSize()),
            () -> assertEquals(
                2,
                result.getTotalElements()),
            () -> assertEquals(
                1,
                result.getTotalPages()));
    }

    @Test
    void getQuestionMessages_shouldValidateAccessBeforeMessageQuery() {
        QuestionThread question = createQuestion();

        Page<QuestionMessage> emptyPage =
            Page.empty(
                PageRequest.of(PAGE, SIZE));

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(question));

        when(questionMessageRepository
            .findAllByQuestionThreadId(
                eq(QUESTION_ID),
                any(Pageable.class)))
            .thenReturn(emptyPage);

        participantQuestionService.getQuestionMessages(
            QUESTION_ID,
            PAGE,
            SIZE);

        InOrder order = inOrder(
            questionThreadRepository,
            questionAccessPolicy,
            questionMessageRepository);

        order.verify(questionThreadRepository)
            .findById(QUESTION_ID);

        order.verify(questionAccessPolicy)
            .requireQuestionViewAccess(question);

        order.verify(questionMessageRepository)
            .findAllByQuestionThreadId(
                eq(QUESTION_ID),
                any(Pageable.class));
    }

    @Test
    void getQuestionMessages_accessFailure_shouldNotQueryOrMapMessages() {
        QuestionThread question = createQuestion();

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(question));

        doThrow(new QuestionNotFoundException(QUESTION_ID))
            .when(questionAccessPolicy)
            .requireQuestionViewAccess(question);

        assertThrows(
            QuestionNotFoundException.class,
            () -> participantQuestionService
                .getQuestionMessages(
                    QUESTION_ID,
                    PAGE,
                    SIZE));

        verifyNoInteractions(
            questionMessageRepository,
            questionMessageMapper);
    }

    @Test
    void getQuestionMessages_missingQuestion_shouldNotQueryOrMapMessages() {
        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.empty());

        assertThrows(
            QuestionNotFoundException.class,
            () -> participantQuestionService
                .getQuestionMessages(
                    QUESTION_ID,
                    PAGE,
                    SIZE));

        verifyNoInteractions(
            questionAccessPolicy,
            questionMessageRepository,
            questionMessageMapper);
    }

    @Test
    void getQuestionMessages_shouldPassExactQuestionIdAndPagination() {
        QuestionThread question = createQuestion();

        Page<QuestionMessage> emptyPage =
            Page.empty(
                PageRequest.of(2, 10));

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(question));

        when(questionMessageRepository
            .findAllByQuestionThreadId(
                eq(QUESTION_ID),
                any(Pageable.class)))
            .thenReturn(emptyPage);

        participantQuestionService.getQuestionMessages(
            QUESTION_ID,
            2,
            10);

        ArgumentCaptor<Pageable> pageableCaptor =
            ArgumentCaptor.forClass(Pageable.class);

        verify(questionMessageRepository)
            .findAllByQuestionThreadId(
                eq(QUESTION_ID),
                pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();

        assertAll(
            () -> assertEquals(
                2,
                pageable.getPageNumber()),
            () -> assertEquals(
                10,
                pageable.getPageSize()));
    }

    @Test
    void getQuestionMessages_shouldUseDeterministicChronologicalSort() {
        QuestionThread question = createQuestion();

        Page<QuestionMessage> emptyPage =
            Page.empty(
                PageRequest.of(PAGE, SIZE));

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(question));

        when(questionMessageRepository
            .findAllByQuestionThreadId(
                eq(QUESTION_ID),
                any(Pageable.class)))
            .thenReturn(emptyPage);

        participantQuestionService.getQuestionMessages(
            QUESTION_ID,
            PAGE,
            SIZE);

        ArgumentCaptor<Pageable> pageableCaptor =
            ArgumentCaptor.forClass(Pageable.class);

        verify(questionMessageRepository)
            .findAllByQuestionThreadId(
                eq(QUESTION_ID),
                pageableCaptor.capture());

        List<Sort.Order> orders =
            pageableCaptor.getValue()
                .getSort()
                .stream()
                .toList();

        assertAll(
            () -> assertEquals(
                2,
                orders.size()),
            () -> assertEquals(
                "createdAt",
                orders.get(0).getProperty()),
            () -> assertEquals(
                Sort.Direction.ASC,
                orders.get(0).getDirection()),
            () -> assertEquals(
                "id",
                orders.get(1).getProperty()),
            () -> assertEquals(
                Sort.Direction.ASC,
                orders.get(1).getDirection()));
    }

    @Test
    void getQuestionMessages_emptyHistory_shouldReturnEmptyPage() {
        QuestionThread question = createQuestion();

        Page<QuestionMessage> emptyPage =
            Page.empty(
                PageRequest.of(PAGE, SIZE));

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(question));

        when(questionMessageRepository
            .findAllByQuestionThreadId(
                eq(QUESTION_ID),
                any(Pageable.class)))
            .thenReturn(emptyPage);

        Page<QuestionMessageResponseDTO> result =
            participantQuestionService
                .getQuestionMessages(
                    QUESTION_ID,
                    PAGE,
                    SIZE);

        assertAll(
            () -> assertEquals(
                0,
                result.getTotalElements()),
            () -> assertEquals(
                PAGE,
                result.getNumber()),
            () -> assertEquals(
                SIZE,
                result.getSize()));

        verifyNoInteractions(questionMessageMapper);
    }

    @Test
    void getQuestionMessages_invalidQuestionId_shouldRejectBeforeRepositories() {
        assertAll(
            () -> assertThrows(
                ValidationException.class,
                () -> participantQuestionService
                    .getQuestionMessages(
                        null,
                        PAGE,
                        SIZE)),
            () -> assertThrows(
                ValidationException.class,
                () -> participantQuestionService
                    .getQuestionMessages(
                        0L,
                        PAGE,
                        SIZE)),
            () -> assertThrows(
                ValidationException.class,
                () -> participantQuestionService
                    .getQuestionMessages(
                        -1L,
                        PAGE,
                        SIZE)));

        verifyNoInteractions(
            questionThreadRepository,
            questionMessageRepository,
            questionAccessPolicy,
            questionMessageMapper);
    }

    @Test
    void getQuestionMessages_invalidPagination_shouldRejectBeforeRepositories() {
        assertAll(
            () -> assertThrows(
                ValidationException.class,
                () -> participantQuestionService
                    .getQuestionMessages(
                        QUESTION_ID,
                        -1,
                        SIZE)),
            () -> assertThrows(
                ValidationException.class,
                () -> participantQuestionService
                    .getQuestionMessages(
                        QUESTION_ID,
                        PAGE,
                        0)),
            () -> assertThrows(
                ValidationException.class,
                () -> participantQuestionService
                    .getQuestionMessages(
                        QUESTION_ID,
                        PAGE,
                        101)));

        verifyNoInteractions(
            questionThreadRepository,
            questionMessageRepository,
            questionAccessPolicy,
            questionMessageMapper);
    }

    @Test
    void addComment_openAccessibleQuestion_shouldPersistServerControlledCommentAndReturnResponse() {
        QuestionThread question = createQuestion();
        question.setState(OPEN);

        CreateCommentRequestDTO request =
            new CreateCommentRequestDTO(
                COMMENT_CONTENT);

        /*
         * Simulates a polluted mapper result to verify that the service overwrites
         * every protected field.
         */
        QuestionMessage mappedComment =
            QuestionMessage.builder()
                .id(999L)
                .questionThreadId(999L)
                .authorId(999L)
                .type(OFFICIAL_ANSWER)
                .content(COMMENT_CONTENT)
                .createdAt(CREATED_AT)
                .build();

        QuestionMessage savedComment =
            QuestionMessage.builder()
                .id(COMMENT_ID)
                .questionThreadId(QUESTION_ID)
                .authorId(CURRENT_USER_ID)
                .type(COMMENT)
                .content(COMMENT_CONTENT)
                .createdAt(CREATED_AT)
                .build();

        QuestionMessageResponseDTO response =
            new QuestionMessageResponseDTO(
                COMMENT_ID,
                QUESTION_ID,
                CURRENT_USER_ID,
                COMMENT,
                COMMENT_CONTENT,
                CREATED_AT);

        var originalStatus = question.getStatus();
        var originalState = question.getState();
        var originalVisibility = question.getVisibility();
        var originalReviewer = question.getAssignedReviewerId();
        var originalVersion = question.getVersion();

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(question));

        when(questionAccessPolicy
            .requireQuestionCommentAccess(question))
            .thenReturn(CURRENT_USER_ID);

        when(questionMessageMapper.toEntity(request))
            .thenReturn(mappedComment);

        when(questionMessageRepository.save(mappedComment))
            .thenReturn(savedComment);

        when(questionMessageMapper.toResponse(savedComment))
            .thenReturn(response);

        QuestionMessageResponseDTO result =
            participantQuestionService.addComment(
                QUESTION_ID,
                request);

        ArgumentCaptor<QuestionMessage> messageCaptor =
            ArgumentCaptor.forClass(
                QuestionMessage.class);

        verify(questionMessageRepository)
            .save(messageCaptor.capture());

        QuestionMessage persistedComment =
            messageCaptor.getValue();

        assertAll(
            () -> assertSame(response, result),
            () -> assertNull(persistedComment.getId()),
            () -> assertEquals(
                QUESTION_ID,
                persistedComment.getQuestionThreadId()),
            () -> assertEquals(
                CURRENT_USER_ID,
                persistedComment.getAuthorId()),
            () -> assertEquals(
                COMMENT,
                persistedComment.getType()),
            () -> assertEquals(
                COMMENT_CONTENT,
                persistedComment.getContent()),
            () -> assertNull(
                persistedComment.getCreatedAt()),
            () -> assertEquals(
                originalStatus,
                question.getStatus()),
            () -> assertEquals(
                originalState,
                question.getState()),
            () -> assertEquals(
                originalVisibility,
                question.getVisibility()),
            () -> assertEquals(
                originalReviewer,
                question.getAssignedReviewerId()),
            () -> assertEquals(
                originalVersion,
                question.getVersion()));

        InOrder order = inOrder(
            questionThreadRepository,
            questionAccessPolicy,
            questionMessageMapper,
            questionMessageRepository);

        order.verify(questionThreadRepository)
            .findById(QUESTION_ID);

        order.verify(questionAccessPolicy)
            .requireQuestionCommentAccess(question);

        order.verify(questionMessageMapper)
            .toEntity(request);

        order.verify(questionMessageRepository)
            .save(mappedComment);

        order.verify(questionMessageMapper)
            .toResponse(savedComment);

        verify(questionThreadRepository, never())
            .save(any(QuestionThread.class));
    }

    @Test
    void addComment_missingQuestion_shouldNotCheckAccessOrPersist() {
        CreateCommentRequestDTO request =
            new CreateCommentRequestDTO(
                COMMENT_CONTENT);

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.empty());

        assertThrows(
            QuestionNotFoundException.class,
            () -> participantQuestionService
                .addComment(
                    QUESTION_ID,
                    request));

        verifyNoInteractions(
            questionAccessPolicy,
            questionMessageRepository,
            questionMessageMapper);
    }

    @Test
    void addComment_accessFailure_shouldNotMapOrPersistComment() {
        QuestionThread question = createQuestion();
        question.setState(OPEN);

        CreateCommentRequestDTO request =
            new CreateCommentRequestDTO(
                COMMENT_CONTENT);

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(question));

        when(questionAccessPolicy
            .requireQuestionCommentAccess(question))
            .thenThrow(
                new QuestionNotFoundException(
                    QUESTION_ID));

        assertThrows(
            QuestionNotFoundException.class,
            () -> participantQuestionService
                .addComment(
                    QUESTION_ID,
                    request));

        verifyNoInteractions(
            questionMessageRepository,
            questionMessageMapper);

        verify(questionThreadRepository, never())
            .save(any(QuestionThread.class));
    }

    @Test
    void addComment_closedQuestion_shouldThrowInvalidStateBeforeMappingOrPersistence() {
        QuestionThread question = createQuestion();
        question.setState(CLOSED);

        CreateCommentRequestDTO request =
            new CreateCommentRequestDTO(
                COMMENT_CONTENT);

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(question));

        when(questionAccessPolicy
            .requireQuestionCommentAccess(question))
            .thenReturn(CURRENT_USER_ID);

        InvalidQuestionStateException exception =
            assertThrows(
                InvalidQuestionStateException.class,
                () -> participantQuestionService
                    .addComment(
                        QUESTION_ID,
                        request));

        assertEquals(
            ErrorCode.QUESTION_INVALID_STATE,
            exception.getErrorCode());

        verifyNoInteractions(
            questionMessageRepository,
            questionMessageMapper);

        verify(questionThreadRepository, never())
            .save(any(QuestionThread.class));
    }

    @Test
    void addComment_invalidQuestionId_shouldRejectBeforeRepositories() {
        CreateCommentRequestDTO request =
            new CreateCommentRequestDTO(
                COMMENT_CONTENT);

        assertAll(
            () -> assertThrows(
                ValidationException.class,
                () -> participantQuestionService
                    .addComment(null, request)),
            () -> assertThrows(
                ValidationException.class,
                () -> participantQuestionService
                    .addComment(0L, request)),
            () -> assertThrows(
                ValidationException.class,
                () -> participantQuestionService
                    .addComment(-1L, request)));

        verifyNoInteractions(
            questionThreadRepository,
            questionMessageRepository,
            questionAccessPolicy,
            questionMessageMapper);
    }

    @Test
    void addComment_repositoryFailure_shouldPropagateAndNotMapResponse() {
        QuestionThread question = createQuestion();
        question.setState(OPEN);

        CreateCommentRequestDTO request =
            new CreateCommentRequestDTO(
                COMMENT_CONTENT);

        QuestionMessage mappedComment =
            QuestionMessage.builder()
                .content(COMMENT_CONTENT)
                .build();

        RuntimeException repositoryFailure =
            new RuntimeException(
                "Database failure");

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(question));

        when(questionAccessPolicy
            .requireQuestionCommentAccess(question))
            .thenReturn(CURRENT_USER_ID);

        when(questionMessageMapper.toEntity(request))
            .thenReturn(mappedComment);

        when(questionMessageRepository.save(mappedComment))
            .thenThrow(repositoryFailure);

        RuntimeException result =
            assertThrows(
                RuntimeException.class,
                () -> participantQuestionService
                    .addComment(
                        QUESTION_ID,
                        request));

        assertSame(repositoryFailure, result);

        verify(questionMessageMapper, never())
            .toResponse(any(QuestionMessage.class));

        verify(questionThreadRepository, never())
            .save(any(QuestionThread.class));
    }

    private QuestionThread createQuestion() {
        return QuestionThread.builder()
            .id(QUESTION_ID)
            .taskAssignmentId(TASK_ASSIGNMENT_ID)
            .authorId(AUTHOR_ID)
            .assignedReviewerId(REVIEWER_ID)
            .title("Question title")
            .content("Question content")
            .status(ANSWERED)
            .visibility(PRIVATE)
            .state(CLOSED)
            .version(2L)
            .createdAt(CREATED_AT)
            .updatedAt(UPDATED_AT)
            .build();
    }

    private QuestionThreadResponseDTO createQuestionResponse() {

        return new QuestionThreadResponseDTO(
            QUESTION_ID,
            TASK_ASSIGNMENT_ID,
            AUTHOR_ID,
            REVIEWER_ID,
            "Question title",
            "Question content",
            ANSWERED,
            PRIVATE,
            CLOSED,
            2L,
            CREATED_AT,
            UPDATED_AT);
    }

    private QuestionMessage createMessage(
        Long id,
        QuestionMessageType type,
        String content,
        Instant createdAt) {

        return QuestionMessage.builder()
            .id(id)
            .questionThreadId(QUESTION_ID)
            .authorId(AUTHOR_ID)
            .type(type)
            .content(content)
            .createdAt(createdAt)
            .build();
    }

    private QuestionMessageResponseDTO createMessageResponse(
        QuestionMessage message) {

        return new QuestionMessageResponseDTO(
            message.getId(),
            message.getQuestionThreadId(),
            message.getAuthorId(),
            message.getType(),
            message.getContent(),
            message.getCreatedAt());
    }
}