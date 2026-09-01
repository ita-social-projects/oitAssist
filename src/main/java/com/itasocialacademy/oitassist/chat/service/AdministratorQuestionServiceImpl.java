package com.itasocialacademy.oitassist.chat.service;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType.OFFICIAL_ANSWER;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.ANSWERED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.NEW;
import static com.itasocialacademy.oitassist.core.config.PaginationConfig.MAX_PAGE_SIZE;
import com.itasocialacademy.oitassist.chat.dao.dto.request.CreateOfficialAnswerRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.request.UpdateQuestionStateRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.request.UpdateQuestionStatusRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.request.UpdateQuestionVisibilityRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionReviewInboxItemResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionState;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionMessage;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionMessageRepository;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import com.itasocialacademy.oitassist.chat.event.domain.OfficialAnswerPublishedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.domain.QuestionClaimedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.domain.QuestionStateChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.domain.QuestionStatusChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.domain.QuestionVisibilityChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.exceptions.InvalidQuestionStateException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionVersionConflictException;
import com.itasocialacademy.oitassist.chat.mapper.QuestionMessageMapper;
import com.itasocialacademy.oitassist.chat.mapper.QuestionThreadMapper;
import com.itasocialacademy.oitassist.chat.service.interfaces.AdministratorQuestionService;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final QuestionClaimService questionClaimService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionReviewInboxItemResponseDTO> getUnclaimedQuestions(int page, int size) {
        validatePageAndSize(page, size);

        Long administratorId = requireAdministrator();

        log.debug(
            "Retrieving unclaimed administrator questions: "
                + "administratorId={}, page={}, size={}",
            administratorId,
            page,
            size);

        Pageable pageable = PageRequest.of(page, size, UNCLAIMED_QUESTION_SORT);

        Page<QuestionReviewInboxItemResponseDTO> result = questionThreadRepository
            .findAllByStateAndStatusAndAssignedReviewerIdIsNull(OPEN, NEW, pageable)
            .map(questionThreadMapper::toReviewInboxItemResponse);

        log.debug(
            "Unclaimed administrator questions retrieved: "
                + "administratorId={}, page={}, returnedElements={}, "
                + "totalElements={}",
            administratorId,
            page,
            result.getNumberOfElements(),
            result.getTotalElements());

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionReviewInboxItemResponseDTO> getAssignedQuestions(QuestionStatus status, int page, int size) {
        validatePageAndSize(page, size);
        Long administratorId = requireAdministrator();

        log.debug(
            "Retrieving questions assigned to administrator: "
                + "administratorId={}, status={}, page={}, size={}",
            administratorId,
            status,
            page,
            size);

        Pageable pageable = PageRequest.of(page, size, ASSIGNED_QUESTION_SORT);

        Page<QuestionThread> assignedQuestions = status == null
            ? questionThreadRepository.findAllByStateAndAssignedReviewerId(OPEN, administratorId, pageable)
            : questionThreadRepository.findAllByStateAndAssignedReviewerIdAndStatus(
                OPEN, administratorId, status, pageable);

        Page<QuestionReviewInboxItemResponseDTO> result =
            assignedQuestions.map(questionThreadMapper::toReviewInboxItemResponse);

        log.debug(
            "Assigned administrator questions retrieved: "
                + "administratorId={}, status={}, page={}, returnedElements={}, "
                + "totalElements={}",
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
            "Claiming question: questionId={}, administratorId={}, "
                + "expectedVersion={}",
            questionId,
            administratorId,
            expectedVersion);

        QuestionThread claimedQuestion = questionClaimService.claimAsAdministrator(
            questionId, administratorId, expectedVersion, Instant.now());

        log.debug(
            "Question claimed successfully: "
                + "questionId={}, administratorId={}, version={}",
            questionId,
            administratorId,
            claimedQuestion.getVersion());

        QuestionThreadResponseDTO response = questionThreadMapper.toResponse(claimedQuestion);

        applicationEventPublisher.publishEvent(
            new QuestionClaimedDomainEvent(response, null, administratorId, Instant.now()));

        return response;
    }

    @Override
    @Transactional
    public QuestionMessageResponseDTO publishOfficialAnswer(Long questionId, CreateOfficialAnswerRequestDTO request) {
        validateQuestionId(questionId);

        Long administratorId = requireAdministrator();

        log.debug(
            "Publishing official answer: "
                + "questionId={}, administratorId={}",
            questionId,
            administratorId);

        QuestionThread question = questionThreadRepository.findByIdForUpdate(questionId)
            .orElseThrow(() -> new QuestionNotFoundException(questionId));

        validateQuestionAcceptsOfficialAnswers(question);

        QuestionMessage officialAnswer = questionMessageMapper.toOfficialAnswerEntity(request);

        officialAnswer.setId(null);
        officialAnswer.setQuestionThreadId(questionId);
        officialAnswer.setAuthorId(administratorId);
        officialAnswer.setType(OFFICIAL_ANSWER);
        officialAnswer.setCreatedAt(null);

        QuestionStatus previousStatus = question.getStatus();

        if (question.getStatus() != ANSWERED) {
            question.setStatus(ANSWERED);
        }

        QuestionMessage savedAnswer = questionMessageRepository.save(officialAnswer);

        questionThreadRepository.flush();

        QuestionMessageResponseDTO messageResponse = questionMessageMapper.toResponse(savedAnswer);
        QuestionThreadResponseDTO questionResponse = questionThreadMapper.toResponse(question);

        applicationEventPublisher.publishEvent(
            new OfficialAnswerPublishedDomainEvent(
                questionResponse,
                messageResponse,
                previousStatus,
                questionResponse.status(),
                Instant.now()));

        log.info(
            "Official answer published: "
                + "messageId={}, questionId={}, administratorId={}",
            savedAnswer.getId(),
            questionId,
            administratorId);

        return messageResponse;
    }

    @Override
    @Transactional
    public QuestionThreadResponseDTO updateVisibility(Long questionId, UpdateQuestionVisibilityRequestDTO request) {
        validateModerationRequest(questionId, request);
        validateModerationValue(request.visibility(), "Question visibility");
        validateExpectedVersion(request.version());

        Long administratorId = requireAdministrator();
        QuestionThread currentQuestion = loadQuestion(questionId);
        QuestionVisibility previousVisibility = currentQuestion.getVisibility();

        log.debug(
            "Updating question visibility: "
                + "questionId={}, administratorId={}, visibility={}, "
                + "expectedVersion={}",
            questionId,
            administratorId,
            request.visibility(),
            request.version());

        int updatedRows = questionThreadRepository.updateVisibilityIfVersionMatches(
            questionId, request.visibility(), request.version(), Instant.now());

        QuestionThreadResponseDTO response = completeModerationUpdate(
            questionId,
            administratorId,
            request.version(),
            "visibility",
            request.visibility(),
            updatedRows);

        applicationEventPublisher.publishEvent(
            new QuestionVisibilityChangedDomainEvent(
                response,
                previousVisibility,
                response.visibility(),
                Instant.now()));

        return response;
    }

    @Override
    @Transactional
    public QuestionThreadResponseDTO updateStatus(Long questionId, UpdateQuestionStatusRequestDTO request) {
        validateModerationRequest(questionId, request);
        validateModerationValue(request.status(), "Question status");
        validateExpectedVersion(request.version());

        Long administratorId = requireAdministrator();
        QuestionThread currentQuestion = loadQuestion(questionId);
        QuestionStatus previousStatus = currentQuestion.getStatus();

        log.debug(
            "Updating question status: "
                + "questionId={}, administratorId={}, status={}, "
                + "expectedVersion={}",
            questionId,
            administratorId,
            request.status(),
            request.version());

        int updatedRows = questionThreadRepository.updateStatusIfVersionMatches(
            questionId, request.status(), request.version(), Instant.now());

        QuestionThreadResponseDTO response = completeModerationUpdate(
            questionId,
            administratorId,
            request.version(),
            "status",
            request.status(),
            updatedRows);

        applicationEventPublisher.publishEvent(
            new QuestionStatusChangedDomainEvent(
                response,
                previousStatus,
                response.status(),
                Instant.now()));

        return response;
    }

    @Override
    @Transactional
    public QuestionThreadResponseDTO updateState(Long questionId, UpdateQuestionStateRequestDTO request) {
        validateModerationRequest(questionId, request);
        validateModerationValue(request.state(), "Question state");
        validateExpectedVersion(request.version());

        Long administratorId = requireAdministrator();
        QuestionThread currentQuestion = loadQuestion(questionId);
        QuestionState previousState = currentQuestion.getState();

        log.debug(
            "Updating question lifecycle state: "
                + "questionId={}, administratorId={}, state={}, "
                + "expectedVersion={}",
            questionId,
            administratorId,
            request.state(),
            request.version());

        int updatedRows = questionThreadRepository.updateStateIfVersionMatches(
            questionId, request.state(), request.version(), Instant.now());

        QuestionThreadResponseDTO response = completeModerationUpdate(
            questionId,
            administratorId,
            request.version(),
            "state",
            request.state(),
            updatedRows);

        applicationEventPublisher.publishEvent(
            new QuestionStateChangedDomainEvent(
                response,
                previousState,
                response.state(),
                Instant.now()));

        return response;
    }

    private QuestionThread loadQuestion(Long questionId) {
        return questionThreadRepository.findById(questionId)
            .orElseThrow(() -> new QuestionNotFoundException(questionId));
    }

    private QuestionThreadResponseDTO completeModerationUpdate(
        Long questionId,
        Long administratorId,
        Long expectedVersion,
        String operation,
        Object requestedValue,
        int updatedRows) {
        if (updatedRows == 0) {
            QuestionThread currentQuestion = questionThreadRepository.findById(questionId)
                .orElseThrow(() -> {
                    log.warn(
                        "Question moderation failed because question does not exist: "
                            + "questionId={}, administratorId={}, operation={}",
                        questionId,
                        administratorId,
                        operation);

                    return new QuestionNotFoundException(questionId);
                });

            log.warn(
                "Question moderation version conflict: "
                    + "questionId={}, administratorId={}, operation={}, "
                    + "requestedValue={}, expectedVersion={}, currentVersion={}",
                questionId,
                administratorId,
                operation,
                requestedValue,
                expectedVersion,
                currentQuestion.getVersion());

            throw new QuestionVersionConflictException(questionId);
        }

        QuestionThread updatedQuestion = questionThreadRepository.findById(questionId)
            .orElseThrow(() -> new QuestionNotFoundException(questionId));

        log.info(
            "Question moderation completed: "
                + "questionId={}, administratorId={}, operation={}, "
                + "requestedValue={}, version={}",
            questionId,
            administratorId,
            operation,
            requestedValue,
            updatedQuestion.getVersion());

        return questionThreadMapper.toResponse(updatedQuestion);
    }

    private void validateQuestionId(Long questionId) {
        if (questionId == null || questionId <= 0) {
            throw new ValidationException(
                "Question id must be a positive number",
                ErrorCode.COMMON_VALIDATION_FAILED);
        }
    }

    private Long requireAdministrator() {
        Long currentUserId = securityFacade.getCurrentUserId()
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

    private void validatePageAndSize(int page, int size) {
        if (page < 0) {
            throw new ValidationException(
                "Page number must not be negative",
                ErrorCode.COMMON_VALIDATION_FAILED);
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ValidationException(
                "Page size must be between 1 and %d".formatted(MAX_PAGE_SIZE),
                ErrorCode.COMMON_VALIDATION_FAILED);
        }
    }

    private void validateClaimInput(Long questionId, Long expectedVersion) {
        validateQuestionId(questionId);
        validateExpectedVersion(expectedVersion);
    }

    private void validateQuestionAcceptsOfficialAnswers(QuestionThread question) {
        if (question.getState() != OPEN) {
            throw new InvalidQuestionStateException(
                question.getId(),
                question.getState(),
                "publish official answer");
        }
    }

    private void validateModerationRequest(Long questionId, Object request) {
        validateQuestionId(questionId);

        if (request == null) {
            throw new ValidationException(
                "Question moderation request must not be null",
                ErrorCode.COMMON_VALIDATION_FAILED);
        }
    }

    private void validateModerationValue(Object value, String valueName) {
        if (value == null) {
            throw new ValidationException(
                "%s must not be null".formatted(valueName),
                ErrorCode.COMMON_VALIDATION_FAILED);
        }
    }

    private void validateExpectedVersion(Long expectedVersion) {
        if (expectedVersion == null) {
            throw new ValidationException(
                "Question version must not be null",
                ErrorCode.COMMON_VALIDATION_FAILED);
        }

        if (expectedVersion < 0) {
            throw new ValidationException(
                "Question version must not be negative",
                ErrorCode.COMMON_VALIDATION_FAILED);
        }
    }
}