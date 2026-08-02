package com.itasocialacademy.oitassist.chat.service;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.CLOSED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.IN_REVIEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import com.itasocialacademy.oitassist.chat.utils.QuestionClaimFailureClassifier;
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
class AdministratorQuestionClaimServiceTest {

    private static final String ADMIN_ROLE = "ADMIN";

    private static final Long QUESTION_ID = 10L;
    private static final Long ADMINISTRATOR_ID = 20L;
    private static final Long EXPECTED_VERSION = 3L;

    @Mock
    private QuestionThreadRepository questionThreadRepository;

    @Mock
    private QuestionThreadMapper questionThreadMapper;

    @Mock
    private SecurityFacade securityFacade;

    @Mock
    private QuestionClaimFailureClassifier questionClaimFailureClassifier;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private AdministratorQuestionServiceImpl administratorQuestionService;

    @Test
    void claimQuestion_validRequest_shouldClaimPublishAndReturnUpdatedQuestion() {
        stubAdministrator();

        QuestionThread claimedQuestion =
            claimedQuestion();

        QuestionThreadResponseDTO response =
            claimedResponse();

        when(questionThreadRepository.claimForReview(
            eq(QUESTION_ID),
            eq(ADMINISTRATOR_ID),
            eq(EXPECTED_VERSION),
            any(Instant.class)))
            .thenReturn(1);

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(
                Optional.of(claimedQuestion));

        when(questionThreadMapper.toResponse(claimedQuestion))
            .thenReturn(response);

        QuestionThreadResponseDTO result =
            administratorQuestionService.claimQuestion(
                QUESTION_ID,
                EXPECTED_VERSION);

        assertAll(
            () -> assertSame(
                response,
                result),
            () -> assertEquals(
                ADMINISTRATOR_ID,
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

        verify(questionThreadRepository)
            .claimForReview(
                eq(QUESTION_ID),
                eq(ADMINISTRATOR_ID),
                eq(EXPECTED_VERSION),
                any(Instant.class));

        verify(questionThreadRepository)
            .findById(QUESTION_ID);

        verify(questionThreadRepository, never())
            .save(any(QuestionThread.class));

        verifyNoInteractions(
            questionClaimFailureClassifier);

        ArgumentCaptor<QuestionClaimedDomainEvent> eventCaptor =
            ArgumentCaptor.forClass(
                QuestionClaimedDomainEvent.class);

        verify(applicationEventPublisher)
            .publishEvent(eventCaptor.capture());

        QuestionClaimedDomainEvent event =
            eventCaptor.getValue();

        assertAll(
            () -> assertSame(
                response,
                event.question()),
            () -> assertNull(
                event.previousReviewerId()),
            () -> assertEquals(
                ADMINISTRATOR_ID,
                event.currentReviewerId()),
            () -> assertEquals(
                QUESTION_ID,
                event.questionId()),
            () -> assertNotNull(
                event.occurredAt()));
    }

    @ParameterizedTest
    @MethodSource("invalidClaimArguments")
    void claimQuestion_invalidInput_shouldRejectBeforeSecurityAndPersistence(
        Long questionId,
        Long expectedVersion) {

        assertThrows(
            ValidationException.class,
            () -> administratorQuestionService
                .claimQuestion(
                    questionId,
                    expectedVersion));

        verifyNoInteractions(
            securityFacade,
            questionThreadRepository,
            questionThreadMapper,
            questionClaimFailureClassifier,
            applicationEventPublisher);
    }

    @Test
    void claimQuestion_unauthenticated_shouldRejectBeforeUpdate() {
        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.empty());

        assertThrows(
            AuthenticationException.class,
            () -> administratorQuestionService
                .claimQuestion(
                    QUESTION_ID,
                    EXPECTED_VERSION));

        verify(
            securityFacade,
            never())
            .hasRole(ADMIN_ROLE);

        verifyNoInteractions(
            questionThreadRepository,
            questionThreadMapper,
            questionClaimFailureClassifier,
            applicationEventPublisher);
    }

    @Test
    void claimQuestion_nonAdministrator_shouldRejectBeforeUpdate() {
        when(securityFacade.getCurrentUserId())
            .thenReturn(
                Optional.of(ADMINISTRATOR_ID));

        when(securityFacade.hasRole(ADMIN_ROLE))
            .thenReturn(false);

        assertThrows(
            AuthorizationException.class,
            () -> administratorQuestionService
                .claimQuestion(
                    QUESTION_ID,
                    EXPECTED_VERSION));

        verifyNoInteractions(
            questionThreadRepository,
            questionThreadMapper,
            questionClaimFailureClassifier,
            applicationEventPublisher);
    }

    @ParameterizedTest
    @MethodSource("classifiedFailures")
    void claimQuestion_zeroUpdatedRows_shouldPropagateClassification(
        RuntimeException failure) {

        stubAdministrator();

        when(questionThreadRepository.claimForReview(
            eq(QUESTION_ID),
            eq(ADMINISTRATOR_ID),
            eq(EXPECTED_VERSION),
            any(Instant.class)))
            .thenReturn(0);

        doThrow(failure)
            .when(questionClaimFailureClassifier)
            .classifyAndThrow(
                QUESTION_ID,
                EXPECTED_VERSION);

        RuntimeException result =
            assertThrows(
                failure.getClass(),
                () -> administratorQuestionService
                    .claimQuestion(
                        QUESTION_ID,
                        EXPECTED_VERSION));

        assertSame(failure, result);

        verify(questionThreadRepository, times(1))
            .claimForReview(
                eq(QUESTION_ID),
                eq(ADMINISTRATOR_ID),
                eq(EXPECTED_VERSION),
                any(Instant.class));

        verify(questionClaimFailureClassifier)
            .classifyAndThrow(
                QUESTION_ID,
                EXPECTED_VERSION);

        verify(questionThreadRepository, never())
            .findById(QUESTION_ID);

        verifyNoInteractions(
            questionThreadMapper,
            applicationEventPublisher);
    }

    @Test
    void claimQuestion_repositoryFailure_shouldPropagateWithoutClassification() {
        stubAdministrator();

        RuntimeException repositoryFailure =
            new RuntimeException("Database failure");

        when(questionThreadRepository.claimForReview(
            eq(QUESTION_ID),
            eq(ADMINISTRATOR_ID),
            eq(EXPECTED_VERSION),
            any(Instant.class)))
            .thenThrow(repositoryFailure);

        RuntimeException result =
            assertThrows(
                RuntimeException.class,
                () -> administratorQuestionService
                    .claimQuestion(
                        QUESTION_ID,
                        EXPECTED_VERSION));

        assertSame(repositoryFailure, result);

        verifyNoInteractions(
            questionClaimFailureClassifier,
            questionThreadMapper,
            applicationEventPublisher);
    }

    private void stubAdministrator() {
        when(securityFacade.getCurrentUserId())
            .thenReturn(
                Optional.of(ADMINISTRATOR_ID));

        when(securityFacade.hasRole(ADMIN_ROLE))
            .thenReturn(true);
    }

    private QuestionThread claimedQuestion() {
        return QuestionThread.builder()
            .id(QUESTION_ID)
            .taskAssignmentId(100L)
            .authorId(200L)
            .assignedReviewerId(ADMINISTRATOR_ID)
            .title("Question title")
            .content("Question content")
            .status(IN_REVIEW)
            .state(OPEN)
            .visibility(PRIVATE)
            .version(EXPECTED_VERSION + 1)
            .createdAt(
                Instant.parse(
                    "2026-08-01T10:00:00Z"))
            .updatedAt(
                Instant.parse(
                    "2026-08-01T10:15:00Z"))
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
            Arguments.of(null, EXPECTED_VERSION),
            Arguments.of(0L, EXPECTED_VERSION),
            Arguments.of(-1L, EXPECTED_VERSION),
            Arguments.of(QUESTION_ID, null),
            Arguments.of(QUESTION_ID, -1L));
    }

    private static Stream<RuntimeException> classifiedFailures() {

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
                QUESTION_ID));
    }
}