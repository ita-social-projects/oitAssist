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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionMessage;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionMessageRepository;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
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
