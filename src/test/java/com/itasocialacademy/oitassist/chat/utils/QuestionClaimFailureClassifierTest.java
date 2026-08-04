package com.itasocialacademy.oitassist.chat.utils;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.CLOSED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.ANSWERED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.IN_REVIEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.NEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import com.itasocialacademy.oitassist.chat.exceptions.InvalidQuestionStateException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionAlreadyClaimedException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionVersionConflictException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestionClaimFailureClassifierTest {

    private static final Long QUESTION_ID = 10L;
    private static final Long REVIEWER_ID = 20L;

    private static final Long TASK_ASSIGNMENT_ID = 100L;
    private static final Long OTHER_TASK_ASSIGNMENT_ID = 101L;

    private static final Long EXPECTED_VERSION = 3L;

    @Mock
    private QuestionThreadRepository questionThreadRepository;

    @InjectMocks
    private QuestionClaimFailureClassifier classifier;

    @Test
    void classifyAndThrow_missingQuestion_shouldThrowNotFound() {

        when(questionThreadRepository
            .findById(QUESTION_ID))
            .thenReturn(Optional.empty());

        assertThrows(
            QuestionNotFoundException.class,
            () -> classifier.classifyAndThrow(
                QUESTION_ID,
                EXPECTED_VERSION));

        verify(questionThreadRepository)
            .findById(QUESTION_ID);

        verify(questionThreadRepository, never())
            .save(any(QuestionThread.class));
    }

    @Test
    void classifyAndThrow_assignedReviewer_shouldThrowAlreadyClaimed() {

        QuestionThread question =
            eligibleQuestion(
                EXPECTED_VERSION);

        question.setAssignedReviewerId(
            REVIEWER_ID);

        when(questionThreadRepository
            .findById(QUESTION_ID))
            .thenReturn(
                Optional.of(question));

        assertThrows(
            QuestionAlreadyClaimedException.class,
            () -> classifier.classifyAndThrow(
                QUESTION_ID,
                EXPECTED_VERSION));
    }

    @Test
    void classifyAndThrow_inReviewStatus_shouldThrowAlreadyClaimed() {

        QuestionThread question =
            eligibleQuestion(
                EXPECTED_VERSION);

        question.setStatus(
            IN_REVIEW);

        when(questionThreadRepository
            .findById(QUESTION_ID))
            .thenReturn(
                Optional.of(question));

        assertThrows(
            QuestionAlreadyClaimedException.class,
            () -> classifier.classifyAndThrow(
                QUESTION_ID,
                EXPECTED_VERSION));
    }

    @Test
    void classifyAndThrow_concurrentWinner_shouldPreferAlreadyClaimed() {

        QuestionThread question =
            eligibleQuestion(
                EXPECTED_VERSION + 1);

        question.setAssignedReviewerId(
            REVIEWER_ID);

        question.setStatus(
            IN_REVIEW);

        when(questionThreadRepository
            .findById(QUESTION_ID))
            .thenReturn(
                Optional.of(question));

        assertThrows(
            QuestionAlreadyClaimedException.class,
            () -> classifier.classifyAndThrow(
                QUESTION_ID,
                EXPECTED_VERSION));
    }

    @Test
    void classifyAndThrow_closedQuestion_shouldThrowInvalidState() {

        QuestionThread question =
            eligibleQuestion(
                EXPECTED_VERSION);

        question.setState(
            CLOSED);

        when(questionThreadRepository
            .findById(QUESTION_ID))
            .thenReturn(
                Optional.of(question));

        assertThrows(
            InvalidQuestionStateException.class,
            () -> classifier.classifyAndThrow(
                QUESTION_ID,
                EXPECTED_VERSION));
    }

    @Test
    void classifyAndThrow_answeredQuestion_shouldThrowInvalidState() {

        QuestionThread question =
            eligibleQuestion(
                EXPECTED_VERSION);

        question.setStatus(
            ANSWERED);

        when(questionThreadRepository
            .findById(QUESTION_ID))
            .thenReturn(
                Optional.of(question));

        assertThrows(
            InvalidQuestionStateException.class,
            () -> classifier.classifyAndThrow(
                QUESTION_ID,
                EXPECTED_VERSION));
    }

    @Test
    void classifyAndThrow_staleVersion_shouldThrowVersionConflict() {

        QuestionThread question =
            eligibleQuestion(
                EXPECTED_VERSION + 1);

        when(questionThreadRepository
            .findById(QUESTION_ID))
            .thenReturn(
                Optional.of(question));

        assertThrows(
            QuestionVersionConflictException.class,
            () -> classifier.classifyAndThrow(
                QUESTION_ID,
                EXPECTED_VERSION));
    }

    @Test
    void classifyAndThrow_sameVersionFallback_shouldThrowVersionConflict() {

        QuestionThread question =
            eligibleQuestion(
                EXPECTED_VERSION);

        when(questionThreadRepository
            .findById(QUESTION_ID))
            .thenReturn(
                Optional.of(question));

        assertThrows(
            QuestionVersionConflictException.class,
            () -> classifier.classifyAndThrow(
                QUESTION_ID,
                EXPECTED_VERSION));

        verify(questionThreadRepository, never())
            .save(any(QuestionThread.class));
    }

    @Test
    void classifyAndThrow_shouldNotMutateQuestion() {

        QuestionThread question =
            eligibleQuestion(
                EXPECTED_VERSION + 1);

        Long originalVersion =
            question.getVersion();

        when(questionThreadRepository
            .findById(QUESTION_ID))
            .thenReturn(
                Optional.of(question));

        assertThrows(
            QuestionVersionConflictException.class,
            () -> classifier.classifyAndThrow(
                QUESTION_ID,
                EXPECTED_VERSION));

        assertEquals(
            originalVersion,
            question.getVersion());

        assertEquals(
            NEW,
            question.getStatus());

        assertEquals(
            OPEN,
            question.getState());
    }

    @Test
    void classifyResponderClaim_missingQuestion_shouldReturnMaskedNotFound() {

        when(questionThreadRepository
            .findById(QUESTION_ID))
            .thenReturn(Optional.empty());

        assertThrows(
            QuestionNotFoundException.class,
            () -> classifier
                .classifyResponderClaimAndThrow(
                    QUESTION_ID,
                    EXPECTED_VERSION,
                    TASK_ASSIGNMENT_ID));
    }

    @Test
    void classifyResponderClaim_otherAssignment_shouldReturnMaskedNotFound() {

        QuestionThread question =
            eligibleQuestion(
                EXPECTED_VERSION);

        question.setTaskAssignmentId(
            OTHER_TASK_ASSIGNMENT_ID);

        when(questionThreadRepository
            .findById(QUESTION_ID))
            .thenReturn(
                Optional.of(question));

        assertThrows(
            QuestionNotFoundException.class,
            () -> classifier
                .classifyResponderClaimAndThrow(
                    QUESTION_ID,
                    EXPECTED_VERSION,
                    TASK_ASSIGNMENT_ID));
    }

    @Test
    void classifyResponderClaim_inScopeStaleVersion_shouldReturnConflict() {

        QuestionThread question =
            eligibleQuestion(
                EXPECTED_VERSION + 1);

        when(questionThreadRepository
            .findById(QUESTION_ID))
            .thenReturn(
                Optional.of(question));

        assertThrows(
            QuestionVersionConflictException.class,
            () -> classifier
                .classifyResponderClaimAndThrow(
                    QUESTION_ID,
                    EXPECTED_VERSION,
                    TASK_ASSIGNMENT_ID));
    }

    @Test
    void classifyResponderClaim_inScopeAlreadyClaimed_shouldReturnConflict() {

        QuestionThread question =
            eligibleQuestion(
                EXPECTED_VERSION);

        question.setAssignedReviewerId(
            REVIEWER_ID);

        question.setStatus(
            IN_REVIEW);

        when(questionThreadRepository
            .findById(QUESTION_ID))
            .thenReturn(
                Optional.of(question));

        assertThrows(
            QuestionAlreadyClaimedException.class,
            () -> classifier
                .classifyResponderClaimAndThrow(
                    QUESTION_ID,
                    EXPECTED_VERSION,
                    TASK_ASSIGNMENT_ID));
    }

    private QuestionThread eligibleQuestion(
        Long version) {

        return QuestionThread.builder()
            .id(QUESTION_ID)
            .taskAssignmentId(
                TASK_ASSIGNMENT_ID)
            .authorId(200L)
            .title("Question title")
            .content("Question content")
            .status(NEW)
            .state(OPEN)
            .visibility(PRIVATE)
            .version(version)
            .build();
    }
}