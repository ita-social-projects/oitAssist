package com.itasocialacademy.oitassist.chat.service;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType.OFFICIAL_ANSWER;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.ANSWERED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.NEW;
import static com.itasocialacademy.oitassist.core.config.PaginationConfig.MAX_PAGE_SIZE;
import com.itasocialacademy.oitassist.chat.dao.dto.request.CreateOfficialAnswerRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.AdminQuestionInboxItemResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionMessage;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionMessageRepository;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import com.itasocialacademy.oitassist.chat.exceptions.InvalidQuestionStateException;
import com.itasocialacademy.oitassist.chat.mapper.QuestionMessageMapper;
import com.itasocialacademy.oitassist.chat.mapper.QuestionThreadMapper;
import com.itasocialacademy.oitassist.chat.service.interfaces.AdministratorQuestionService;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionVersionConflictException;
import com.itasocialacademy.oitassist.chat.utils.QuestionClaimFailureClassifier;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdministratorQuestionServiceImpl implements AdministratorQuestionService {
    private static final String ADMIN_ROLE = "ADMIN";

    private static final Sort UNCLAIMED_QUESTION_SORT = Sort.by(
        Sort.Order.asc("createdAt"),
        Sort.Order.asc("id"));

    private static final Sort ASSIGNED_QUESTION_SORT = Sort.by(
        Sort.Order.desc("updatedAt"),
        Sort.Order.desc("id"));

    private final QuestionMessageRepository questionMessageRepository;
    private final QuestionMessageMapper questionMessageMapper;
    private final QuestionThreadRepository questionThreadRepository;
    private final QuestionThreadMapper questionThreadMapper;
    private final SecurityFacade securityFacade;
    private final QuestionClaimFailureClassifier questionClaimFailureClassifier;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminQuestionInboxItemResponseDTO> getUnclaimedQuestions(
        int page,
        int size) {
        validatePageAndSize(page, size);

        Long administratorId = requireAdministrator();

        log.debug(
            "Retrieving unclaimed administrator questions: "
                + "administratorId={}, page={}, size={}",
            administratorId,
            page,
            size);

        Pageable pageable = PageRequest.of(
            page,
            size,
            UNCLAIMED_QUESTION_SORT);

        Page<AdminQuestionInboxItemResponseDTO> result =
            questionThreadRepository
                .findAllByStateAndStatusAndAssignedReviewerIdIsNull(
                    OPEN,
                    NEW,
                    pageable)
                .map(
                    questionThreadMapper::toAdminInboxItemResponse);

        log.debug(
            "Unclaimed administrator questions retrieved: "
                + "administratorId={}, page={}, "
                + "returnedElements={}, totalElements={}",
            administratorId,
            page,
            result.getNumberOfElements(),
            result.getTotalElements());

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminQuestionInboxItemResponseDTO> getAssignedQuestions(
        QuestionStatus status,
        int page,
        int size) {
        validatePageAndSize(page, size);

        Long administratorId = requireAdministrator();

        log.debug(
            "Retrieving questions assigned to administrator: "
                + "administratorId={}, status={}, page={}, size={}",
            administratorId,
            status,
            page,
            size);

        Pageable pageable = PageRequest.of(
            page,
            size,
            ASSIGNED_QUESTION_SORT);

        Page<QuestionThread> assignedQuestions =
            status == null
                ? questionThreadRepository
                    .findAllByStateAndAssignedReviewerId(
                        OPEN,
                        administratorId,
                        pageable)
                : questionThreadRepository
                    .findAllByStateAndAssignedReviewerIdAndStatus(
                        OPEN,
                        administratorId,
                        status,
                        pageable);

        Page<AdminQuestionInboxItemResponseDTO> result =
            assignedQuestions.map(
                questionThreadMapper::toAdminInboxItemResponse);

        log.debug(
            "Assigned administrator questions retrieved: "
                + "administratorId={}, status={}, page={}, "
                + "returnedElements={}, totalElements={}",
            administratorId,
            status,
            page,
            result.getNumberOfElements(),
            result.getTotalElements());

        return result;
    }

    @Override
    @Transactional
    public QuestionThreadResponseDTO claimQuestion(Long questionId, Long expectedVersion) {
        validateClaimInput(questionId, expectedVersion);

        Long administratorId = requireAdministrator();

        log.debug(
            "Claiming question: questionId={}, "
                + "administratorId={}, expectedVersion={}",
            questionId,
            administratorId,
            expectedVersion);

        int updatedRows = questionThreadRepository.claimForReview(
            questionId,
            administratorId,
            expectedVersion,
            Instant.now());

        if (updatedRows == 0) {
            log.debug(
                "Question claim update affected no rows: "
                    + "questionId={}, administratorId={}, "
                    + "expectedVersion={}",
                questionId,
                administratorId,
                expectedVersion);

            questionClaimFailureClassifier.classifyAndThrow(questionId, expectedVersion);

            /*
             * The classifier is required to throw. This fallback prevents accidental
             * success if that contract is broken.
             */
            throw new QuestionVersionConflictException(questionId);
        }

        QuestionThread claimedQuestion = questionThreadRepository.findById(questionId)
            .orElseThrow(() -> new QuestionNotFoundException(questionId));

        log.debug(
            "Question claimed successfully: "
                + "questionId={}, administratorId={}, version={}",
            questionId,
            administratorId,
            claimedQuestion.getVersion());

        return questionThreadMapper.toResponse(claimedQuestion);
    }

    @Override
    @Transactional
    public QuestionMessageResponseDTO publishOfficialAnswer(
        Long questionId,
        CreateOfficialAnswerRequestDTO request) {
        validateQuestionId(questionId);

        Long administratorId = requireAdministrator();

        log.debug(
            "Publishing official answer: "
                + "questionId={}, administratorId={}",
            questionId,
            administratorId);

        /*
         * The write lock prevents a lifecycle operation from closing the question
         * between the OPEN-state validation and message persistence.
         */
        QuestionThread question = questionThreadRepository
            .findByIdForUpdate(questionId)
            .orElseThrow(() -> new QuestionNotFoundException(
                questionId));

        validateQuestionAcceptsOfficialAnswers(question);

        QuestionMessage officialAnswer =
            questionMessageMapper
                .toOfficialAnswerEntity(request);

        /*
         * All fields except content are controlled by the backend. The assignments are
         * intentionally performed even if a mapper implementation returns a polluted
         * entity.
         */
        officialAnswer.setId(null);
        officialAnswer.setQuestionThreadId(questionId);
        officialAnswer.setAuthorId(administratorId);
        officialAnswer.setType(OFFICIAL_ANSWER);
        officialAnswer.setCreatedAt(null);

        /*
         * QuestionThread is a managed entity loaded in the current transaction. Dirty
         * checking persists this transition without an explicit save().
         */
        if (question.getStatus() != ANSWERED) {
            question.setStatus(ANSWERED);
        }

        QuestionMessage savedAnswer =
            questionMessageRepository.save(
                officialAnswer);

        log.info(
            "Official answer published: "
                + "messageId={}, questionId={}, administratorId={}",
            savedAnswer.getId(),
            questionId,
            administratorId);

        return questionMessageMapper.toResponse(
            savedAnswer);
    }

    private void validateQuestionId(
        Long questionId) {
        if (questionId == null || questionId <= 0) {
            throw new ValidationException(
                "Question id must be a positive number",
                ErrorCode.COMMON_VALIDATION_FAILED);
        }
    }

    private Long requireAdministrator() {
        Long currentUserId = securityFacade
            .getCurrentUserId()
            .orElseThrow(() -> new AuthenticationException(
                "Authentication is required to access "
                    + "the administrator question inbox",
                ErrorCode.AUTHENTICATION_REQUIRED));

        if (!securityFacade.hasRole(ADMIN_ROLE)) {
            throw new AuthorizationException(
                "Global administrator role is required "
                    + "to access the question inbox",
                ErrorCode.ACCESS_DENIED);
        }

        return currentUserId;
    }

    private void validatePageAndSize(
        int page,
        int size) {
        if (page < 0) {
            throw new ValidationException(
                "Page number must not be negative",
                ErrorCode.COMMON_VALIDATION_FAILED);
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ValidationException(
                "Page size must be between 1 and %d"
                    .formatted(MAX_PAGE_SIZE),
                ErrorCode.COMMON_VALIDATION_FAILED);
        }
    }

    private void validateClaimInput(
        Long questionId,
        Long expectedVersion) {
        validateQuestionId(questionId);

        if (expectedVersion == null || expectedVersion < 0) {
            throw new ValidationException(
                "Question version must not be negative",
                ErrorCode.COMMON_VALIDATION_FAILED);
        }
    }

    private void validateQuestionAcceptsOfficialAnswers(
        QuestionThread question) {
        if (question.getState() != OPEN) {
            throw new InvalidQuestionStateException(
                question.getId(),
                question.getState(),
                "publish official answer");
        }
    }
}