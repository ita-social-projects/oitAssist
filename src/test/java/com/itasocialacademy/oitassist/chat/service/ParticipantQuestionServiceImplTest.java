package com.itasocialacademy.oitassist.chat.service;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType.COMMENT;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType.OFFICIAL_ANSWER;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.CLOSED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.IN_REVIEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static com.itasocialacademy.oitassist.core.config.PaginationConfig.MAX_PAGE_SIZE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.itasocialacademy.oitassist.chat.event.domain.CommentCreatedDomainEvent;
import com.itasocialacademy.oitassist.chat.exceptions.InvalidQuestionStateException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionForumAccessRestrictedException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import com.itasocialacademy.oitassist.chat.mapper.QuestionMessageMapper;
import com.itasocialacademy.oitassist.chat.mapper.QuestionThreadMapper;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class ParticipantQuestionServiceImplTest {

    private static final Long QUESTION_ID = 11L;
    private static final Long TASK_ASSIGNMENT_ID = 1L;
    private static final Long AUTHOR_ID = 100L;
    private static final Long REVIEWER_ID = 200L;

    private static final int PAGE = 0;
    private static final int SIZE = 50;

    private static final Instant CREATED_AT = Instant.parse("2026-07-24T10:00:00Z");
    private static final Instant ANSWERED_AT = Instant.parse("2026-07-24T11:00:00Z");

    private static final Long COMMENT_ID = 31L;
    private static final Long COMMENTER_ID = 150L;
    private static final String COMMENT_CONTENT = "Could you also clarify the memory limit?";

    @Mock
    private QuestionThreadRepository questionThreadRepository;

    @Mock
    private QuestionMessageRepository questionMessageRepository;

    @Mock
    private QuestionThreadMapper questionThreadMapper;

    @Mock
    private QuestionMessageMapper questionMessageMapper;

    @Mock
    private ForumAccessService forumAccessService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private ParticipantQuestionServiceImpl participantQuestionService;

    @Test
    void getQuestionDetails_accessibleQuestion_shouldCheckAccessBeforeMapping() {
        QuestionThread question = createQuestion();
        QuestionThreadResponseDTO response = createQuestionResponse(question);

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(question));
        when(questionThreadMapper.toResponse(question))
            .thenReturn(response);

        QuestionThreadResponseDTO result =
            participantQuestionService.getQuestionDetails(QUESTION_ID);

        assertSame(response, result);

        InOrder inOrder = inOrder(
            questionThreadRepository,
            forumAccessService,
            questionThreadMapper);

        inOrder.verify(questionThreadRepository).findById(QUESTION_ID);
        inOrder.verify(forumAccessService).requireQuestionViewAccess(question);
        inOrder.verify(questionThreadMapper).toResponse(question);
    }

    @Test
    void getQuestionDetails_closedQuestion_shouldRemainReadable() {
        QuestionThread question = createQuestion();
        question.setState(CLOSED);

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(question));
        when(questionThreadMapper.toResponse(question))
            .thenReturn(createQuestionResponse(question));

        QuestionThreadResponseDTO result =
            participantQuestionService.getQuestionDetails(QUESTION_ID);

        assertEquals(CLOSED, result.state());
    }

    @Test
    void getQuestionDetails_maskedQuestion_shouldNotMapQuestion() {
        QuestionThread question = createQuestion();

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(question));
        doThrow(new QuestionNotFoundException(QUESTION_ID))
            .when(forumAccessService).requireQuestionViewAccess(question);

        assertThrows(
            QuestionNotFoundException.class,
            () -> participantQuestionService.getQuestionDetails(QUESTION_ID));

        verifyNoInteractions(questionThreadMapper);
    }

    @Test
    void getQuestionDetails_missingQuestion_shouldThrowNotFoundWithoutAccessCheck() {
        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.empty());

        assertThrows(
            QuestionNotFoundException.class,
            () -> participantQuestionService.getQuestionDetails(QUESTION_ID));

        verifyNoInteractions(
            forumAccessService,
            questionThreadMapper);
    }

    @Test
    void getQuestionDetails_invalidQuestionId_shouldRejectBeforeLoading() {
        assertAll(
            () -> assertThrows(
                ValidationException.class,
                () -> participantQuestionService.getQuestionDetails(null)),
            () -> assertThrows(
                ValidationException.class,
                () -> participantQuestionService.getQuestionDetails(0L)),
            () -> assertThrows(
                ValidationException.class,
                () -> participantQuestionService.getQuestionDetails(-1L)));

        verifyNoInteractions(
            questionThreadRepository,
            forumAccessService,
            questionThreadMapper);
    }

    @Test
    void getQuestionMessages_accessibleQuestion_shouldReturnHistoryWithAllMessageTypes() {
        QuestionMessage comment = createMessage(21L, COMMENT, AUTHOR_ID, CREATED_AT);
        QuestionMessage officialAnswer = createMessage(22L, OFFICIAL_ANSWER, REVIEWER_ID, ANSWERED_AT);

        QuestionMessageResponseDTO commentResponse = createMessageResponse(comment);
        QuestionMessageResponseDTO answerResponse = createMessageResponse(officialAnswer);

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(createQuestion()));
        when(questionMessageRepository.findAllByQuestionThreadId(
            eq(QUESTION_ID),
            any(Pageable.class))).thenReturn(new PageImpl<>(
                List.of(comment, officialAnswer),
                PageRequest.of(PAGE, SIZE),
                2));
        when(questionMessageMapper.toResponse(comment))
            .thenReturn(commentResponse);
        when(questionMessageMapper.toResponse(officialAnswer))
            .thenReturn(answerResponse);

        Page<QuestionMessageResponseDTO> result =
            participantQuestionService.getQuestionMessages(QUESTION_ID, PAGE, SIZE);

        assertAll(
            () -> assertEquals(
                List.of(commentResponse, answerResponse),
                result.getContent()),
            () -> assertEquals(COMMENT, result.getContent().get(0).type()),
            () -> assertEquals(OFFICIAL_ANSWER, result.getContent().get(1).type()),
            () -> assertEquals(2, result.getTotalElements()),
            () -> assertEquals(PAGE, result.getNumber()),
            () -> assertEquals(SIZE, result.getSize()));
    }

    @Test
    void getQuestionMessages_shouldOrderByCreatedAtThenIdAscending() {
        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(createQuestion()));
        when(questionMessageRepository.findAllByQuestionThreadId(
            eq(QUESTION_ID),
            any(Pageable.class))).thenReturn(Page.empty());

        participantQuestionService.getQuestionMessages(QUESTION_ID, PAGE, SIZE);

        ArgumentCaptor<Pageable> pageableCaptor =
            ArgumentCaptor.forClass(Pageable.class);

        verify(questionMessageRepository).findAllByQuestionThreadId(
            eq(QUESTION_ID),
            pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        List<Sort.Order> orders = pageable.getSort().stream().toList();

        assertAll(
            () -> assertEquals(PAGE, pageable.getPageNumber()),
            () -> assertEquals(SIZE, pageable.getPageSize()),
            () -> assertEquals(2, orders.size()),
            () -> assertEquals("createdAt", orders.get(0).getProperty()),
            () -> assertEquals(Sort.Direction.ASC, orders.get(0).getDirection()),
            () -> assertEquals("id", orders.get(1).getProperty()),
            () -> assertEquals(Sort.Direction.ASC, orders.get(1).getDirection()));
    }

    @Test
    void getQuestionMessages_shouldCheckAccessBeforeLoadingMessages() {
        QuestionThread question = createQuestion();

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(question));
        when(questionMessageRepository.findAllByQuestionThreadId(
            eq(QUESTION_ID),
            any(Pageable.class))).thenReturn(Page.empty());

        participantQuestionService.getQuestionMessages(QUESTION_ID, PAGE, SIZE);

        InOrder inOrder = inOrder(
            forumAccessService,
            questionMessageRepository);

        inOrder.verify(forumAccessService).requireQuestionViewAccess(question);
        inOrder.verify(questionMessageRepository).findAllByQuestionThreadId(
            eq(QUESTION_ID),
            any(Pageable.class));
    }

    @Test
    void getQuestionMessages_maskedQuestion_shouldNotLoadMessages() {
        QuestionThread question = createQuestion();

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(question));
        doThrow(new QuestionNotFoundException(QUESTION_ID))
            .when(forumAccessService).requireQuestionViewAccess(question);

        assertThrows(
            QuestionNotFoundException.class,
            () -> participantQuestionService.getQuestionMessages(QUESTION_ID, PAGE, SIZE));

        verifyNoInteractions(
            questionMessageRepository,
            questionMessageMapper);
    }

    @Test
    void getQuestionMessages_restrictedForum_shouldNotLoadMessages() {
        QuestionThread question = createQuestion();

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(question));
        doThrow(new QuestionForumAccessRestrictedException(TASK_ASSIGNMENT_ID))
            .when(forumAccessService).requireQuestionViewAccess(question);

        assertThrows(
            QuestionForumAccessRestrictedException.class,
            () -> participantQuestionService.getQuestionMessages(QUESTION_ID, PAGE, SIZE));

        verifyNoInteractions(
            questionMessageRepository,
            questionMessageMapper);
    }

    @Test
    void getQuestionMessages_missingQuestion_shouldNotLoadMessages() {
        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.empty());

        assertThrows(
            QuestionNotFoundException.class,
            () -> participantQuestionService.getQuestionMessages(QUESTION_ID, PAGE, SIZE));

        verifyNoInteractions(
            forumAccessService,
            questionMessageRepository,
            questionMessageMapper);
    }

    @Test
    void getQuestionMessages_invalidPagination_shouldRejectBeforeAccessCheck() {
        assertAll(
            () -> assertThrows(
                ValidationException.class,
                () -> participantQuestionService.getQuestionMessages(QUESTION_ID, -1, SIZE)),
            () -> assertThrows(
                ValidationException.class,
                () -> participantQuestionService.getQuestionMessages(QUESTION_ID, PAGE, 0)),
            () -> assertThrows(
                ValidationException.class,
                () -> participantQuestionService.getQuestionMessages(QUESTION_ID, PAGE, MAX_PAGE_SIZE + 1)));

        verifyNoInteractions(
            questionThreadRepository,
            forumAccessService,
            questionMessageRepository);
    }

    @Test
    void addComment_accessibleOpenQuestion_shouldPersistCommentWithServerControlledFields() {
        QuestionThread question = createQuestion();
        CreateCommentRequestDTO request = new CreateCommentRequestDTO(COMMENT_CONTENT);

        QuestionMessage mappedComment = QuestionMessage.builder()
            .id(999L)
            .questionThreadId(999L)
            .authorId(999L)
            .type(OFFICIAL_ANSWER)
            .content(COMMENT_CONTENT)
            .createdAt(ANSWERED_AT)
            .build();

        stubCommentCreation(question, request, mappedComment);

        participantQuestionService.addComment(QUESTION_ID, request);

        ArgumentCaptor<QuestionMessage> commentCaptor =
            ArgumentCaptor.forClass(QuestionMessage.class);

        verify(questionMessageRepository).save(commentCaptor.capture());

        QuestionMessage persistedComment = commentCaptor.getValue();

        assertAll(
            () -> assertNull(persistedComment.getId()),
            () -> assertEquals(QUESTION_ID, persistedComment.getQuestionThreadId()),
            () -> assertEquals(COMMENTER_ID, persistedComment.getAuthorId()),
            () -> assertEquals(COMMENT, persistedComment.getType()),
            () -> assertEquals(COMMENT_CONTENT, persistedComment.getContent()),
            () -> assertNull(persistedComment.getCreatedAt()));
    }

    @Test
    void addComment_accessibleOpenQuestion_shouldReturnSavedCommentAndPublishEventAfterSaving() {
        QuestionThread question = createQuestion();
        CreateCommentRequestDTO request = new CreateCommentRequestDTO(COMMENT_CONTENT);
        QuestionMessage mappedComment = QuestionMessage.builder()
            .content(COMMENT_CONTENT)
            .build();

        stubCommentCreation(question, request, mappedComment);

        QuestionMessageResponseDTO result =
            participantQuestionService.addComment(QUESTION_ID, request);

        assertAll(
            () -> assertEquals(COMMENT_ID, result.id()),
            () -> assertEquals(COMMENTER_ID, result.authorId()),
            () -> assertEquals(COMMENT, result.type()));

        ArgumentCaptor<CommentCreatedDomainEvent> eventCaptor =
            ArgumentCaptor.forClass(CommentCreatedDomainEvent.class);

        InOrder inOrder = inOrder(
            questionMessageRepository,
            applicationEventPublisher);

        inOrder.verify(questionMessageRepository).save(mappedComment);
        inOrder.verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

        assertAll(
            () -> assertSame(result, eventCaptor.getValue().message()),
            () -> assertEquals(QUESTION_ID, eventCaptor.getValue().question().id()));
    }

    @Test
    void addComment_accessibleOpenQuestion_shouldNotChangeQuestionWorkflowFields() {
        QuestionThread question = createQuestion();
        CreateCommentRequestDTO request = new CreateCommentRequestDTO(COMMENT_CONTENT);

        stubCommentCreation(
            question,
            request,
            QuestionMessage.builder()
                .content(COMMENT_CONTENT)
                .build());

        participantQuestionService.addComment(QUESTION_ID, request);

        assertAll(
            () -> assertEquals(IN_REVIEW, question.getStatus()),
            () -> assertEquals(OPEN, question.getState()),
            () -> assertEquals(PRIVATE, question.getVisibility()),
            () -> assertEquals(REVIEWER_ID, question.getAssignedReviewerId()),
            () -> assertEquals(2L, question.getVersion()));

        verify(questionThreadRepository, never()).save(any(QuestionThread.class));
    }

    @Test
    void addComment_closedQuestion_shouldRejectWithoutSaving() {
        QuestionThread question = createQuestion();
        question.setState(CLOSED);

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(question));
        when(forumAccessService.requireQuestionCommentAccess(question))
            .thenReturn(COMMENTER_ID);

        assertThrows(
            InvalidQuestionStateException.class,
            () -> participantQuestionService.addComment(
                QUESTION_ID,
                new CreateCommentRequestDTO(COMMENT_CONTENT)));

        verifyNoInteractions(
            questionMessageMapper,
            questionMessageRepository,
            applicationEventPublisher);
    }

    @Test
    void addComment_inaccessibleClosedQuestion_shouldMaskBeforeLifecycleCheck() {
        QuestionThread question = createQuestion();
        question.setState(CLOSED);

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(question));
        when(forumAccessService.requireQuestionCommentAccess(question))
            .thenThrow(new QuestionNotFoundException(QUESTION_ID));

        assertThrows(
            QuestionNotFoundException.class,
            () -> participantQuestionService.addComment(
                QUESTION_ID,
                new CreateCommentRequestDTO(COMMENT_CONTENT)));

        verifyNoInteractions(
            questionMessageMapper,
            questionMessageRepository,
            applicationEventPublisher);
    }

    @Test
    void addComment_restrictedForum_shouldNotSave() {
        QuestionThread question = createQuestion();

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(question));
        when(forumAccessService.requireQuestionCommentAccess(question))
            .thenThrow(new QuestionForumAccessRestrictedException(TASK_ASSIGNMENT_ID));

        assertThrows(
            QuestionForumAccessRestrictedException.class,
            () -> participantQuestionService.addComment(
                QUESTION_ID,
                new CreateCommentRequestDTO(COMMENT_CONTENT)));

        verifyNoInteractions(
            questionMessageMapper,
            questionMessageRepository,
            applicationEventPublisher);
    }

    @Test
    void addComment_missingQuestion_shouldNotCheckAccessOrSave() {
        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.empty());

        assertThrows(
            QuestionNotFoundException.class,
            () -> participantQuestionService.addComment(
                QUESTION_ID,
                new CreateCommentRequestDTO(COMMENT_CONTENT)));

        verifyNoInteractions(
            forumAccessService,
            questionMessageMapper,
            questionMessageRepository,
            applicationEventPublisher);
    }

    @Test
    void addComment_invalidQuestionId_shouldRejectBeforeLoading() {
        CreateCommentRequestDTO request = new CreateCommentRequestDTO(COMMENT_CONTENT);

        assertAll(
            () -> assertThrows(
                ValidationException.class,
                () -> participantQuestionService.addComment(null, request)),
            () -> assertThrows(
                ValidationException.class,
                () -> participantQuestionService.addComment(0L, request)));

        verifyNoInteractions(
            questionThreadRepository,
            forumAccessService,
            questionMessageRepository,
            applicationEventPublisher);
    }

    @Test
    void addComment_repositoryFailure_shouldNotPublishEvent() {
        QuestionThread question = createQuestion();
        CreateCommentRequestDTO request = new CreateCommentRequestDTO(COMMENT_CONTENT);
        QuestionMessage mappedComment = QuestionMessage.builder()
            .content(COMMENT_CONTENT)
            .build();

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(question));
        when(forumAccessService.requireQuestionCommentAccess(question))
            .thenReturn(COMMENTER_ID);
        when(questionMessageMapper.toEntity(request))
            .thenReturn(mappedComment);
        when(questionMessageRepository.save(mappedComment))
            .thenThrow(new RuntimeException("Database failure"));

        assertThrows(
            RuntimeException.class,
            () -> participantQuestionService.addComment(QUESTION_ID, request));

        verifyNoInteractions(applicationEventPublisher);
    }

    private void stubCommentCreation(
        QuestionThread question,
        CreateCommentRequestDTO request,
        QuestionMessage mappedComment) {
        QuestionMessage savedComment = QuestionMessage.builder()
            .id(COMMENT_ID)
            .questionThreadId(QUESTION_ID)
            .authorId(COMMENTER_ID)
            .type(COMMENT)
            .content(COMMENT_CONTENT)
            .createdAt(ANSWERED_AT)
            .build();

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(question));
        when(forumAccessService.requireQuestionCommentAccess(question))
            .thenReturn(COMMENTER_ID);
        when(questionMessageMapper.toEntity(request))
            .thenReturn(mappedComment);
        when(questionMessageRepository.save(mappedComment))
            .thenReturn(savedComment);
        when(questionMessageMapper.toResponse(savedComment))
            .thenReturn(createMessageResponse(savedComment));
        when(questionThreadMapper.toResponse(question))
            .thenReturn(createQuestionResponse(question));
    }

    private QuestionThread createQuestion() {
        return QuestionThread.builder()
            .id(QUESTION_ID)
            .taskAssignmentId(TASK_ASSIGNMENT_ID)
            .authorId(AUTHOR_ID)
            .assignedReviewerId(REVIEWER_ID)
            .title("Clarification about input format")
            .content("May the input contain duplicate values?")
            .status(IN_REVIEW)
            .state(OPEN)
            .visibility(PRIVATE)
            .version(2L)
            .createdAt(CREATED_AT)
            .updatedAt(ANSWERED_AT)
            .build();
    }

    private QuestionThreadResponseDTO createQuestionResponse(QuestionThread question) {
        return new QuestionThreadResponseDTO(
            question.getId(),
            question.getTaskAssignmentId(),
            question.getAuthorId(),
            question.getAssignedReviewerId(),
            question.getTitle(),
            question.getContent(),
            question.getStatus(),
            question.getVisibility(),
            question.getState(),
            question.getVersion(),
            question.getCreatedAt(),
            question.getUpdatedAt());
    }

    private QuestionMessage createMessage(
        Long id,
        QuestionMessageType type,
        Long authorId,
        Instant createdAt) {
        return QuestionMessage.builder()
            .id(id)
            .questionThreadId(QUESTION_ID)
            .authorId(authorId)
            .type(type)
            .content(type == COMMENT
                ? "Does the limit include the output buffer?"
                : "Yes, both input and output buffers are included.")
            .createdAt(createdAt)
            .build();
    }

    private QuestionMessageResponseDTO createMessageResponse(QuestionMessage message) {
        return new QuestionMessageResponseDTO(
            message.getId(),
            message.getQuestionThreadId(),
            message.getAuthorId(),
            message.getType(),
            message.getContent(),
            message.getCreatedAt());
    }
}
