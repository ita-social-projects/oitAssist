package com.itasocialacademy.oitassist.chat.service;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType.OFFICIAL_ANSWER;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.CLOSED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.ANSWERED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.IN_REVIEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.NEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PUBLIC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.itasocialacademy.oitassist.chat.dao.dto.request.CreateOfficialAnswerRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.request.UpdateQuestionStateRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.request.UpdateQuestionStatusRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.request.UpdateQuestionVisibilityRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionMessage;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionMessageRepository;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import com.itasocialacademy.oitassist.chat.dao.repository.TaskAssignmentForumResponderRepository;
import com.itasocialacademy.oitassist.chat.event.domain.OfficialAnswerPublishedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.domain.QuestionStateChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.domain.QuestionStatusChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.domain.QuestionVisibilityChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.exceptions.InvalidQuestionStateException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionVersionConflictException;
import com.itasocialacademy.oitassist.chat.mapper.QuestionMessageMapper;
import com.itasocialacademy.oitassist.chat.mapper.QuestionThreadMapper;
import com.itasocialacademy.oitassist.chat.service.interfaces.TaskAssignmentForumResponderService;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrganizationQuestionServiceImplTest {

    private static final String ORG_ROLE = "ORG";

    private static final Long QUESTION_ID = 10L;
    private static final Long TASK_ASSIGNMENT_ID = 20L;
    private static final Long ANSWER_ID = 30L;
    private static final Long RESPONDER_ID = 100L;
    private static final Long OTHER_RESPONDER_ID = 200L;
    private static final Long AUTHOR_ID = 300L;

    private static final Long VERSION = 3L;
    private static final Long NEXT_VERSION = 4L;

    private static final Instant CREATED_AT = Instant.parse("2026-09-01T10:00:00Z");

    private static final String ANSWER_CONTENT = "The memory limit includes input and output buffers.";

    @Mock
    private QuestionThreadRepository questionThreadRepository;

    @Mock
    private QuestionMessageRepository questionMessageRepository;

    @Mock
    private TaskAssignmentForumResponderRepository responderRepository;

    @Mock
    private QuestionThreadMapper questionThreadMapper;

    @Mock
    private QuestionMessageMapper questionMessageMapper;

    @Mock
    private SecurityFacade securityFacade;

    @Mock
    private TaskAssignmentForumResponderService taskAssignmentForumResponderService;

    @Mock
    private QuestionClaimService questionClaimService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private OrganizationQuestionServiceImpl organizationQuestionService;

    @BeforeEach
    void authenticateAsOrganizationMember() {
        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.of(RESPONDER_ID));
        when(securityFacade.hasRole(ORG_ROLE))
            .thenReturn(true);

        lenient().when(questionThreadMapper.toResponse(any(QuestionThread.class)))
            .thenAnswer(invocation -> toResponse(invocation.getArgument(0)));
    }

    @Test
    void updateVisibility_assignedResponderWithGrant_shouldUpdateAndPublishEvent() {
        QuestionThread currentQuestion = questionAssignedTo(RESPONDER_ID);
        QuestionThread updatedQuestion = questionAssignedTo(RESPONDER_ID);
        updatedQuestion.setVisibility(PUBLIC);
        updatedQuestion.setVersion(NEXT_VERSION);

        givenResponderGrant(true);

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(currentQuestion), Optional.of(updatedQuestion));
        when(questionThreadRepository.updateVisibilityAsResponderIfVersionMatches(
            eq(QUESTION_ID),
            eq(TASK_ASSIGNMENT_ID),
            eq(RESPONDER_ID),
            eq(PUBLIC),
            eq(VERSION),
            any(Instant.class))).thenReturn(1);

        QuestionThreadResponseDTO result = organizationQuestionService.updateVisibility(
            QUESTION_ID,
            new UpdateQuestionVisibilityRequestDTO(PUBLIC, VERSION));

        assertEquals(PUBLIC, result.visibility());
        assertEquals(NEXT_VERSION, result.version());

        ArgumentCaptor<QuestionVisibilityChangedDomainEvent> eventCaptor =
            ArgumentCaptor.forClass(QuestionVisibilityChangedDomainEvent.class);

        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

        assertEquals(PRIVATE, eventCaptor.getValue().previousVisibility());
        assertEquals(PUBLIC, eventCaptor.getValue().currentVisibility());
    }

    @Test
    void updateVisibility_otherResponderOfSameTaskAssignment_shouldHideQuestionWithoutUpdating() {
        givenResponderGrant(true);
        givenDatabaseAcceptsResponderModeration();

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(questionAssignedTo(OTHER_RESPONDER_ID)));

        assertThrows(
            QuestionNotFoundException.class,
            () -> organizationQuestionService.updateVisibility(
                QUESTION_ID,
                new UpdateQuestionVisibilityRequestDTO(PUBLIC, VERSION)));

        verify(questionThreadRepository, never())
            .updateVisibilityAsResponderIfVersionMatches(anyLong(), anyLong(), anyLong(), any(), anyLong(), any());
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void updateVisibility_orgUserWithoutResponderGrant_shouldHideQuestionWithoutUpdating() {
        givenResponderGrant(false);
        givenDatabaseAcceptsResponderModeration();

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(questionAssignedTo(OTHER_RESPONDER_ID)));

        assertThrows(
            QuestionNotFoundException.class,
            () -> organizationQuestionService.updateVisibility(
                QUESTION_ID,
                new UpdateQuestionVisibilityRequestDTO(PUBLIC, VERSION)));

        verify(questionThreadRepository, never())
            .updateVisibilityAsResponderIfVersionMatches(anyLong(), anyLong(), anyLong(), any(), anyLong(), any());
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void updateVisibility_staleVersion_shouldThrowVersionConflictWithoutEvent() {
        givenResponderGrant(true);

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(questionAssignedTo(RESPONDER_ID)));
        when(questionThreadRepository.updateVisibilityAsResponderIfVersionMatches(
            eq(QUESTION_ID),
            eq(TASK_ASSIGNMENT_ID),
            eq(RESPONDER_ID),
            eq(PUBLIC),
            eq(VERSION),
            any(Instant.class))).thenReturn(0);

        assertThrows(
            QuestionVersionConflictException.class,
            () -> organizationQuestionService.updateVisibility(
                QUESTION_ID,
                new UpdateQuestionVisibilityRequestDTO(PUBLIC, VERSION)));

        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void updateStatus_assignedResponderWithGrant_shouldUpdateAndPublishEvent() {
        QuestionThread currentQuestion = questionAssignedTo(RESPONDER_ID);
        QuestionThread updatedQuestion = questionAssignedTo(RESPONDER_ID);
        updatedQuestion.setStatus(ANSWERED);
        updatedQuestion.setVersion(NEXT_VERSION);

        givenResponderGrant(true);

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(currentQuestion), Optional.of(updatedQuestion));
        when(questionThreadRepository.updateStatusAsResponderIfVersionMatches(
            eq(QUESTION_ID),
            eq(TASK_ASSIGNMENT_ID),
            eq(RESPONDER_ID),
            eq(ANSWERED),
            eq(VERSION),
            any(Instant.class))).thenReturn(1);

        QuestionThreadResponseDTO result = organizationQuestionService.updateStatus(
            QUESTION_ID,
            new UpdateQuestionStatusRequestDTO(ANSWERED, VERSION));

        assertEquals(ANSWERED, result.status());

        ArgumentCaptor<QuestionStatusChangedDomainEvent> eventCaptor =
            ArgumentCaptor.forClass(QuestionStatusChangedDomainEvent.class);

        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

        assertEquals(IN_REVIEW, eventCaptor.getValue().previousStatus());
        assertEquals(ANSWERED, eventCaptor.getValue().currentStatus());
    }

    @Test
    void updateStatus_otherResponderOfSameTaskAssignment_shouldHideQuestionWithoutUpdating() {
        givenResponderGrant(true);
        givenDatabaseAcceptsResponderModeration();

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(questionAssignedTo(OTHER_RESPONDER_ID)));

        assertThrows(
            QuestionNotFoundException.class,
            () -> organizationQuestionService.updateStatus(
                QUESTION_ID,
                new UpdateQuestionStatusRequestDTO(ANSWERED, VERSION)));

        verify(questionThreadRepository, never())
            .updateStatusAsResponderIfVersionMatches(anyLong(), anyLong(), anyLong(), any(), anyLong(), any());
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void updateState_assignedResponderWithGrant_shouldUpdateAndPublishEvent() {
        QuestionThread currentQuestion = questionAssignedTo(RESPONDER_ID);
        QuestionThread updatedQuestion = questionAssignedTo(RESPONDER_ID);
        updatedQuestion.setState(CLOSED);
        updatedQuestion.setVersion(NEXT_VERSION);

        givenResponderGrant(true);

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(currentQuestion), Optional.of(updatedQuestion));
        when(questionThreadRepository.updateStateAsResponderIfVersionMatches(
            eq(QUESTION_ID),
            eq(TASK_ASSIGNMENT_ID),
            eq(RESPONDER_ID),
            eq(CLOSED),
            eq(VERSION),
            any(Instant.class))).thenReturn(1);

        QuestionThreadResponseDTO result = organizationQuestionService.updateState(
            QUESTION_ID,
            new UpdateQuestionStateRequestDTO(CLOSED, VERSION));

        assertEquals(CLOSED, result.state());

        ArgumentCaptor<QuestionStateChangedDomainEvent> eventCaptor =
            ArgumentCaptor.forClass(QuestionStateChangedDomainEvent.class);

        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

        assertEquals(OPEN, eventCaptor.getValue().previousState());
        assertEquals(CLOSED, eventCaptor.getValue().currentState());
    }

    @Test
    void updateState_otherResponderOfSameTaskAssignment_shouldHideQuestionWithoutUpdating() {
        givenResponderGrant(true);
        givenDatabaseAcceptsResponderModeration();

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(questionAssignedTo(OTHER_RESPONDER_ID)));

        assertThrows(
            QuestionNotFoundException.class,
            () -> organizationQuestionService.updateState(
                QUESTION_ID,
                new UpdateQuestionStateRequestDTO(CLOSED, VERSION)));

        verify(questionThreadRepository, never())
            .updateStateAsResponderIfVersionMatches(anyLong(), anyLong(), anyLong(), any(), anyLong(), any());
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void updateState_assignedReviewerWithRevokedGrant_shouldNotReopenQuestion() {
        QuestionThread closedQuestion = questionAssignedTo(RESPONDER_ID);
        closedQuestion.setState(CLOSED);

        givenResponderGrant(false);
        givenDatabaseAcceptsResponderModeration();

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(closedQuestion));

        assertThrows(
            QuestionNotFoundException.class,
            () -> organizationQuestionService.updateState(
                QUESTION_ID,
                new UpdateQuestionStateRequestDTO(OPEN, VERSION)));

        verify(questionThreadRepository, never())
            .updateStateAsResponderIfVersionMatches(anyLong(), anyLong(), anyLong(), any(), anyLong(), any());
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void publishOfficialAnswer_assignedResponderWithGrant_shouldSaveAnswerAndMarkQuestionAnswered() {
        QuestionThread question = questionAssignedTo(RESPONDER_ID);

        givenResponderGrant(true);
        givenOfficialAnswerCanBePersisted();

        when(questionThreadRepository.findByIdForUpdate(QUESTION_ID))
            .thenReturn(Optional.of(question));

        QuestionMessageResponseDTO result = organizationQuestionService.publishOfficialAnswer(
            QUESTION_ID,
            new CreateOfficialAnswerRequestDTO(ANSWER_CONTENT));

        assertEquals(OFFICIAL_ANSWER, result.type());
        assertEquals(RESPONDER_ID, result.authorId());
        assertEquals(ANSWERED, question.getStatus());

        ArgumentCaptor<OfficialAnswerPublishedDomainEvent> eventCaptor =
            ArgumentCaptor.forClass(OfficialAnswerPublishedDomainEvent.class);

        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

        assertEquals(IN_REVIEW, eventCaptor.getValue().previousStatus());
        assertEquals(ANSWERED, eventCaptor.getValue().currentStatus());
    }

    @Test
    void publishOfficialAnswer_otherResponderOfSameTaskAssignment_shouldHideQuestionWithoutSaving() {
        givenResponderGrant(true);
        givenOfficialAnswerCanBePersisted();

        when(questionThreadRepository.findByIdForUpdate(QUESTION_ID))
            .thenReturn(Optional.of(questionAssignedTo(OTHER_RESPONDER_ID)));

        assertThrows(
            QuestionNotFoundException.class,
            () -> organizationQuestionService.publishOfficialAnswer(
                QUESTION_ID,
                new CreateOfficialAnswerRequestDTO(ANSWER_CONTENT)));

        verify(questionMessageRepository, never()).save(any(QuestionMessage.class));
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void publishOfficialAnswer_unclaimedQuestion_shouldRequireClaimBeforeAnswering() {
        QuestionThread unclaimedQuestion = questionAssignedTo(null);
        unclaimedQuestion.setStatus(NEW);

        givenResponderGrant(true);
        givenOfficialAnswerCanBePersisted();

        when(questionThreadRepository.findByIdForUpdate(QUESTION_ID))
            .thenReturn(Optional.of(unclaimedQuestion));

        assertThrows(
            QuestionNotFoundException.class,
            () -> organizationQuestionService.publishOfficialAnswer(
                QUESTION_ID,
                new CreateOfficialAnswerRequestDTO(ANSWER_CONTENT)));

        verify(questionMessageRepository, never()).save(any(QuestionMessage.class));
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void publishOfficialAnswer_orgUserWithoutResponderGrant_shouldHideQuestionWithoutSaving() {
        givenResponderGrant(false);
        givenOfficialAnswerCanBePersisted();

        when(questionThreadRepository.findByIdForUpdate(QUESTION_ID))
            .thenReturn(Optional.of(questionAssignedTo(OTHER_RESPONDER_ID)));

        assertThrows(
            QuestionNotFoundException.class,
            () -> organizationQuestionService.publishOfficialAnswer(
                QUESTION_ID,
                new CreateOfficialAnswerRequestDTO(ANSWER_CONTENT)));

        verify(questionMessageRepository, never()).save(any(QuestionMessage.class));
        verifyNoInteractions(applicationEventPublisher);
    }

    @Test
    void publishOfficialAnswer_closedQuestion_shouldRejectAnswer() {
        QuestionThread closedQuestion = questionAssignedTo(RESPONDER_ID);
        closedQuestion.setState(CLOSED);

        givenResponderGrant(true);

        when(questionThreadRepository.findByIdForUpdate(QUESTION_ID))
            .thenReturn(Optional.of(closedQuestion));

        assertThrows(
            InvalidQuestionStateException.class,
            () -> organizationQuestionService.publishOfficialAnswer(
                QUESTION_ID,
                new CreateOfficialAnswerRequestDTO(ANSWER_CONTENT)));

        verify(questionMessageRepository, never()).save(any(QuestionMessage.class));
        verifyNoInteractions(applicationEventPublisher);
    }

    private void givenResponderGrant(boolean granted) {
        lenient().when(taskAssignmentForumResponderService.isResponder(TASK_ASSIGNMENT_ID, RESPONDER_ID))
            .thenReturn(granted);
        lenient().when(responderRepository.existsByTaskAssignmentIdAndResponderUserId(TASK_ASSIGNMENT_ID, RESPONDER_ID))
            .thenReturn(granted);
    }

    private void givenDatabaseAcceptsResponderModeration() {
        lenient().when(questionThreadRepository.updateVisibilityAsResponderIfVersionMatches(
            anyLong(), anyLong(), anyLong(), any(), anyLong(), any())).thenReturn(1);
        lenient().when(questionThreadRepository.updateStatusAsResponderIfVersionMatches(
            anyLong(), anyLong(), anyLong(), any(), anyLong(), any())).thenReturn(1);
        lenient().when(questionThreadRepository.updateStateAsResponderIfVersionMatches(
            anyLong(), anyLong(), anyLong(), any(), anyLong(), any())).thenReturn(1);
    }

    private void givenOfficialAnswerCanBePersisted() {
        lenient().when(questionMessageMapper.toOfficialAnswerEntity(any(CreateOfficialAnswerRequestDTO.class)))
            .thenAnswer(invocation -> QuestionMessage.builder()
                .content(((CreateOfficialAnswerRequestDTO) invocation.getArgument(0)).content())
                .build());

        lenient().when(questionMessageRepository.save(any(QuestionMessage.class)))
            .thenAnswer(invocation -> {
                QuestionMessage message = invocation.getArgument(0);
                message.setId(ANSWER_ID);
                message.setCreatedAt(CREATED_AT);
                return message;
            });

        lenient().when(questionMessageMapper.toResponse(any(QuestionMessage.class)))
            .thenAnswer(invocation -> {
                QuestionMessage message = invocation.getArgument(0);
                return new QuestionMessageResponseDTO(
                    message.getId(),
                    message.getQuestionThreadId(),
                    message.getAuthorId(),
                    message.getType(),
                    message.getContent(),
                    message.getCreatedAt());
            });
    }

    private QuestionThread questionAssignedTo(Long reviewerId) {
        return QuestionThread.builder()
            .id(QUESTION_ID)
            .taskAssignmentId(TASK_ASSIGNMENT_ID)
            .authorId(AUTHOR_ID)
            .assignedReviewerId(reviewerId)
            .title("Clarification about memory limit")
            .content("Does the memory limit include input buffers?")
            .status(IN_REVIEW)
            .state(OPEN)
            .visibility(PRIVATE)
            .version(VERSION)
            .createdAt(CREATED_AT)
            .updatedAt(CREATED_AT)
            .build();
    }

    private QuestionThreadResponseDTO toResponse(QuestionThread question) {
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
}
