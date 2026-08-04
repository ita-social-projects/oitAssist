package com.itasocialacademy.oitassist.chat.utils;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.IN_REVIEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.model.TaskAssignmentForumResponder;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import com.itasocialacademy.oitassist.chat.dao.repository.TaskAssignmentForumResponderRepository;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionAlreadyClaimedException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionVersionConflictException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationQuestionClaimCoordinatorTest {

    private static final Long QUESTION_ID = 10L;
    private static final Long TASK_ASSIGNMENT_ID = 20L;
    private static final Long RESPONDER_ID = 30L;
    private static final Long ADMINISTRATOR_ID = 40L;
    private static final Long EXPECTED_VERSION = 3L;

    private static final Instant UPDATED_AT =
        Instant.parse(
            "2026-08-05T12:00:00Z");

    @Mock
    private QuestionThreadRepository questionThreadRepository;

    @Mock
    private TaskAssignmentForumResponderRepository responderRepository;

    @Mock
    private QuestionClaimFailureClassifier failureClassifier;

    @InjectMocks
    private OrganizationQuestionClaimCoordinator coordinator;

    @Test
    void claimQuestion_success_shouldLockEligibilityBeforeAtomicUpdate() {

        TaskAssignmentForumResponder responder =
            responderAssignment();

        QuestionThread claimedQuestion =
            claimedQuestion();

        when(questionThreadRepository
            .findTaskAssignmentIdByQuestionId(
                QUESTION_ID))
            .thenReturn(
                Optional.of(
                    TASK_ASSIGNMENT_ID));

        when(responderRepository
            .findByTaskAssignmentIdAndResponderUserIdForUpdate(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(
                Optional.of(
                    responder));

        when(questionThreadRepository
            .claimForReviewAsResponder(
                QUESTION_ID,
                RESPONDER_ID,
                TASK_ASSIGNMENT_ID,
                EXPECTED_VERSION,
                UPDATED_AT))
            .thenReturn(1);

        when(questionThreadRepository
            .findById(QUESTION_ID))
            .thenReturn(
                Optional.of(
                    claimedQuestion));

        QuestionThread result =
            coordinator.claimQuestion(
                QUESTION_ID,
                RESPONDER_ID,
                EXPECTED_VERSION,
                UPDATED_AT);

        assertSame(
            claimedQuestion,
            result);

        assertEquals(
            RESPONDER_ID,
            result.getAssignedReviewerId());

        assertEquals(
            IN_REVIEW,
            result.getStatus());

        assertEquals(
            EXPECTED_VERSION + 1,
            result.getVersion());

        InOrder order =
            inOrder(
                questionThreadRepository,
                responderRepository);

        order.verify(questionThreadRepository)
            .findTaskAssignmentIdByQuestionId(
                QUESTION_ID);

        order.verify(responderRepository)
            .findByTaskAssignmentIdAndResponderUserIdForUpdate(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID);

        order.verify(questionThreadRepository)
            .claimForReviewAsResponder(
                QUESTION_ID,
                RESPONDER_ID,
                TASK_ASSIGNMENT_ID,
                EXPECTED_VERSION,
                UPDATED_AT);

        order.verify(questionThreadRepository)
            .findById(QUESTION_ID);

        verifyNoInteractions(
            failureClassifier);

        verify(questionThreadRepository, never())
            .save(any(QuestionThread.class));
    }

    @Test
    void claimQuestion_missingQuestion_shouldReturnMaskedNotFound() {

        when(questionThreadRepository
            .findTaskAssignmentIdByQuestionId(
                QUESTION_ID))
            .thenReturn(Optional.empty());

        assertThrows(
            QuestionNotFoundException.class,
            () -> coordinator.claimQuestion(
                QUESTION_ID,
                RESPONDER_ID,
                EXPECTED_VERSION,
                UPDATED_AT));

        verifyNoInteractions(
            responderRepository,
            failureClassifier);

        verify(questionThreadRepository, never())
            .claimForReviewAsResponder(
                any(),
                any(),
                any(),
                any(),
                any());
    }

    @Test
    void claimQuestion_missingEligibility_shouldReturnMaskedNotFound() {

        when(questionThreadRepository
            .findTaskAssignmentIdByQuestionId(
                QUESTION_ID))
            .thenReturn(
                Optional.of(
                    TASK_ASSIGNMENT_ID));

        when(responderRepository
            .findByTaskAssignmentIdAndResponderUserIdForUpdate(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(Optional.empty());

        assertThrows(
            QuestionNotFoundException.class,
            () -> coordinator.claimQuestion(
                QUESTION_ID,
                RESPONDER_ID,
                EXPECTED_VERSION,
                UPDATED_AT));

        verify(questionThreadRepository, never())
            .claimForReviewAsResponder(
                any(),
                any(),
                any(),
                any(),
                any());

        verifyNoInteractions(
            failureClassifier);
    }

    @Test
    void claimQuestion_zeroRows_shouldUseScopedFailureClassifier() {

        QuestionAlreadyClaimedException failure =
            new QuestionAlreadyClaimedException(
                QUESTION_ID);

        prepareEligibility();

        when(questionThreadRepository
            .claimForReviewAsResponder(
                QUESTION_ID,
                RESPONDER_ID,
                TASK_ASSIGNMENT_ID,
                EXPECTED_VERSION,
                UPDATED_AT))
            .thenReturn(0);

        doThrow(failure)
            .when(failureClassifier)
            .classifyResponderClaimAndThrow(
                QUESTION_ID,
                EXPECTED_VERSION,
                TASK_ASSIGNMENT_ID);

        RuntimeException result =
            assertThrows(
                QuestionAlreadyClaimedException.class,
                () -> coordinator.claimQuestion(
                    QUESTION_ID,
                    RESPONDER_ID,
                    EXPECTED_VERSION,
                    UPDATED_AT));

        assertSame(
            failure,
            result);

        verify(failureClassifier)
            .classifyResponderClaimAndThrow(
                QUESTION_ID,
                EXPECTED_VERSION,
                TASK_ASSIGNMENT_ID);

        verify(questionThreadRepository, never())
            .findById(QUESTION_ID);

        verify(questionThreadRepository, never())
            .save(any(QuestionThread.class));
    }

    @Test
    void claimQuestion_classifierUnexpectedlyReturns_shouldUseFallback() {

        prepareEligibility();

        when(questionThreadRepository
            .claimForReviewAsResponder(
                QUESTION_ID,
                RESPONDER_ID,
                TASK_ASSIGNMENT_ID,
                EXPECTED_VERSION,
                UPDATED_AT))
            .thenReturn(0);

        assertThrows(
            QuestionVersionConflictException.class,
            () -> coordinator.claimQuestion(
                QUESTION_ID,
                RESPONDER_ID,
                EXPECTED_VERSION,
                UPDATED_AT));

        verify(failureClassifier)
            .classifyResponderClaimAndThrow(
                QUESTION_ID,
                EXPECTED_VERSION,
                TASK_ASSIGNMENT_ID);

        verify(questionThreadRepository, never())
            .findById(QUESTION_ID);
    }

    @Test
    void claimQuestion_unexpectedUpdatedRowCount_shouldReject() {

        prepareEligibility();

        when(questionThreadRepository
            .claimForReviewAsResponder(
                QUESTION_ID,
                RESPONDER_ID,
                TASK_ASSIGNMENT_ID,
                EXPECTED_VERSION,
                UPDATED_AT))
            .thenReturn(2);

        assertThrows(
            IllegalStateException.class,
            () -> coordinator.claimQuestion(
                QUESTION_ID,
                RESPONDER_ID,
                EXPECTED_VERSION,
                UPDATED_AT));

        verifyNoInteractions(
            failureClassifier);

        verify(questionThreadRepository, never())
            .findById(QUESTION_ID);
    }

    @Test
    void claimQuestion_atomicUpdateFailure_shouldPropagateWithoutClassification() {

        prepareEligibility();

        RuntimeException repositoryFailure =
            new RuntimeException(
                "Database failure");

        when(questionThreadRepository
            .claimForReviewAsResponder(
                QUESTION_ID,
                RESPONDER_ID,
                TASK_ASSIGNMENT_ID,
                EXPECTED_VERSION,
                UPDATED_AT))
            .thenThrow(
                repositoryFailure);

        RuntimeException result =
            assertThrows(
                RuntimeException.class,
                () -> coordinator.claimQuestion(
                    QUESTION_ID,
                    RESPONDER_ID,
                    EXPECTED_VERSION,
                    UPDATED_AT));

        assertSame(
            repositoryFailure,
            result);

        verifyNoInteractions(
            failureClassifier);

        verify(questionThreadRepository, never())
            .findById(QUESTION_ID);
    }

    private void prepareEligibility() {

        when(questionThreadRepository
            .findTaskAssignmentIdByQuestionId(
                QUESTION_ID))
            .thenReturn(
                Optional.of(
                    TASK_ASSIGNMENT_ID));

        when(responderRepository
            .findByTaskAssignmentIdAndResponderUserIdForUpdate(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(
                Optional.of(
                    responderAssignment()));
    }

    private TaskAssignmentForumResponder responderAssignment() {

        return TaskAssignmentForumResponder.builder()
            .id(1L)
            .taskAssignmentId(
                TASK_ASSIGNMENT_ID)
            .responderUserId(
                RESPONDER_ID)
            .assignedByUserId(
                ADMINISTRATOR_ID)
            .assignedAt(
                UPDATED_AT)
            .build();
    }

    private QuestionThread claimedQuestion() {

        return QuestionThread.builder()
            .id(QUESTION_ID)
            .taskAssignmentId(
                TASK_ASSIGNMENT_ID)
            .authorId(50L)
            .assignedReviewerId(
                RESPONDER_ID)
            .title("Question title")
            .content("Question content")
            .status(IN_REVIEW)
            .state(OPEN)
            .visibility(PRIVATE)
            .version(
                EXPECTED_VERSION + 1)
            .updatedAt(
                UPDATED_AT)
            .build();
    }
}