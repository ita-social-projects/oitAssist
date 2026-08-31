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
    private static final String CLAIM_OPERATION = "claim for review";

    private final QuestionThreadRepository questionThreadRepository;

    /**
     * Reloads the current persisted question and throws the domain exception
     * corresponding to the unsuccessful claim.
     *
     * @param questionId      question identifier
     * @param expectedVersion version supplied by the claim request
     */
    public void classifyAndThrow(Long questionId, Long expectedVersion) {
        QuestionThread question = questionThreadRepository.findById(questionId)
            .orElseThrow(() -> new QuestionNotFoundException(questionId));

        if (question.getState() == CLOSED) {
            throw new InvalidQuestionStateException(
                questionId,
                question.getState(),
                CLAIM_OPERATION);
        }

        if (question.getStatus() == ANSWERED) {
            throw new InvalidQuestionStateException(
                questionId,
                question.getStatus(),
                CLAIM_OPERATION);
        }

        if (question.getAssignedReviewerId() != null || question.getStatus() == IN_REVIEW) {
            throw new QuestionAlreadyClaimedException(questionId);
        }

        if (!Objects.equals(question.getVersion(), expectedVersion)) {
            throw new QuestionVersionConflictException(questionId);
        }

        /*
         * Defensive fallback: the atomic update reported zero rows even though the
         * reloaded question still appears claimable. Never retry and never report
         * success.
         */
        throw new QuestionVersionConflictException(questionId);
    }
}