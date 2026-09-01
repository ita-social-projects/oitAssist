package com.itasocialacademy.oitassist.chat.service;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.CLOSED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.ANSWERED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.IN_REVIEW;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import com.itasocialacademy.oitassist.chat.dao.repository.TaskAssignmentForumResponderRepository;
import com.itasocialacademy.oitassist.chat.exceptions.InvalidQuestionStateException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionAlreadyClaimedException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionVersionConflictException;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuestionClaimService {
    private static final String CLAIM_OPERATION = "claim for review";

    private final QuestionThreadRepository questionThreadRepository;
    private final TaskAssignmentForumResponderRepository responderRepository;

    @Transactional
    public QuestionThread claimAsAdministrator(
        Long questionId,
        Long administratorId,
        Long expectedVersion,
        Instant updatedAt) {
        int updatedRows = questionThreadRepository.claimForReview(
            questionId,
            administratorId,
            expectedVersion,
            updatedAt);

        if (updatedRows == 0) {
            classifyClaimFailureAndThrow(questionId, expectedVersion);
            throw new QuestionVersionConflictException(questionId);
        }

        return requireQuestion(questionId);
    }

    @Transactional
    public QuestionThread claimAsResponder(
        Long questionId,
        Long responderUserId,
        Long expectedVersion,
        Instant updatedAt) {
        Long taskAssignmentId = questionThreadRepository.findTaskAssignmentIdByQuestionId(questionId)
            .orElseThrow(() -> new QuestionNotFoundException(questionId));

        responderRepository.findByTaskAssignmentIdAndResponderUserIdForUpdate(taskAssignmentId, responderUserId)
            .orElseThrow(() -> new QuestionNotFoundException(questionId));

        int updatedRows = questionThreadRepository.claimForReviewAsResponder(
            questionId,
            responderUserId,
            taskAssignmentId,
            expectedVersion,
            updatedAt);

        if (updatedRows == 0) {
            classifyResponderClaimFailureAndThrow(questionId, expectedVersion, taskAssignmentId);
            throw new QuestionVersionConflictException(questionId);
        }
        if (updatedRows != 1) {
            throw new IllegalStateException(
                ("Question claim affected an unexpected number of rows: questionId=%s, rows=%s")
                    .formatted(questionId, updatedRows));
        }

        return requireQuestion(questionId);
    }

    private void classifyClaimFailureAndThrow(Long questionId, Long expectedVersion) {
        QuestionThread question = requireQuestion(questionId);
        classifyPersistedStateAndThrow(question, expectedVersion);
    }

    private void classifyResponderClaimFailureAndThrow(
        Long questionId,
        Long expectedVersion,
        Long permittedTaskAssignmentId) {
        QuestionThread question = requireQuestion(questionId);
        if (!Objects.equals(question.getTaskAssignmentId(), permittedTaskAssignmentId)) {
            throw new QuestionNotFoundException(questionId);
        }
        classifyPersistedStateAndThrow(question, expectedVersion);
    }

    private QuestionThread requireQuestion(Long questionId) {
        return questionThreadRepository.findById(questionId)
            .orElseThrow(() -> new QuestionNotFoundException(questionId));
    }

    private void classifyPersistedStateAndThrow(QuestionThread question, Long expectedVersion) {
        if (question.getState() == CLOSED) {
            throw new InvalidQuestionStateException(question.getId(), question.getState(), CLAIM_OPERATION);
        }
        if (question.getStatus() == ANSWERED) {
            throw new InvalidQuestionStateException(question.getId(), question.getStatus(), CLAIM_OPERATION);
        }
        if (question.getAssignedReviewerId() != null || question.getStatus() == IN_REVIEW) {
            throw new QuestionAlreadyClaimedException(question.getId());
        }
        if (!Objects.equals(question.getVersion(), expectedVersion)) {
            throw new QuestionVersionConflictException(question.getId());
        }
        throw new QuestionVersionConflictException(question.getId());
    }
}