package com.itasocialacademy.oitassist.chat.service;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.CLOSED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.ANSWERED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.IN_REVIEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.NEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PUBLIC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionVersionConflictException;
import com.itasocialacademy.oitassist.chat.mapper.QuestionMessageMapper;
import com.itasocialacademy.oitassist.chat.mapper.QuestionThreadMapper;
import com.itasocialacademy.oitassist.chat.utils.QuestionClaimFailureClassifier;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class AdministratorQuestionModerationServiceTest {

    private static final String ADMIN_ROLE = "ADMIN";

    private static final Long QUESTION_ID = 10L;
    private static final Long ADMINISTRATOR_ID = 20L;
    private static final Long AUTHOR_ID = 30L;
    private static final Long REVIEWER_ID = 40L;
    private static final Long TASK_ASSIGNMENT_ID = 50L;

    private static final Long EXPECTED_VERSION = 3L;
    private static final Long UPDATED_VERSION = 4L;

    private static final Instant CREATED_AT =
        Instant.parse("2026-08-01T10:00:00Z");

    private static final Instant UPDATED_AT =
        Instant.parse("2026-08-01T11:00:00Z");

    @Mock
    private QuestionMessageRepository questionMessageRepository;

    @Mock
    private QuestionMessageMapper questionMessageMapper;

    @Mock
    private QuestionThreadRepository questionThreadRepository;

    @Mock
    private QuestionThreadMapper questionThreadMapper;

    @Mock
    private SecurityFacade securityFacade;

    @Mock
    private QuestionClaimFailureClassifier questionClaimFailureClassifier;

    @InjectMocks
    private AdministratorQuestionServiceImpl administratorQuestionService;

    @ParameterizedTest
    @EnumSource(ModerationOperation.class)
    void moderation_success_shouldUpdateExactlyOnceAndReturnFreshResponse(
        ModerationOperation operation) {

        stubAdministrator();
        stubUpdate(operation, 1);

        QuestionThread updatedQuestion =
            createUpdatedQuestion(operation);

        QuestionThreadResponseDTO response =
            createResponse(updatedQuestion);

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(updatedQuestion));

        when(questionThreadMapper.toResponse(updatedQuestion))
            .thenReturn(response);

        QuestionThreadResponseDTO result =
            invoke(operation);

        assertSame(response, result);
        assertEquals(UPDATED_VERSION, result.version());

        verifyUpdate(operation, 1);

        verify(questionThreadRepository)
            .findById(QUESTION_ID);

        verify(questionThreadMapper)
            .toResponse(updatedQuestion);

        verifyNoInteractions(
            questionMessageRepository,
            questionMessageMapper,
            questionClaimFailureClassifier);
    }

    @ParameterizedTest
    @MethodSource("statusTransitions")
    void updateStatus_allSupportedTransitions_shouldSucceed(
        com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus initialStatus,
        com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus targetStatus) {

        stubAdministrator();

        UpdateQuestionStatusRequestDTO request =
            new UpdateQuestionStatusRequestDTO(
                targetStatus,
                EXPECTED_VERSION);

        QuestionThread updatedQuestion =
            createBaseQuestion();

        updatedQuestion.setStatus(targetStatus);
        updatedQuestion.setVersion(UPDATED_VERSION);

        when(questionThreadRepository
            .updateStatusIfVersionMatches(
                org.mockito.ArgumentMatchers.eq(QUESTION_ID),
                org.mockito.ArgumentMatchers.eq(targetStatus),
                org.mockito.ArgumentMatchers.eq(EXPECTED_VERSION),
                any(Instant.class)))
            .thenReturn(1);

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(updatedQuestion));

        when(questionThreadMapper.toResponse(updatedQuestion))
            .thenReturn(createResponse(updatedQuestion));

        QuestionThreadResponseDTO result =
            administratorQuestionService.updateStatus(
                QUESTION_ID,
                request);

        assertEquals(targetStatus, result.status());
        assertEquals(PRIVATE, result.visibility());
        assertEquals(OPEN, result.state());
        assertEquals(REVIEWER_ID, result.assignedReviewerId());

        verify(questionThreadRepository)
            .updateStatusIfVersionMatches(
                org.mockito.ArgumentMatchers.eq(QUESTION_ID),
                org.mockito.ArgumentMatchers.eq(targetStatus),
                org.mockito.ArgumentMatchers.eq(EXPECTED_VERSION),
                any(Instant.class));

        verifyNoInteractions(
            questionMessageRepository,
            questionMessageMapper);

        assertNotNull(initialStatus);
    }

    @ParameterizedTest
    @EnumSource(ModerationOperation.class)
    void moderation_staleVersion_shouldThrowConflictWithoutRetry(
        ModerationOperation operation) {

        stubAdministrator();
        stubUpdate(operation, 0);

        QuestionThread currentQuestion =
            createBaseQuestion();

        currentQuestion.setVersion(UPDATED_VERSION);

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.of(currentQuestion));

        assertThrows(
            QuestionVersionConflictException.class,
            () -> invoke(operation));

        verifyUpdate(operation, 1);

        verify(questionThreadMapper, never())
            .toResponse(any(QuestionThread.class));

        verifyNoInteractions(
            questionMessageRepository,
            questionMessageMapper,
            questionClaimFailureClassifier);
    }

    @ParameterizedTest
    @EnumSource(ModerationOperation.class)
    void moderation_missingQuestion_shouldThrowNotFoundWithoutRetry(
        ModerationOperation operation) {

        stubAdministrator();
        stubUpdate(operation, 0);

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(Optional.empty());

        assertThrows(
            QuestionNotFoundException.class,
            () -> invoke(operation));

        verifyUpdate(operation, 1);

        verify(questionThreadMapper, never())
            .toResponse(any(QuestionThread.class));
    }

    @ParameterizedTest
    @EnumSource(ModerationOperation.class)
    void moderation_sameVersionUsedTwice_secondRequestShouldConflict(
        ModerationOperation operation) {

        stubAdministrator();

        stubSequentialUpdate(
            operation,
            1,
            0);

        QuestionThread updatedQuestion =
            createUpdatedQuestion(operation);

        QuestionThreadResponseDTO response =
            createResponse(updatedQuestion);

        when(questionThreadRepository.findById(QUESTION_ID))
            .thenReturn(
                Optional.of(updatedQuestion),
                Optional.of(updatedQuestion));

        when(questionThreadMapper.toResponse(updatedQuestion))
            .thenReturn(response);

        QuestionThreadResponseDTO firstResult =
            invoke(operation);

        assertSame(response, firstResult);

        assertThrows(
            QuestionVersionConflictException.class,
            () -> invoke(operation));

        verifyUpdate(operation, 2);

        verify(questionThreadMapper, times(1))
            .toResponse(updatedQuestion);
    }

    @ParameterizedTest
    @EnumSource(ModerationOperation.class)
    void moderation_unauthenticated_shouldRejectBeforeUpdate(
        ModerationOperation operation) {

        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.empty());

        assertThrows(
            AuthenticationException.class,
            () -> invoke(operation));

        verify(securityFacade, never())
            .hasRole(ADMIN_ROLE);

        verifyNoInteractions(
            questionThreadRepository,
            questionThreadMapper,
            questionMessageRepository,
            questionMessageMapper,
            questionClaimFailureClassifier);
    }

    @ParameterizedTest
    @EnumSource(ModerationOperation.class)
    void moderation_nonAdministratorIncludingOrg_shouldRejectBeforeUpdate(
        ModerationOperation operation) {

        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.of(ADMINISTRATOR_ID));

        when(securityFacade.hasRole(ADMIN_ROLE))
            .thenReturn(false);

        assertThrows(
            AuthorizationException.class,
            () -> invoke(operation));

        verifyNoInteractions(
            questionThreadRepository,
            questionThreadMapper,
            questionMessageRepository,
            questionMessageMapper,
            questionClaimFailureClassifier);
    }

    @Test
    void updateVisibility_invalidInput_shouldRejectBeforeSecurityAndRepository() {
        assertThrows(
            ValidationException.class,
            () -> administratorQuestionService
                .updateVisibility(
                    null,
                    new UpdateQuestionVisibilityRequestDTO(
                        PUBLIC,
                        EXPECTED_VERSION)));

        assertThrows(
            ValidationException.class,
            () -> administratorQuestionService
                .updateVisibility(
                    0L,
                    new UpdateQuestionVisibilityRequestDTO(
                        PUBLIC,
                        EXPECTED_VERSION)));

        assertThrows(
            ValidationException.class,
            () -> administratorQuestionService
                .updateVisibility(
                    QUESTION_ID,
                    null));

        assertThrows(
            ValidationException.class,
            () -> administratorQuestionService
                .updateVisibility(
                    QUESTION_ID,
                    new UpdateQuestionVisibilityRequestDTO(
                        null,
                        EXPECTED_VERSION)));

        assertThrows(
            ValidationException.class,
            () -> administratorQuestionService
                .updateVisibility(
                    QUESTION_ID,
                    new UpdateQuestionVisibilityRequestDTO(
                        PUBLIC,
                        null)));

        assertThrows(
            ValidationException.class,
            () -> administratorQuestionService
                .updateVisibility(
                    QUESTION_ID,
                    new UpdateQuestionVisibilityRequestDTO(
                        PUBLIC,
                        -1L)));

        verifyNoInteractions(
            securityFacade,
            questionThreadRepository,
            questionThreadMapper);
    }

    @Test
    void updateStatus_invalidInput_shouldRejectBeforeSecurityAndRepository() {
        assertThrows(
            ValidationException.class,
            () -> administratorQuestionService
                .updateStatus(
                    QUESTION_ID,
                    null));

        assertThrows(
            ValidationException.class,
            () -> administratorQuestionService
                .updateStatus(
                    QUESTION_ID,
                    new UpdateQuestionStatusRequestDTO(
                        null,
                        EXPECTED_VERSION)));

        assertThrows(
            ValidationException.class,
            () -> administratorQuestionService
                .updateStatus(
                    QUESTION_ID,
                    new UpdateQuestionStatusRequestDTO(
                        IN_REVIEW,
                        null)));

        assertThrows(
            ValidationException.class,
            () -> administratorQuestionService
                .updateStatus(
                    QUESTION_ID,
                    new UpdateQuestionStatusRequestDTO(
                        IN_REVIEW,
                        -1L)));

        verifyNoInteractions(
            securityFacade,
            questionThreadRepository,
            questionThreadMapper);
    }

    @Test
    void updateState_invalidInput_shouldRejectBeforeSecurityAndRepository() {
        assertThrows(
            ValidationException.class,
            () -> administratorQuestionService
                .updateState(
                    QUESTION_ID,
                    null));

        assertThrows(
            ValidationException.class,
            () -> administratorQuestionService
                .updateState(
                    QUESTION_ID,
                    new UpdateQuestionStateRequestDTO(
                        null,
                        EXPECTED_VERSION)));

        assertThrows(
            ValidationException.class,
            () -> administratorQuestionService
                .updateState(
                    QUESTION_ID,
                    new UpdateQuestionStateRequestDTO(
                        CLOSED,
                        null)));

        assertThrows(
            ValidationException.class,
            () -> administratorQuestionService
                .updateState(
                    QUESTION_ID,
                    new UpdateQuestionStateRequestDTO(
                        CLOSED,
                        -1L)));

        verifyNoInteractions(
            securityFacade,
            questionThreadRepository,
            questionThreadMapper);
    }

    @ParameterizedTest
    @EnumSource(ModerationOperation.class)
    void moderationMethods_shouldUseWritableTransaction(
        ModerationOperation operation)
        throws NoSuchMethodException {

        Method method = switch (operation) {
            case VISIBILITY ->
                AdministratorQuestionServiceImpl.class.getMethod(
                    "updateVisibility",
                    Long.class,
                    UpdateQuestionVisibilityRequestDTO.class);
            case STATUS ->
                AdministratorQuestionServiceImpl.class.getMethod(
                    "updateStatus",
                    Long.class,
                    UpdateQuestionStatusRequestDTO.class);
            case STATE ->
                AdministratorQuestionServiceImpl.class.getMethod(
                    "updateState",
                    Long.class,
                    UpdateQuestionStateRequestDTO.class);
        };

        Transactional transactional =
            AnnotatedElementUtils.findMergedAnnotation(
                method,
                Transactional.class);

        assertNotNull(transactional);
        assertFalse(transactional.readOnly());
    }

    private QuestionThreadResponseDTO invoke(
        ModerationOperation operation) {

        return switch (operation) {
            case VISIBILITY ->
                administratorQuestionService.updateVisibility(
                    QUESTION_ID,
                    new UpdateQuestionVisibilityRequestDTO(
                        PUBLIC,
                        EXPECTED_VERSION));
            case STATUS ->
                administratorQuestionService.updateStatus(
                    QUESTION_ID,
                    new UpdateQuestionStatusRequestDTO(
                        IN_REVIEW,
                        EXPECTED_VERSION));
            case STATE ->
                administratorQuestionService.updateState(
                    QUESTION_ID,
                    new UpdateQuestionStateRequestDTO(
                        CLOSED,
                        EXPECTED_VERSION));
        };
    }

    private void stubUpdate(
        ModerationOperation operation,
        int updatedRows) {

        switch (operation) {
            case VISIBILITY ->
                when(questionThreadRepository
                    .updateVisibilityIfVersionMatches(
                        org.mockito.ArgumentMatchers.eq(QUESTION_ID),
                        org.mockito.ArgumentMatchers.eq(PUBLIC),
                        org.mockito.ArgumentMatchers.eq(EXPECTED_VERSION),
                        any(Instant.class)))
                    .thenReturn(updatedRows);
            case STATUS ->
                when(questionThreadRepository
                    .updateStatusIfVersionMatches(
                        org.mockito.ArgumentMatchers.eq(QUESTION_ID),
                        org.mockito.ArgumentMatchers.eq(IN_REVIEW),
                        org.mockito.ArgumentMatchers.eq(EXPECTED_VERSION),
                        any(Instant.class)))
                    .thenReturn(updatedRows);
            case STATE ->
                when(questionThreadRepository
                    .updateStateIfVersionMatches(
                        org.mockito.ArgumentMatchers.eq(QUESTION_ID),
                        org.mockito.ArgumentMatchers.eq(CLOSED),
                        org.mockito.ArgumentMatchers.eq(EXPECTED_VERSION),
                        any(Instant.class)))
                    .thenReturn(updatedRows);
        }
    }

    private void stubSequentialUpdate(
        ModerationOperation operation,
        int firstResult,
        int secondResult) {

        switch (operation) {
            case VISIBILITY ->
                when(questionThreadRepository
                    .updateVisibilityIfVersionMatches(
                        org.mockito.ArgumentMatchers.eq(QUESTION_ID),
                        org.mockito.ArgumentMatchers.eq(PUBLIC),
                        org.mockito.ArgumentMatchers.eq(EXPECTED_VERSION),
                        any(Instant.class)))
                    .thenReturn(
                        firstResult,
                        secondResult);
            case STATUS ->
                when(questionThreadRepository
                    .updateStatusIfVersionMatches(
                        org.mockito.ArgumentMatchers.eq(QUESTION_ID),
                        org.mockito.ArgumentMatchers.eq(IN_REVIEW),
                        org.mockito.ArgumentMatchers.eq(EXPECTED_VERSION),
                        any(Instant.class)))
                    .thenReturn(
                        firstResult,
                        secondResult);
            case STATE ->
                when(questionThreadRepository
                    .updateStateIfVersionMatches(
                        org.mockito.ArgumentMatchers.eq(QUESTION_ID),
                        org.mockito.ArgumentMatchers.eq(CLOSED),
                        org.mockito.ArgumentMatchers.eq(EXPECTED_VERSION),
                        any(Instant.class)))
                    .thenReturn(
                        firstResult,
                        secondResult);
        }
    }

    private void verifyUpdate(
        ModerationOperation operation,
        int invocationCount) {

        switch (operation) {
            case VISIBILITY ->
                verify(
                    questionThreadRepository,
                    times(invocationCount))
                    .updateVisibilityIfVersionMatches(
                        org.mockito.ArgumentMatchers.eq(QUESTION_ID),
                        org.mockito.ArgumentMatchers.eq(PUBLIC),
                        org.mockito.ArgumentMatchers.eq(EXPECTED_VERSION),
                        any(Instant.class));
            case STATUS ->
                verify(
                    questionThreadRepository,
                    times(invocationCount))
                    .updateStatusIfVersionMatches(
                        org.mockito.ArgumentMatchers.eq(QUESTION_ID),
                        org.mockito.ArgumentMatchers.eq(IN_REVIEW),
                        org.mockito.ArgumentMatchers.eq(EXPECTED_VERSION),
                        any(Instant.class));
            case STATE ->
                verify(
                    questionThreadRepository,
                    times(invocationCount))
                    .updateStateIfVersionMatches(
                        org.mockito.ArgumentMatchers.eq(QUESTION_ID),
                        org.mockito.ArgumentMatchers.eq(CLOSED),
                        org.mockito.ArgumentMatchers.eq(EXPECTED_VERSION),
                        any(Instant.class));
        }
    }

    private void stubAdministrator() {
        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.of(ADMINISTRATOR_ID));

        when(securityFacade.hasRole(ADMIN_ROLE))
            .thenReturn(true);
    }

    private QuestionThread createBaseQuestion() {
        return QuestionThread.builder()
            .id(QUESTION_ID)
            .taskAssignmentId(TASK_ASSIGNMENT_ID)
            .authorId(AUTHOR_ID)
            .assignedReviewerId(REVIEWER_ID)
            .title("Question title")
            .content("Question content")
            .status(ANSWERED)
            .visibility(PRIVATE)
            .state(OPEN)
            .version(EXPECTED_VERSION)
            .createdAt(CREATED_AT)
            .updatedAt(UPDATED_AT)
            .build();
    }

    private QuestionThread createUpdatedQuestion(
        ModerationOperation operation) {

        QuestionThread question =
            createBaseQuestion();

        switch (operation) {
            case VISIBILITY ->
                question.setVisibility(PUBLIC);
            case STATUS ->
                question.setStatus(IN_REVIEW);
            case STATE ->
                question.setState(CLOSED);
        }

        question.setVersion(UPDATED_VERSION);

        return question;
    }

    private QuestionThreadResponseDTO createResponse(
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

    private static Stream<Arguments> statusTransitions() {
        return Stream.of(
            Arguments.of(NEW, IN_REVIEW),
            Arguments.of(NEW, ANSWERED),
            Arguments.of(IN_REVIEW, NEW),
            Arguments.of(IN_REVIEW, ANSWERED),
            Arguments.of(ANSWERED, NEW),
            Arguments.of(ANSWERED, IN_REVIEW));
    }

    private enum ModerationOperation {
        VISIBILITY,
        STATUS,
        STATE
    }
}