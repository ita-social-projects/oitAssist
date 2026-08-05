package com.itasocialacademy.oitassist.chat.utils;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.IN_REVIEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import com.itasocialacademy.oitassist.chat.dao.repository.TaskAssignmentForumResponderRepository;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionVersionConflictException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationQuestionModerationFailureClassifierTest {

    private static final Long QUESTION_ID = 10L;
    private static final Long TASK_ASSIGNMENT_ID = 20L;
    private static final Long RESPONDER_ID = 30L;

    @Mock
    private QuestionThreadRepository questionThreadRepository;

    @Mock
    private TaskAssignmentForumResponderRepository responderRepository;

    @InjectMocks
    private OrganizationQuestionModerationFailureClassifier classifier;

    @Test
    void classify_missingQuestion_shouldReturnNotFound() {

        when(questionThreadRepository.findById(
            QUESTION_ID))
            .thenReturn(
                Optional.empty());

        assertThrows(
            QuestionNotFoundException.class,
            () -> classifier.classifyAndThrow(
                QUESTION_ID,
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID));

        verify(
            responderRepository,
            never())
            .existsByTaskAssignmentIdAndResponderUserId(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID);
    }

    @Test
    void classify_differentTaskAssignment_shouldReturnNotFound() {

        QuestionThread question =
            question();

        question.setTaskAssignmentId(
            999L);

        when(questionThreadRepository.findById(
            QUESTION_ID))
            .thenReturn(
                Optional.of(
                    question));

        assertThrows(
            QuestionNotFoundException.class,
            () -> classifier.classifyAndThrow(
                QUESTION_ID,
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID));

        verify(
            responderRepository,
            never())
            .existsByTaskAssignmentIdAndResponderUserId(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID);
    }

    @Test
    void classify_reviewerChanged_shouldReturnNotFound() {

        QuestionThread question =
            question();

        question.setAssignedReviewerId(
            999L);

        when(questionThreadRepository.findById(
            QUESTION_ID))
            .thenReturn(
                Optional.of(
                    question));

        assertThrows(
            QuestionNotFoundException.class,
            () -> classifier.classifyAndThrow(
                QUESTION_ID,
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID));

        verify(
            responderRepository,
            never())
            .existsByTaskAssignmentIdAndResponderUserId(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID);
    }

    @Test
    void classify_responderGrantMissing_shouldReturnNotFound() {

        when(questionThreadRepository.findById(
            QUESTION_ID))
            .thenReturn(
                Optional.of(
                    question()));

        when(responderRepository
            .existsByTaskAssignmentIdAndResponderUserId(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(false);

        assertThrows(
            QuestionNotFoundException.class,
            () -> classifier.classifyAndThrow(
                QUESTION_ID,
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID));
    }

    @Test
    void classify_accessStillValid_shouldReturnVersionConflict() {

        when(questionThreadRepository.findById(
            QUESTION_ID))
            .thenReturn(
                Optional.of(
                    question()));

        when(responderRepository
            .existsByTaskAssignmentIdAndResponderUserId(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(true);

        assertThrows(
            QuestionVersionConflictException.class,
            () -> classifier.classifyAndThrow(
                QUESTION_ID,
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID));
    }

    private QuestionThread question() {

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
                PRIVATE)
            .status(
                IN_REVIEW)
            .state(
                OPEN)
            .version(
                4L)
            .build();
    }
}