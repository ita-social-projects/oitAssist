package com.itasocialacademy.oitassist.chat.service;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.CLOSED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.IN_REVIEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import com.itasocialacademy.oitassist.chat.event.QuestionClaimedDomainEvent;
import com.itasocialacademy.oitassist.chat.exceptions.InvalidQuestionStateException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionAlreadyClaimedException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionVersionConflictException;
import com.itasocialacademy.oitassist.chat.mapper.QuestionThreadMapper;
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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrganizationQuestionClaimServiceTest {

    private static final String ORG_ROLE =
        "ORG";

    private static final Long QUESTION_ID =
        10L;

    private static final Long TASK_ASSIGNMENT_ID =
        20L;

    private static final Long RESPONDER_ID =
        30L;

    private static final Long AUTHOR_ID =
        40L;

    private static final Long EXPECTED_VERSION =
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
    private QuestionThreadMapper questionThreadMapper;

    @Mock
    private SecurityFacade securityFacade;

    @Mock
    private OrganizationQuestionClaimCoordinator organizationQuestionClaimCoordinator;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private OrganizationQuestionServiceImpl organizationQuestionService;

    @Test
    void claimQuestion_validRequest_shouldClaimPublishAndReturnQuestion() {

        stubOrganizationMember();

        QuestionThread claimedQuestion =
            claimedQuestion();

        QuestionThreadResponseDTO response =
            claimedResponse();

        when(organizationQuestionClaimCoordinator
            .claimQuestion(
                eq(QUESTION_ID),
                eq(RESPONDER_ID),
                eq(EXPECTED_VERSION),
                any(Instant.class)))
            .thenReturn(
                claimedQuestion);

        when(questionThreadMapper
            .toResponse(
                claimedQuestion))
            .thenReturn(
                response);

        QuestionThreadResponseDTO result =
            organizationQuestionService
                .claimQuestion(
                    QUESTION_ID,
                    EXPECTED_VERSION);

        assertSame(
            response,
            result);

        assertAll(
            () -> assertEquals(
                RESPONDER_ID,
                result.assignedReviewerId()),
            () -> assertEquals(
                IN_REVIEW,
                result.status()),
            () -> assertEquals(
                OPEN,
                result.state()),
            () -> assertEquals(
                EXPECTED_VERSION + 1,
                result.version()));

        ArgumentCaptor<Instant> claimTimeCaptor =
            ArgumentCaptor.forClass(
                Instant.class);

        verify(organizationQuestionClaimCoordinator)
            .claimQuestion(
                eq(QUESTION_ID),
                eq(RESPONDER_ID),
                eq(EXPECTED_VERSION),
                claimTimeCaptor.capture());

        ArgumentCaptor<QuestionClaimedDomainEvent> eventCaptor =
            ArgumentCaptor.forClass(
                QuestionClaimedDomainEvent.class);

        verify(applicationEventPublisher)
            .publishEvent(
                eventCaptor.capture());

        QuestionClaimedDomainEvent event =
            eventCaptor.getValue();

        assertAll(
            () -> assertSame(
                response,
                event.question()),
            () -> assertNull(
                event.previousReviewerId()),
            () -> assertEquals(
                RESPONDER_ID,
                event.currentReviewerId()),
            () -> assertEquals(
                claimTimeCaptor.getValue(),
                event.occurredAt()),
            () -> assertNotNull(
                event.occurredAt()));

        verifyNoInteractions(
            questionThreadRepository);
    }

    @ParameterizedTest
    @MethodSource("invalidClaimArguments")
    void claimQuestion_invalidInput_shouldRejectBeforeSecurityAndCoordinator(
        Long questionId,
        Long expectedVersion) {

        assertThrows(
            ValidationException.class,
            () -> organizationQuestionService
                .claimQuestion(
                    questionId,
                    expectedVersion));

        verifyNoInteractions(
            securityFacade,
            questionThreadRepository,
            questionThreadMapper,
            organizationQuestionClaimCoordinator,
            applicationEventPublisher);
    }

    @Test
    void claimQuestion_unauthenticated_shouldRejectBeforeCoordinator() {

        when(securityFacade
            .getCurrentUserId())
            .thenReturn(
                Optional.empty());

        assertThrows(
            AuthenticationException.class,
            () -> organizationQuestionService
                .claimQuestion(
                    QUESTION_ID,
                    EXPECTED_VERSION));

        verify(
            securityFacade,
            never())
            .hasRole(
                anyString());

        verifyNoInteractions(
            questionThreadRepository,
            questionThreadMapper,
            organizationQuestionClaimCoordinator,
            applicationEventPublisher);
    }

    @Test
    void claimQuestion_nonOrg_shouldRejectBeforeCoordinator() {

        when(securityFacade
            .getCurrentUserId())
            .thenReturn(
                Optional.of(
                    RESPONDER_ID));

        when(securityFacade
            .hasRole(ORG_ROLE))
            .thenReturn(false);

        assertThrows(
            AuthorizationException.class,
            () -> organizationQuestionService
                .claimQuestion(
                    QUESTION_ID,
                    EXPECTED_VERSION));

        verifyNoInteractions(
            questionThreadRepository,
            questionThreadMapper,
            organizationQuestionClaimCoordinator,
            applicationEventPublisher);
    }

    @ParameterizedTest
    @MethodSource("claimFailures")
    void claimQuestion_failedCoordinator_shouldPropagateWithoutEvent(
        RuntimeException failure) {

        stubOrganizationMember();

        when(organizationQuestionClaimCoordinator
            .claimQuestion(
                eq(QUESTION_ID),
                eq(RESPONDER_ID),
                eq(EXPECTED_VERSION),
                any(Instant.class)))
            .thenThrow(
                failure);

        RuntimeException result =
            assertThrows(
                failure.getClass(),
                () -> organizationQuestionService
                    .claimQuestion(
                        QUESTION_ID,
                        EXPECTED_VERSION));

        assertSame(
            failure,
            result);

        verifyNoInteractions(
            questionThreadRepository,
            questionThreadMapper,
            applicationEventPublisher);
    }

    @Test
    void claimQuestion_mapperFailure_shouldNotPublishEvent() {

        stubOrganizationMember();

        QuestionThread claimedQuestion =
            claimedQuestion();

        RuntimeException mapperFailure =
            new RuntimeException(
                "Mapper failure");

        when(organizationQuestionClaimCoordinator
            .claimQuestion(
                eq(QUESTION_ID),
                eq(RESPONDER_ID),
                eq(EXPECTED_VERSION),
                any(Instant.class)))
            .thenReturn(
                claimedQuestion);

        when(questionThreadMapper
            .toResponse(
                claimedQuestion))
            .thenThrow(
                mapperFailure);

        RuntimeException result =
            assertThrows(
                RuntimeException.class,
                () -> organizationQuestionService
                    .claimQuestion(
                        QUESTION_ID,
                        EXPECTED_VERSION));

        assertSame(
            mapperFailure,
            result);

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
            .hasRole(ORG_ROLE))
            .thenReturn(true);
    }

    private QuestionThread claimedQuestion() {

        return QuestionThread.builder()
            .id(QUESTION_ID)
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
            .status(
                IN_REVIEW)
            .state(
                OPEN)
            .visibility(
                PRIVATE)
            .version(
                EXPECTED_VERSION + 1)
            .createdAt(
                CREATED_AT)
            .updatedAt(
                UPDATED_AT)
            .build();
    }

    private QuestionThreadResponseDTO claimedResponse() {

        QuestionThread question =
            claimedQuestion();

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

    private static Stream<Arguments> invalidClaimArguments() {

        return Stream.of(
            Arguments.of(
                null,
                EXPECTED_VERSION),
            Arguments.of(
                0L,
                EXPECTED_VERSION),
            Arguments.of(
                -1L,
                EXPECTED_VERSION),
            Arguments.of(
                QUESTION_ID,
                null),
            Arguments.of(
                QUESTION_ID,
                -1L));
    }

    private static Stream<RuntimeException> claimFailures() {

        return Stream.of(
            new QuestionNotFoundException(
                QUESTION_ID),
            new QuestionAlreadyClaimedException(
                QUESTION_ID),
            new InvalidQuestionStateException(
                QUESTION_ID,
                CLOSED,
                "claim for review"),
            new QuestionVersionConflictException(
                QUESTION_ID),
            new RuntimeException(
                "Database failure"));
    }
}