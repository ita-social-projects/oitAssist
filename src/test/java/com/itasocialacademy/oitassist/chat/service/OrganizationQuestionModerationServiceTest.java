package com.itasocialacademy.oitassist.chat.service;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.CLOSED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.ANSWERED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.IN_REVIEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PUBLIC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.chat.dao.dto.request.UpdateQuestionStateRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.request.UpdateQuestionStatusRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.request.UpdateQuestionVisibilityRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionMessageRepository;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import com.itasocialacademy.oitassist.chat.event.QuestionStateChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionStatusChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionVisibilityChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionVersionConflictException;
import com.itasocialacademy.oitassist.chat.mapper.QuestionMessageMapper;
import com.itasocialacademy.oitassist.chat.mapper.QuestionThreadMapper;
import com.itasocialacademy.oitassist.chat.service.interfaces.TaskAssignmentForumResponderService;
import com.itasocialacademy.oitassist.chat.utils.OrganizationQuestionClaimCoordinator;
import com.itasocialacademy.oitassist.chat.utils.OrganizationQuestionModerationCoordinator;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrganizationQuestionModerationServiceTest {

    private static final String ORG_ROLE =
        "ORG";

    private static final Long QUESTION_ID =
        10L;

    private static final Long TASK_ASSIGNMENT_ID =
        20L;

    private static final Long AUTHOR_ID =
        30L;

    private static final Long RESPONDER_ID =
        40L;

    private static final Long OTHER_RESPONDER_ID =
        41L;

    private static final Long VERSION =
        3L;

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
    private OrganizationQuestionModerationCoordinator organizationQuestionModerationCoordinator;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private OrganizationQuestionServiceImpl organizationQuestionService;

    @Test
    void updateVisibility_assignedResponder_shouldPublishVisibilityEvent() {

        stubOrganizationMember();

        QuestionThread currentQuestion =
            question(
                PRIVATE,
                IN_REVIEW,
                OPEN,
                VERSION);

        QuestionThread updatedQuestion =
            question(
                PUBLIC,
                IN_REVIEW,
                OPEN,
                VERSION + 1);

        QuestionThreadResponseDTO response =
            response(
                updatedQuestion);

        when(questionThreadRepository.findById(
            QUESTION_ID))
            .thenReturn(
                Optional.of(
                    currentQuestion));

        when(organizationQuestionModerationCoordinator
            .updateVisibility(
                eq(currentQuestion),
                eq(RESPONDER_ID),
                eq(PUBLIC),
                eq(VERSION),
                any(Instant.class)))
            .thenReturn(
                updatedQuestion);

        when(questionThreadMapper.toResponse(
            updatedQuestion))
            .thenReturn(
                response);

        QuestionThreadResponseDTO result =
            organizationQuestionService
                .updateVisibility(
                    QUESTION_ID,
                    new UpdateQuestionVisibilityRequestDTO(
                        PUBLIC,
                        VERSION));

        assertSame(
            response,
            result);

        ArgumentCaptor<QuestionVisibilityChangedDomainEvent> eventCaptor =
            ArgumentCaptor.forClass(
                QuestionVisibilityChangedDomainEvent.class);

        verify(applicationEventPublisher)
            .publishEvent(
                eventCaptor.capture());

        assertEquals(
            PRIVATE,
            eventCaptor.getValue()
                .previousVisibility());

        assertEquals(
            PUBLIC,
            eventCaptor.getValue()
                .currentVisibility());

        assertEquals(
            RESPONDER_ID,
            result.assignedReviewerId());

        assertEquals(
            IN_REVIEW,
            result.status());

        assertEquals(
            OPEN,
            result.state());

        assertEquals(
            VERSION + 1,
            result.version());
    }

    @Test
    void updateStatus_assignedResponder_shouldPublishStatusEvent() {

        stubOrganizationMember();

        QuestionThread currentQuestion =
            question(
                PRIVATE,
                IN_REVIEW,
                OPEN,
                VERSION);

        QuestionThread updatedQuestion =
            question(
                PRIVATE,
                ANSWERED,
                OPEN,
                VERSION + 1);

        QuestionThreadResponseDTO response =
            response(
                updatedQuestion);

        when(questionThreadRepository.findById(
            QUESTION_ID))
            .thenReturn(
                Optional.of(
                    currentQuestion));

        when(organizationQuestionModerationCoordinator
            .updateStatus(
                eq(currentQuestion),
                eq(RESPONDER_ID),
                eq(ANSWERED),
                eq(VERSION),
                any(Instant.class)))
            .thenReturn(
                updatedQuestion);

        when(questionThreadMapper.toResponse(
            updatedQuestion))
            .thenReturn(
                response);

        QuestionThreadResponseDTO result =
            organizationQuestionService
                .updateStatus(
                    QUESTION_ID,
                    new UpdateQuestionStatusRequestDTO(
                        ANSWERED,
                        VERSION));

        assertSame(
            response,
            result);

        ArgumentCaptor<QuestionStatusChangedDomainEvent> eventCaptor =
            ArgumentCaptor.forClass(
                QuestionStatusChangedDomainEvent.class);

        verify(applicationEventPublisher)
            .publishEvent(
                eventCaptor.capture());

        assertEquals(
            IN_REVIEW,
            eventCaptor.getValue()
                .previousStatus());

        assertEquals(
            ANSWERED,
            eventCaptor.getValue()
                .currentStatus());

        assertEquals(
            PRIVATE,
            result.visibility());

        assertEquals(
            OPEN,
            result.state());

        assertEquals(
            RESPONDER_ID,
            result.assignedReviewerId());
    }

    @Test
    void updateState_assignedResponder_shouldPublishStateEvent() {

        stubOrganizationMember();

        QuestionThread currentQuestion =
            question(
                PRIVATE,
                ANSWERED,
                OPEN,
                VERSION);

        QuestionThread updatedQuestion =
            question(
                PRIVATE,
                ANSWERED,
                CLOSED,
                VERSION + 1);

        QuestionThreadResponseDTO response =
            response(
                updatedQuestion);

        when(questionThreadRepository.findById(
            QUESTION_ID))
            .thenReturn(
                Optional.of(
                    currentQuestion));

        when(organizationQuestionModerationCoordinator
            .updateState(
                eq(currentQuestion),
                eq(RESPONDER_ID),
                eq(CLOSED),
                eq(VERSION),
                any(Instant.class)))
            .thenReturn(
                updatedQuestion);

        when(questionThreadMapper.toResponse(
            updatedQuestion))
            .thenReturn(
                response);

        QuestionThreadResponseDTO result =
            organizationQuestionService
                .updateState(
                    QUESTION_ID,
                    new UpdateQuestionStateRequestDTO(
                        CLOSED,
                        VERSION));

        assertSame(
            response,
            result);

        ArgumentCaptor<QuestionStateChangedDomainEvent> eventCaptor =
            ArgumentCaptor.forClass(
                QuestionStateChangedDomainEvent.class);

        verify(applicationEventPublisher)
            .publishEvent(
                eventCaptor.capture());

        assertEquals(
            OPEN,
            eventCaptor.getValue()
                .previousState());

        assertEquals(
            CLOSED,
            eventCaptor.getValue()
                .currentState());

        assertEquals(
            PRIVATE,
            result.visibility());

        assertEquals(
            ANSWERED,
            result.status());

        assertEquals(
            RESPONDER_ID,
            result.assignedReviewerId());
    }

    @Test
    void updateVisibility_otherReviewer_shouldMaskBeforeCoordinator() {

        stubOrganizationMember();

        QuestionThread question =
            question(
                PRIVATE,
                IN_REVIEW,
                OPEN,
                VERSION);

        question.setAssignedReviewerId(
            OTHER_RESPONDER_ID);

        when(questionThreadRepository.findById(
            QUESTION_ID))
            .thenReturn(
                Optional.of(
                    question));

        assertThrows(
            QuestionNotFoundException.class,
            () -> organizationQuestionService
                .updateVisibility(
                    QUESTION_ID,
                    new UpdateQuestionVisibilityRequestDTO(
                        PUBLIC,
                        VERSION)));

        verifyNoInteractions(
            organizationQuestionModerationCoordinator,
            questionThreadMapper,
            applicationEventPublisher);
    }

    @Test
    void updateStatus_missingQuestion_shouldMask() {

        stubOrganizationMember();

        when(questionThreadRepository.findById(
            QUESTION_ID))
            .thenReturn(
                Optional.empty());

        assertThrows(
            QuestionNotFoundException.class,
            () -> organizationQuestionService
                .updateStatus(
                    QUESTION_ID,
                    new UpdateQuestionStatusRequestDTO(
                        ANSWERED,
                        VERSION)));

        verifyNoInteractions(
            organizationQuestionModerationCoordinator,
            questionThreadMapper,
            applicationEventPublisher);
    }

    @Test
    void updateState_versionConflict_shouldPublishNoEvent() {

        stubOrganizationMember();

        QuestionThread question =
            question(
                PRIVATE,
                IN_REVIEW,
                OPEN,
                VERSION);

        when(questionThreadRepository.findById(
            QUESTION_ID))
            .thenReturn(
                Optional.of(
                    question));

        when(organizationQuestionModerationCoordinator
            .updateState(
                eq(question),
                eq(RESPONDER_ID),
                eq(CLOSED),
                eq(VERSION),
                any(Instant.class)))
            .thenThrow(
                new QuestionVersionConflictException(
                    QUESTION_ID));

        assertThrows(
            QuestionVersionConflictException.class,
            () -> organizationQuestionService
                .updateState(
                    QUESTION_ID,
                    new UpdateQuestionStateRequestDTO(
                        CLOSED,
                        VERSION)));

        verifyNoInteractions(
            questionThreadMapper,
            applicationEventPublisher);
    }

    @Test
    void updateVisibility_unauthenticated_shouldRejectBeforeQuestionQuery() {

        when(securityFacade.getCurrentUserId())
            .thenReturn(
                Optional.empty());

        assertThrows(
            AuthenticationException.class,
            () -> organizationQuestionService
                .updateVisibility(
                    QUESTION_ID,
                    new UpdateQuestionVisibilityRequestDTO(
                        PUBLIC,
                        VERSION)));

        verify(
            securityFacade,
            never())
            .hasRole(
                anyString());

        verifyNoInteractions(
            questionThreadRepository,
            organizationQuestionModerationCoordinator,
            applicationEventPublisher);
    }

    @Test
    void updateStatus_nonOrg_shouldRejectBeforeQuestionQuery() {

        when(securityFacade.getCurrentUserId())
            .thenReturn(
                Optional.of(
                    RESPONDER_ID));

        when(securityFacade.hasRole(
            ORG_ROLE))
            .thenReturn(false);

        assertThrows(
            AuthorizationException.class,
            () -> organizationQuestionService
                .updateStatus(
                    QUESTION_ID,
                    new UpdateQuestionStatusRequestDTO(
                        ANSWERED,
                        VERSION)));

        verifyNoInteractions(
            questionThreadRepository,
            organizationQuestionModerationCoordinator,
            applicationEventPublisher);
    }

    @Test
    void moderation_invalidInput_shouldRejectBeforeSecurity() {

        assertThrows(
            ValidationException.class,
            () -> organizationQuestionService
                .updateVisibility(
                    0L,
                    new UpdateQuestionVisibilityRequestDTO(
                        PUBLIC,
                        VERSION)));

        assertThrows(
            ValidationException.class,
            () -> organizationQuestionService
                .updateStatus(
                    QUESTION_ID,
                    null));

        assertThrows(
            ValidationException.class,
            () -> organizationQuestionService
                .updateState(
                    QUESTION_ID,
                    new UpdateQuestionStateRequestDTO(
                        CLOSED,
                        -1L)));

        verifyNoInteractions(
            securityFacade,
            questionThreadRepository,
            organizationQuestionModerationCoordinator,
            applicationEventPublisher);
    }

    private void stubOrganizationMember() {

        when(securityFacade.getCurrentUserId())
            .thenReturn(
                Optional.of(
                    RESPONDER_ID));

        when(securityFacade.hasRole(
            ORG_ROLE))
            .thenReturn(true);
    }

    private QuestionThread question(
        com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility visibility,
        com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus status,
        com.itasocialacademy.oitassist.chat.dao.enums.QuestionState state,
        Long version) {

        return QuestionThread.builder()
            .id(
                QUESTION_ID)
            .taskAssignmentId(
                TASK_ASSIGNMENT_ID)
            .authorId(
                AUTHOR_ID)
            .assignedReviewerId(
                RESPONDER_ID)
            .title(
                "Question title")
            .content(
                "Question content")
            .visibility(
                visibility)
            .status(
                status)
            .state(
                state)
            .version(
                version)
            .createdAt(
                CREATED_AT)
            .updatedAt(
                UPDATED_AT)
            .build();
    }

    private QuestionThreadResponseDTO response(
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
}