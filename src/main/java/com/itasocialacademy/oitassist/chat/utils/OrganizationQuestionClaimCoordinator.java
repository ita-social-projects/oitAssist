package com.itasocialacademy.oitassist.chat.utils;

import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import com.itasocialacademy.oitassist.chat.dao.repository.TaskAssignmentForumResponderRepository;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionVersionConflictException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates eligibility locking and atomic question claiming for an
 * organizing-committee responder.
 */
@Component
@RequiredArgsConstructor
public class OrganizationQuestionClaimCoordinator {
    private final QuestionThreadRepository questionThreadRepository;

    private final TaskAssignmentForumResponderRepository responderRepository;

    private final QuestionClaimFailureClassifier failureClassifier;

    /**
     * Claims a question for one exact TaskAssignment responder.
     *
     * <p>
     * The responder-assignment row is locked before the atomic question update.
     * Responder revocation uses the same row lock, preventing a successful claim
     * from being committed together with removal of the required eligibility.
     * </p>
     *
     * @param questionId      question identifier
     * @param responderUserId current authenticated responder
     * @param expectedVersion expected question version
     * @param updatedAt       server-controlled mutation timestamp
     * @return claimed and reloaded question
     */
    @Transactional
    public QuestionThread claimQuestion(
        Long questionId,
        Long responderUserId,
        Long expectedVersion,
        Instant updatedAt) {
        Long taskAssignmentId =
            questionThreadRepository
                .findTaskAssignmentIdByQuestionId(
                    questionId)
                .orElseThrow(() -> new QuestionNotFoundException(
                    questionId));

        /*
         * This is the same row lock used by responder revocation.
         *
         * Absence of the assignment is intentionally represented as QUESTION_NOT_FOUND
         * so the caller cannot determine whether the protected question exists outside
         * their responder scope.
         */
        responderRepository
            .findByTaskAssignmentIdAndResponderUserIdForUpdate(
                taskAssignmentId,
                responderUserId)
            .orElseThrow(() -> new QuestionNotFoundException(
                questionId));

        int updatedRows =
            questionThreadRepository
                .claimForReviewAsResponder(
                    questionId,
                    responderUserId,
                    taskAssignmentId,
                    expectedVersion,
                    updatedAt);

        if (updatedRows == 0) {
            failureClassifier
                .classifyResponderClaimAndThrow(
                    questionId,
                    expectedVersion,
                    taskAssignmentId);

            /*
             * The classifier contract requires every branch to throw. Keep a defensive
             * fallback in case that contract is broken.
             */
            throw new QuestionVersionConflictException(
                questionId);
        }

        if (updatedRows != 1) {
            throw new IllegalStateException(
                ("Question claim affected an unexpected "
                    + "number of rows: questionId=%s, rows=%s").formatted(
                        questionId,
                        updatedRows));
        }

        return questionThreadRepository
            .findById(questionId)
            .orElseThrow(() -> new QuestionNotFoundException(
                questionId));
    }
}