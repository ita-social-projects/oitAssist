package com.itasocialacademy.oitassist.chat.utils;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.CLOSED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.ANSWERED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.IN_REVIEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PUBLIC;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.model.TaskAssignmentForumResponder;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import com.itasocialacademy.oitassist.chat.dao.repository.TaskAssignmentForumResponderRepository;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionVersionConflictException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationQuestionModerationCoordinatorTest {

    private static final Long QUESTION_ID = 10L;
    private static final Long TASK_ASSIGNMENT_ID = 20L;
    private static final Long RESPONDER_ID = 30L;
    private static final Long OTHER_RESPONDER_ID = 31L;
    private static final Long VERSION = 3L;

    private static final Instant UPDATED_AT =
        Instant.parse(
            "2026-08-05T10:20:00Z");

    @Mock
    private QuestionThreadRepository questionThreadRepository;

    @Mock
    private TaskAssignmentForumResponderRepository responderRepository;

    @Mock
    private OrganizationQuestionModerationFailureClassifier failureClassifier;

    @InjectMocks
    private OrganizationQuestionModerationCoordinator coordinator;

    @Test
    void updateVisibility_validRequest_shouldLockGrantUpdateAndReload() {

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

        stubResponderGrant();

        when(questionThreadRepository
            .updateVisibilityAsResponderIfVersionMatches(
                QUESTION_ID,
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID,
                PUBLIC,
                VERSION,
                UPDATED_AT))
            .thenReturn(1);

        when(questionThreadRepository.findById(
            QUESTION_ID))
            .thenReturn(
                Optional.of(
                    updatedQuestion));

        QuestionThread result =
            coordinator.updateVisibility(
                currentQuestion,
                RESPONDER_ID,
                PUBLIC,
                VERSION,
                UPDATED_AT);

        assertSame(
            updatedQuestion,
            result);
    }

    @Test
    void updateStatus_validRequest_shouldUseStatusOnlyUpdate() {

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

        stubResponderGrant();

        when(questionThreadRepository
            .updateStatusAsResponderIfVersionMatches(
                QUESTION_ID,
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID,
                ANSWERED,
                VERSION,
                UPDATED_AT))
            .thenReturn(1);

        when(questionThreadRepository.findById(
            QUESTION_ID))
            .thenReturn(
                Optional.of(
                    updatedQuestion));

        QuestionThread result =
            coordinator.updateStatus(
                currentQuestion,
                RESPONDER_ID,
                ANSWERED,
                VERSION,
                UPDATED_AT);

        assertSame(
            updatedQuestion,
            result);

        verify(
            questionThreadRepository,
            never())
            .updateVisibilityAsResponderIfVersionMatches(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());

        verify(
            questionThreadRepository,
            never())
            .updateStateAsResponderIfVersionMatches(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateState_validRequest_shouldUseStateOnlyUpdate() {

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

        stubResponderGrant();

        when(questionThreadRepository
            .updateStateAsResponderIfVersionMatches(
                QUESTION_ID,
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID,
                CLOSED,
                VERSION,
                UPDATED_AT))
            .thenReturn(1);

        when(questionThreadRepository.findById(
            QUESTION_ID))
            .thenReturn(
                Optional.of(
                    updatedQuestion));

        QuestionThread result =
            coordinator.updateState(
                currentQuestion,
                RESPONDER_ID,
                CLOSED,
                VERSION,
                UPDATED_AT);

        assertSame(
            updatedQuestion,
            result);
    }

    @Test
    void moderation_questionAssignedToAnotherResponder_shouldMaskBeforeGrantQuery() {

        QuestionThread currentQuestion =
            question(
                PRIVATE,
                IN_REVIEW,
                OPEN,
                VERSION);

        currentQuestion.setAssignedReviewerId(
            OTHER_RESPONDER_ID);

        assertThrows(
            QuestionNotFoundException.class,
            () -> coordinator.updateVisibility(
                currentQuestion,
                RESPONDER_ID,
                PUBLIC,
                VERSION,
                UPDATED_AT));

        verifyNoInteractions(
            responderRepository,
            failureClassifier);

        verifyNoInteractions(
            questionThreadRepository);
    }

    @Test
    void moderation_missingResponderGrant_shouldMaskBeforeUpdate() {

        QuestionThread currentQuestion =
            question(
                PRIVATE,
                IN_REVIEW,
                OPEN,
                VERSION);

        when(responderRepository
            .findByTaskAssignmentIdAndResponderUserIdForUpdate(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(
                Optional.empty());

        assertThrows(
            QuestionNotFoundException.class,
            () -> coordinator.updateStatus(
                currentQuestion,
                RESPONDER_ID,
                ANSWERED,
                VERSION,
                UPDATED_AT));

        verifyNoInteractions(
            questionThreadRepository,
            failureClassifier);
    }

    @Test
    void moderation_zeroRows_shouldDelegateToClassifier() {

        QuestionThread currentQuestion =
            question(
                PRIVATE,
                IN_REVIEW,
                OPEN,
                VERSION);

        stubResponderGrant();

        when(questionThreadRepository
            .updateVisibilityAsResponderIfVersionMatches(
                QUESTION_ID,
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID,
                PUBLIC,
                VERSION,
                UPDATED_AT))
            .thenReturn(0);

        QuestionVersionConflictException conflict =
            new QuestionVersionConflictException(
                QUESTION_ID);

        org.mockito.Mockito.doThrow(
            conflict)
            .when(failureClassifier)
            .classifyAndThrow(
                QUESTION_ID,
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID);

        QuestionVersionConflictException result =
            assertThrows(
                QuestionVersionConflictException.class,
                () -> coordinator.updateVisibility(
                    currentQuestion,
                    RESPONDER_ID,
                    PUBLIC,
                    VERSION,
                    UPDATED_AT));

        assertSame(
            conflict,
            result);
    }

    @Test
    void moderation_unexpectedUpdatedRowCount_shouldFail() {

        QuestionThread currentQuestion =
            question(
                PRIVATE,
                IN_REVIEW,
                OPEN,
                VERSION);

        stubResponderGrant();

        when(questionThreadRepository
            .updateStatusAsResponderIfVersionMatches(
                QUESTION_ID,
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID,
                ANSWERED,
                VERSION,
                UPDATED_AT))
            .thenReturn(2);

        assertThrows(
            IllegalStateException.class,
            () -> coordinator.updateStatus(
                currentQuestion,
                RESPONDER_ID,
                ANSWERED,
                VERSION,
                UPDATED_AT));

        verifyNoInteractions(
            failureClassifier);
    }

    private void stubResponderGrant() {

        when(responderRepository
            .findByTaskAssignmentIdAndResponderUserIdForUpdate(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(
                Optional.of(
                    TaskAssignmentForumResponder.builder()
                        .id(1L)
                        .taskAssignmentId(
                            TASK_ASSIGNMENT_ID)
                        .responderUserId(
                            RESPONDER_ID)
                        .build()));
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
                40L)
            .assignedReviewerId(
                RESPONDER_ID)
            .title(
                "Question")
            .content(
                "Content")
            .visibility(
                visibility)
            .status(
                status)
            .state(
                state)
            .version(
                version)
            .build();
    }
}