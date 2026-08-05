package com.itasocialacademy.oitassist.chat.utils;

import com.itasocialacademy.oitassist.chat.dao.enums.QuestionState;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import com.itasocialacademy.oitassist.chat.dao.repository.TaskAssignmentForumResponderRepository;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionVersionConflictException;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates responder-grant locking and optimistic question moderation.
 */
@Component
@RequiredArgsConstructor
public class OrganizationQuestionModerationCoordinator {
    private final QuestionThreadRepository questionThreadRepository;

    private final TaskAssignmentForumResponderRepository responderRepository;

    private final OrganizationQuestionModerationFailureClassifier failureClassifier;

    @Transactional
    public QuestionThread updateVisibility(
        QuestionThread currentQuestion,
        Long responderUserId,
        QuestionVisibility visibility,
        Long expectedVersion,
        Instant updatedAt) {
        requireAssignedResponder(
            currentQuestion,
            responderUserId);

        int updatedRows =
            questionThreadRepository
                .updateVisibilityAsResponderIfVersionMatches(
                    currentQuestion.getId(),
                    currentQuestion.getTaskAssignmentId(),
                    responderUserId,
                    visibility,
                    expectedVersion,
                    updatedAt);
        return completeUpdate(
            currentQuestion,
            responderUserId,
            updatedRows,
            "visibility");
    }

    @Transactional
    public QuestionThread updateStatus(
        QuestionThread currentQuestion,
        Long responderUserId,
        QuestionStatus status,
        Long expectedVersion,
        Instant updatedAt) {
        requireAssignedResponder(
            currentQuestion,
            responderUserId);

        int updatedRows =
            questionThreadRepository
                .updateStatusAsResponderIfVersionMatches(
                    currentQuestion.getId(),
                    currentQuestion.getTaskAssignmentId(),
                    responderUserId,
                    status,
                    expectedVersion,
                    updatedAt);
        return completeUpdate(
            currentQuestion,
            responderUserId,
            updatedRows,
            "status");
    }

    @Transactional
    public QuestionThread updateState(
        QuestionThread currentQuestion,
        Long responderUserId,
        QuestionState state,
        Long expectedVersion,
        Instant updatedAt) {
        requireAssignedResponder(
            currentQuestion,
            responderUserId);

        int updatedRows =
            questionThreadRepository
                .updateStateAsResponderIfVersionMatches(
                    currentQuestion.getId(),
                    currentQuestion.getTaskAssignmentId(),
                    responderUserId,
                    state,
                    expectedVersion,
                    updatedAt);
        return completeUpdate(
            currentQuestion,
            responderUserId,
            updatedRows,
            "state");
    }

    private void requireAssignedResponder(
        QuestionThread question,
        Long responderUserId) {
        if (!Objects.equals(
            responderUserId,
            question.getAssignedReviewerId())) {
            throw new QuestionNotFoundException(
                question.getId());
        }

        /*
         * Responder revocation uses the same pessimistic row lock. Therefore the
         * required grant cannot disappear between authorization and mutation.
         */
        responderRepository
            .findByTaskAssignmentIdAndResponderUserIdForUpdate(
                question.getTaskAssignmentId(),
                responderUserId)
            .orElseThrow(() -> new QuestionNotFoundException(
                question.getId()));
    }

    private QuestionThread completeUpdate(
        QuestionThread previousQuestion,
        Long responderUserId,
        int updatedRows,
        String operation) {
        if (updatedRows == 0) {
            failureClassifier.classifyAndThrow(
                previousQuestion.getId(),
                previousQuestion.getTaskAssignmentId(),
                responderUserId);
            /*
             * Every classifier branch must throw.
             */
            throw new QuestionVersionConflictException(
                previousQuestion.getId());
        }

        if (updatedRows != 1) {
            throw new IllegalStateException(
                ("Responder moderation affected an unexpected "
                    + "number of rows: questionId=%s, "
                    + "operation=%s, rows=%s")
                    .formatted(
                        previousQuestion.getId(),
                        operation,
                        updatedRows));
        }
        return questionThreadRepository
            .findById(
                previousQuestion.getId())
            .orElseThrow(() -> new QuestionNotFoundException(
                previousQuestion.getId()));
    }
}