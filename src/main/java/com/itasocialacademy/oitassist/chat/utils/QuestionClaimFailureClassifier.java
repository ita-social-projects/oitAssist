package com.itasocialacademy.oitassist.chat.utils;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.CLOSED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.ANSWERED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.IN_REVIEW;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import com.itasocialacademy.oitassist.chat.exceptions.InvalidQuestionStateException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionAlreadyClaimedException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionVersionConflictException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuestionClaimFailureClassifier {
    private static final String CLAIM_OPERATION =
        "claim for review";

    private final QuestionThreadRepository questionThreadRepository;

    /**
     * Classifies a failed global-administrator claim.
     *
     * @param questionId      question identifier
     * @param expectedVersion version supplied by the request
     */
    public void classifyAndThrow(
        Long questionId,
        Long expectedVersion) {
        QuestionThread question =
            requireQuestion(
                questionId);

        classifyPersistedStateAndThrow(
            question,
            expectedVersion);
    }

    /**
     * Classifies a failed responder-scoped claim.
     *
     * <p>
     * The question's persisted state is exposed to the classifier only when it
     * belongs to the exact TaskAssignment for which responder eligibility was
     * locked by the claim transaction.
     * </p>
     *
     * <p>
     * Missing and out-of-scope questions produce the same not-found exception to
     * avoid leaking protected question information.
     * </p>
     *
     * @param questionId                question identifier
     * @param expectedVersion           version supplied by the request
     * @param permittedTaskAssignmentId locked responder scope
     */
    public void classifyResponderClaimAndThrow(
        Long questionId,
        Long expectedVersion,
        Long permittedTaskAssignmentId) {
        QuestionThread question =
            requireQuestion(
                questionId);

        if (!Objects.equals(
            question.getTaskAssignmentId(),
            permittedTaskAssignmentId)) {
            throw new QuestionNotFoundException(
                questionId);
        }

        classifyPersistedStateAndThrow(
            question,
            expectedVersion);
    }

    private QuestionThread requireQuestion(
        Long questionId) {
        return questionThreadRepository
            .findById(questionId)
            .orElseThrow(() -> new QuestionNotFoundException(
                questionId));
    }

    private void classifyPersistedStateAndThrow(
        QuestionThread question,
        Long expectedVersion) {
        if (question.getState() == CLOSED) {
            throw new InvalidQuestionStateException(
                question.getId(),
                question.getState(),
                CLAIM_OPERATION);
        }

        if (question.getStatus() == ANSWERED) {
            throw new InvalidQuestionStateException(
                question.getId(),
                question.getStatus(),
                CLAIM_OPERATION);
        }

        /*
         * This check intentionally precedes the version check.
         *
         * When another reviewer wins a concurrent claim, the question usually has both
         * a changed version and an assigned reviewer. In that situation
         * QUESTION_ALREADY_CLAIMED is more precise than a generic version conflict.
         */
        if (question.getAssignedReviewerId() != null
            || question.getStatus() == IN_REVIEW) {
            throw new QuestionAlreadyClaimedException(
                question.getId());
        }

        if (!Objects.equals(
            question.getVersion(),
            expectedVersion)) {
            throw new QuestionVersionConflictException(
                question.getId());
        }

        /*
         * Defensive fallback. The atomic update returned zero, although the reloaded
         * question still appears claimable. Never retry and never report success.
         */
        throw new QuestionVersionConflictException(
            question.getId());
    }
}