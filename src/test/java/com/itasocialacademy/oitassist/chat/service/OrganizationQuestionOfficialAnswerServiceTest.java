package com.itasocialacademy.oitassist.chat.service;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType.COMMENT;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType.OFFICIAL_ANSWER;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.CLOSED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.ANSWERED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.IN_REVIEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.NEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.chat.dao.dto.request.CreateOfficialAnswerRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionMessage;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionMessageRepository;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import com.itasocialacademy.oitassist.chat.event.OfficialAnswerPublishedDomainEvent;
import com.itasocialacademy.oitassist.chat.exceptions.InvalidQuestionStateException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import com.itasocialacademy.oitassist.chat.mapper.QuestionMessageMapper;
import com.itasocialacademy.oitassist.chat.mapper.QuestionThreadMapper;
import com.itasocialacademy.oitassist.chat.service.interfaces.TaskAssignmentForumResponderService;
import com.itasocialacademy.oitassist.chat.utils.OrganizationQuestionClaimCoordinator;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrganizationQuestionOfficialAnswerServiceTest {

    private static final String ORG_ROLE =
        "ORG";

    private static final Long QUESTION_ID =
        10L;

    private static final Long TASK_ASSIGNMENT_ID =
        20L;

    private static final Long QUESTION_AUTHOR_ID =
        30L;

    private static final Long RESPONDER_ID =
        40L;

    private static final Long OTHER_REVIEWER_ID =
        41L;

    private static final Long MESSAGE_ID =
        50L;

    private static final String ANSWER_CONTENT =
        "The limit includes all process memory.";

    private static final Instant CREATED_AT =
        Instant.parse(
            "2026-08-05T10:00:00Z");

    private static final Instant UPDATED_AT =
        Instant.parse(
            "2026-08-05T10:15:00Z");

    @Mock
    private QuestionThreadRepository questionThreadRepository;

    @Mock
    private QuestionMessageRepository questionMessageRepository;

    @Mock
    private QuestionThreadMapper questionThreadMapper;

    @Mock
    private QuestionMessageMapper questionMessageMapper;

    @Mock
    private SecurityFacade securityFacade;

    @Mock
    private TaskAssignmentForumResponderService taskAssignmentForumResponderService;

    @Mock
    private OrganizationQuestionClaimCoordinator organizationQuestionClaimCoordinator;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private OrganizationQuestionServiceImpl organizationQuestionService;

    @ParameterizedTest
    @MethodSource("answerableStatuses")
    void publishOfficialAnswer_answerableStatus_shouldPersistAndPublish(
        QuestionStatus initialStatus) {

        stubOrganizationMember();

        QuestionThread question =
            createQuestion(
                initialStatus,
                OPEN,
                RESPONDER_ID);

        CreateOfficialAnswerRequestDTO request =
            new CreateOfficialAnswerRequestDTO(
                ANSWER_CONTENT);

        /*
         * Polluted mapper result verifies that every protected field is overwritten by
         * the service.
         */
        QuestionMessage mappedAnswer =
            QuestionMessage.builder()
                .id(999L)
                .questionThreadId(999L)
                .authorId(999L)
                .type(COMMENT)
                .content(
                    ANSWER_CONTENT)
                .createdAt(
                    CREATED_AT)
                .build();

        QuestionMessage savedAnswer =
            QuestionMessage.builder()
                .id(
                    MESSAGE_ID)
                .questionThreadId(
                    QUESTION_ID)
                .authorId(
                    RESPONDER_ID)
                .type(
                    OFFICIAL_ANSWER)
                .content(
                    ANSWER_CONTENT)
                .createdAt(
                    CREATED_AT)
                .build();

        QuestionMessageResponseDTO messageResponse =
            new QuestionMessageResponseDTO(
                MESSAGE_ID,
                QUESTION_ID,
                RESPONDER_ID,
                OFFICIAL_ANSWER,
                ANSWER_CONTENT,
                CREATED_AT);

        var originalTaskAssignmentId =
            question.getTaskAssignmentId();

        var originalAuthorId =
            question.getAuthorId();

        var originalReviewerId =
            question.getAssignedReviewerId();

        var originalTitle =
            question.getTitle();

        var originalContent =
            question.getContent();

        var originalVisibility =
            question.getVisibility();

        var originalState =
            question.getState();

        when(questionThreadRepository
            .findByIdForUpdate(
                QUESTION_ID))
            .thenReturn(
                Optional.of(
                    question));

        when(taskAssignmentForumResponderService
            .isResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(true);

        when(questionMessageMapper
            .toOfficialAnswerEntity(
                request))
            .thenReturn(
                mappedAnswer);

        when(questionMessageRepository.save(
            mappedAnswer))
            .thenReturn(
                savedAnswer);

        when(questionMessageMapper.toResponse(
            savedAnswer))
            .thenReturn(
                messageResponse);

        when(questionThreadMapper.toResponse(
            question))
            .thenAnswer(invocation -> createQuestionResponse(
                invocation.getArgument(0)));

        QuestionMessageResponseDTO result =
            organizationQuestionService
                .publishOfficialAnswer(
                    QUESTION_ID,
                    request);

        assertSame(
            messageResponse,
            result);

        assertAll(
            () -> assertNull(
                mappedAnswer.getId()),
            () -> assertEquals(
                QUESTION_ID,
                mappedAnswer.getQuestionThreadId()),
            () -> assertEquals(
                RESPONDER_ID,
                mappedAnswer.getAuthorId()),
            () -> assertEquals(
                OFFICIAL_ANSWER,
                mappedAnswer.getType()),
            () -> assertEquals(
                ANSWER_CONTENT,
                mappedAnswer.getContent()),
            () -> assertNull(
                mappedAnswer.getCreatedAt()),
            () -> assertEquals(
                ANSWERED,
                question.getStatus()),
            () -> assertEquals(
                originalTaskAssignmentId,
                question.getTaskAssignmentId()),
            () -> assertEquals(
                originalAuthorId,
                question.getAuthorId()),
            () -> assertEquals(
                originalReviewerId,
                question.getAssignedReviewerId()),
            () -> assertEquals(
                originalTitle,
                question.getTitle()),
            () -> assertEquals(
                originalContent,
                question.getContent()),
            () -> assertEquals(
                originalVisibility,
                question.getVisibility()),
            () -> assertEquals(
                originalState,
                question.getState()));

        ArgumentCaptor<OfficialAnswerPublishedDomainEvent> eventCaptor =
            ArgumentCaptor.forClass(
                OfficialAnswerPublishedDomainEvent.class);

        InOrder order =
            inOrder(
                securityFacade,
                questionThreadRepository,
                taskAssignmentForumResponderService,
                questionMessageMapper,
                questionMessageRepository,
                questionThreadMapper,
                applicationEventPublisher);

        order.verify(securityFacade)
            .getCurrentUserId();

        order.verify(securityFacade)
            .hasRole(
                ORG_ROLE);

        order.verify(questionThreadRepository)
            .findByIdForUpdate(
                QUESTION_ID);

        order.verify(taskAssignmentForumResponderService)
            .isResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID);

        order.verify(questionMessageMapper)
            .toOfficialAnswerEntity(
                request);

        order.verify(questionMessageRepository)
            .save(
                mappedAnswer);

        order.verify(questionThreadRepository)
            .flush();

        order.verify(questionMessageMapper)
            .toResponse(
                savedAnswer);

        order.verify(questionThreadMapper)
            .toResponse(
                question);

        order.verify(applicationEventPublisher)
            .publishEvent(
                eventCaptor.capture());

        OfficialAnswerPublishedDomainEvent event =
            eventCaptor.getValue();

        assertAll(
            () -> assertEquals(
                initialStatus,
                event.previousStatus()),
            () -> assertEquals(
                ANSWERED,
                event.currentStatus()),
            () -> assertEquals(
                ANSWERED,
                event.question().status()),
            () -> assertSame(
                messageResponse,
                event.message()),
            () -> assertNotNull(
                event.occurredAt()));

        verify(
            questionThreadRepository,
            never())
            .save(
                any(QuestionThread.class));
    }

    @Test
    void publishOfficialAnswer_questionAssignedToAnotherReviewer_shouldMask() {

        stubOrganizationMember();

        QuestionThread question =
            createQuestion(
                IN_REVIEW,
                OPEN,
                OTHER_REVIEWER_ID);

        when(questionThreadRepository
            .findByIdForUpdate(
                QUESTION_ID))
            .thenReturn(
                Optional.of(
                    question));

        assertThrows(
            QuestionNotFoundException.class,
            () -> organizationQuestionService
                .publishOfficialAnswer(
                    QUESTION_ID,
                    validRequest()));

        verify(
            taskAssignmentForumResponderService,
            never())
            .isResponder(
                any(),
                any());

        verifyNoPersistenceOrEvent();
    }

    @Test
    void publishOfficialAnswer_responderGrantRevoked_shouldMask() {

        stubOrganizationMember();

        QuestionThread question =
            createQuestion(
                IN_REVIEW,
                OPEN,
                RESPONDER_ID);

        when(questionThreadRepository
            .findByIdForUpdate(
                QUESTION_ID))
            .thenReturn(
                Optional.of(
                    question));

        when(taskAssignmentForumResponderService
            .isResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(false);

        assertThrows(
            QuestionNotFoundException.class,
            () -> organizationQuestionService
                .publishOfficialAnswer(
                    QUESTION_ID,
                    validRequest()));

        verifyNoPersistenceOrEvent();
    }

    @Test
    void publishOfficialAnswer_closedQuestion_shouldReject() {

        stubOrganizationMember();

        QuestionThread question =
            createQuestion(
                IN_REVIEW,
                CLOSED,
                RESPONDER_ID);

        when(questionThreadRepository
            .findByIdForUpdate(
                QUESTION_ID))
            .thenReturn(
                Optional.of(
                    question));

        when(taskAssignmentForumResponderService
            .isResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(true);

        InvalidQuestionStateException exception =
            assertThrows(
                InvalidQuestionStateException.class,
                () -> organizationQuestionService
                    .publishOfficialAnswer(
                        QUESTION_ID,
                        validRequest()));

        assertNotNull(exception);

        verifyNoPersistenceOrEvent();
    }

    @Test
    void publishOfficialAnswer_missingQuestion_shouldReturnNotFound() {

        stubOrganizationMember();

        when(questionThreadRepository
            .findByIdForUpdate(
                QUESTION_ID))
            .thenReturn(
                Optional.empty());

        assertThrows(
            QuestionNotFoundException.class,
            () -> organizationQuestionService
                .publishOfficialAnswer(
                    QUESTION_ID,
                    validRequest()));

        verifyNoInteractions(
            taskAssignmentForumResponderService);

        verifyNoPersistenceOrEvent();
    }

    @Test
    void publishOfficialAnswer_unauthenticated_shouldRejectBeforeLock() {

        when(securityFacade
            .getCurrentUserId())
            .thenReturn(
                Optional.empty());

        assertThrows(
            AuthenticationException.class,
            () -> organizationQuestionService
                .publishOfficialAnswer(
                    QUESTION_ID,
                    validRequest()));

        verify(
            securityFacade,
            never())
            .hasRole(
                anyString());

        verifyNoInteractions(
            questionThreadRepository,
            taskAssignmentForumResponderService,
            questionMessageRepository,
            questionMessageMapper,
            questionThreadMapper,
            applicationEventPublisher);
    }

    @Test
    void publishOfficialAnswer_nonOrg_shouldRejectBeforeLock() {

        when(securityFacade
            .getCurrentUserId())
            .thenReturn(
                Optional.of(
                    RESPONDER_ID));

        when(securityFacade
            .hasRole(
                ORG_ROLE))
            .thenReturn(false);

        assertThrows(
            AuthorizationException.class,
            () -> organizationQuestionService
                .publishOfficialAnswer(
                    QUESTION_ID,
                    validRequest()));

        verifyNoInteractions(
            questionThreadRepository,
            taskAssignmentForumResponderService,
            questionMessageRepository,
            questionMessageMapper,
            questionThreadMapper,
            applicationEventPublisher);
    }

    @Test
    void publishOfficialAnswer_invalidQuestionId_shouldRejectBeforeSecurity() {

        assertAll(
            () -> assertThrows(
                ValidationException.class,
                () -> organizationQuestionService
                    .publishOfficialAnswer(
                        null,
                        validRequest())),
            () -> assertThrows(
                ValidationException.class,
                () -> organizationQuestionService
                    .publishOfficialAnswer(
                        0L,
                        validRequest())),
            () -> assertThrows(
                ValidationException.class,
                () -> organizationQuestionService
                    .publishOfficialAnswer(
                        -1L,
                        validRequest())));

        verifyNoInteractions(
            securityFacade,
            questionThreadRepository,
            taskAssignmentForumResponderService,
            questionMessageRepository,
            questionMessageMapper,
            questionThreadMapper,
            applicationEventPublisher);
    }

    @Test
    void publishOfficialAnswer_repositoryFailure_shouldPublishNoEvent() {

        stubOrganizationMember();

        QuestionThread question =
            createQuestion(
                IN_REVIEW,
                OPEN,
                RESPONDER_ID);

        QuestionMessage mappedAnswer =
            QuestionMessage.builder()
                .content(
                    ANSWER_CONTENT)
                .build();

        RuntimeException repositoryFailure =
            new RuntimeException(
                "Database failure");

        when(questionThreadRepository
            .findByIdForUpdate(
                QUESTION_ID))
            .thenReturn(
                Optional.of(
                    question));

        when(taskAssignmentForumResponderService
            .isResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(true);

        when(questionMessageMapper
            .toOfficialAnswerEntity(
                any(CreateOfficialAnswerRequestDTO.class)))
            .thenReturn(
                mappedAnswer);

        when(questionMessageRepository.save(
            mappedAnswer))
            .thenThrow(
                repositoryFailure);

        RuntimeException result =
            assertThrows(
                RuntimeException.class,
                () -> organizationQuestionService
                    .publishOfficialAnswer(
                        QUESTION_ID,
                        validRequest()));

        assertSame(
            repositoryFailure,
            result);

        verify(
            questionThreadRepository,
            never())
            .flush();

        verifyNoInteractions(
            applicationEventPublisher);
    }

    private void stubOrganizationMember() {

        when(securityFacade
            .getCurrentUserId())
            .thenReturn(
                Optional.of(
                    RESPONDER_ID));

        when(securityFacade
            .hasRole(
                ORG_ROLE))
            .thenReturn(true);
    }

    private void verifyNoPersistenceOrEvent() {

        verifyNoInteractions(
            questionMessageRepository,
            questionMessageMapper,
            questionThreadMapper,
            applicationEventPublisher);

        verify(
            questionThreadRepository,
            never())
            .flush();
    }

    private CreateOfficialAnswerRequestDTO validRequest() {

        return new CreateOfficialAnswerRequestDTO(
            ANSWER_CONTENT);
    }

    private QuestionThread createQuestion(
        QuestionStatus status,
        com.itasocialacademy.oitassist.chat.dao.enums.QuestionState state,
        Long reviewerId) {

        return QuestionThread.builder()
            .id(
                QUESTION_ID)
            .taskAssignmentId(
                TASK_ASSIGNMENT_ID)
            .authorId(
                QUESTION_AUTHOR_ID)
            .assignedReviewerId(
                reviewerId)
            .title(
                "Question title")
            .content(
                "Question content")
            .status(
                status)
            .state(
                state)
            .visibility(
                PRIVATE)
            .version(
                3L)
            .createdAt(
                CREATED_AT)
            .updatedAt(
                UPDATED_AT)
            .build();
    }

    private QuestionThreadResponseDTO createQuestionResponse(
        QuestionThread question) {

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

    private static Stream<QuestionStatus> answerableStatuses() {

        return Stream.of(
            NEW,
            IN_REVIEW,
            ANSWERED);
    }
}