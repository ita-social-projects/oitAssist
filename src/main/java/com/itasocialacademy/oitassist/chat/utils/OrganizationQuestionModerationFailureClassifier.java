package com.itasocialacademy.oitassist.chat.utils;

import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import com.itasocialacademy.oitassist.chat.dao.repository.TaskAssignmentForumResponderRepository;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionVersionConflictException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Classifies a failed optimistic moderation update without exposing questions
 * outside the current responder's assigned-review scope.
 */
@Component
@RequiredArgsConstructor
public class OrganizationQuestionModerationFailureClassifier {
    private final QuestionThreadRepository questionThreadRepository;

    private final TaskAssignmentForumResponderRepository responderRepository;

    /**
     * Classifies a zero-row responder moderation update.
     *
     * <p>
     * Missing questions, ownership changes and missing responder grants are all
     * safely represented as {@code QUESTION_NOT_FOUND}. When the question remains
     * accessible, the failed update is represented as an optimistic version
     * conflict.
     * </p>
     */
    public void classifyAndThrow(
        Long questionId,
        Long taskAssignmentId,
        Long responderUserId) {
        QuestionThread currentQuestion =
            questionThreadRepository
                .findById(
                    questionId)
                .orElseThrow(() -> new QuestionNotFoundException(
                    questionId));

        boolean sameTaskAssignment =
            Objects.equals(
                taskAssignmentId,
                currentQuestion.getTaskAssignmentId());

        boolean stillAssigned =
            Objects.equals(
                responderUserId,
                currentQuestion.getAssignedReviewerId());

        if (!sameTaskAssignment
            || !stillAssigned) {
            throw new QuestionNotFoundException(
                questionId);
        }

        boolean stillEligible =
            responderRepository
                .existsByTaskAssignmentIdAndResponderUserId(
                    taskAssignmentId,
                    responderUserId);

        if (!stillEligible) {
            throw new QuestionNotFoundException(
                questionId);
        }

        /*
         * The question remains inside the caller's protected scope. Therefore a
         * zero-row optimistic update represents a stale or concurrently lost version.
         */
        throw new QuestionVersionConflictException(
            questionId);
    }
}