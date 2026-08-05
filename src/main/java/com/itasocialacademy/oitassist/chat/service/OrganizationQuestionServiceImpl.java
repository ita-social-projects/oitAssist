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
import com.itasocialacademy.oitassist.chat.event.OfficialAnswerPublishedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionClaimedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionStateChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionStatusChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionVisibilityChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.exceptions.InvalidQuestionStateException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import com.itasocialacademy.oitassist.chat.mapper.QuestionMessageMapper;
import com.itasocialacademy.oitassist.chat.mapper.QuestionThreadMapper;
import com.itasocialacademy.oitassist.chat.service.interfaces.OrganizationQuestionService;
import com.itasocialacademy.oitassist.chat.service.interfaces.TaskAssignmentForumResponderService;
import com.itasocialacademy.oitassist.chat.utils.OrganizationQuestionClaimCoordinator;
import com.itasocialacademy.oitassist.chat.utils.OrganizationQuestionModerationCoordinator;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import java.time.Instant;
import java.util.Objects;
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
public class OrganizationQuestionServiceImpl
    implements OrganizationQuestionService {
    private static final String ORG_ROLE =
        "ORG";

    private static final Sort RESPONDER_INBOX_SORT =
        Sort.by(
            Sort.Order.asc("createdAt"),
            Sort.Order.asc("id"));

    private static final Sort ASSIGNED_TO_ME_SORT =
        Sort.by(
            Sort.Order.desc("updatedAt"),
            Sort.Order.desc("id"));

    private final QuestionThreadRepository questionThreadRepository;

    private final QuestionMessageRepository questionMessageRepository;

    private final QuestionThreadMapper questionThreadMapper;

    private final QuestionMessageMapper questionMessageMapper;

    private final SecurityFacade securityFacade;

    private final TaskAssignmentForumResponderService taskAssignmentForumResponderService;

    private final OrganizationQuestionClaimCoordinator organizationQuestionClaimCoordinator;

    private final OrganizationQuestionModerationCoordinator organizationQuestionModerationCoordinator;

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionReviewInboxItemResponseDTO> getResponderInbox(
        int page,
        int size) {
        validatePageAndSize(
            page,
            size);

        Long responderUserId =
            requireOrganizationMember();

        Pageable pageable =
            PageRequest.of(
                page,
                size,
                RESPONDER_INBOX_SORT);

        Page<QuestionReviewInboxItemResponseDTO> result =
            questionThreadRepository
                .findResponderUnclaimedQuestions(
                    responderUserId,
                    OPEN,
                    NEW,
                    pageable)
                .map(
                    questionThreadMapper::toReviewInboxItemResponse);

        log.debug(
            "ORG responder inbox retrieved: "
                + "responderUserId={}, page={}, "
                + "returnedElements={}, totalElements={}",
            responderUserId,
            page,
            result.getNumberOfElements(),
            result.getTotalElements());

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionReviewInboxItemResponseDTO> getAssignedToCurrentResponder(
        QuestionStatus status,
        int page,
        int size) {
        validatePageAndSize(
            page,
            size);

        Long responderUserId =
            requireOrganizationMember();

        Pageable pageable =
            PageRequest.of(
                page,
                size,
                ASSIGNED_TO_ME_SORT);

        Page<QuestionThread> assignedQuestions =
            status == null
                ? questionThreadRepository
                    .findAllByStateAndAssignedReviewerId(
                        OPEN,
                        responderUserId,
                        pageable)
                : questionThreadRepository
                    .findAllByStateAndAssignedReviewerIdAndStatus(
                        OPEN,
                        responderUserId,
                        status,
                        pageable);

        return assignedQuestions.map(
            questionThreadMapper::toReviewInboxItemResponse);
    }

    @Override
    @Transactional
    public QuestionThreadResponseDTO claimQuestion(
        Long questionId,
        Long expectedVersion) {
        validateClaimInput(
            questionId,
            expectedVersion);

        Long responderUserId =
            requireOrganizationMember();

        Instant claimTime =
            Instant.now();

        QuestionThread claimedQuestion =
            organizationQuestionClaimCoordinator
                .claimQuestion(
                    questionId,
                    responderUserId,
                    expectedVersion,
                    claimTime);

        QuestionThreadResponseDTO response =
            questionThreadMapper.toResponse(
                claimedQuestion);

        applicationEventPublisher.publishEvent(
            new QuestionClaimedDomainEvent(
                response,
                null,
                responderUserId,
                claimTime));

        return response;
    }

    @Override
    @Transactional
    public QuestionMessageResponseDTO publishOfficialAnswer(
        Long questionId,
        CreateOfficialAnswerRequestDTO request) {
        validateQuestionId(
            questionId);

        Long responderUserId =
            requireOrganizationMember();

        QuestionThread question =
            questionThreadRepository
                .findByIdForUpdate(
                    questionId)
                .orElseThrow(() -> new QuestionNotFoundException(
                    questionId));

        requireAssignedResponderAccess(
            question,
            responderUserId);

        validateQuestionAcceptsOfficialAnswers(
            question);

        QuestionMessage officialAnswer =
            questionMessageMapper
                .toOfficialAnswerEntity(
                    request);

        officialAnswer.setId(null);
        officialAnswer.setQuestionThreadId(
            questionId);
        officialAnswer.setAuthorId(
            responderUserId);
        officialAnswer.setType(
            OFFICIAL_ANSWER);
        officialAnswer.setCreatedAt(null);

        QuestionStatus previousStatus = question.getStatus();

        if (question.getStatus() != ANSWERED) {
            question.setStatus(
                ANSWERED);
        }

        QuestionMessage savedAnswer =
            questionMessageRepository.save(
                officialAnswer);

        questionThreadRepository.flush();

        QuestionMessageResponseDTO messageResponse =
            questionMessageMapper.toResponse(
                savedAnswer);

        QuestionThreadResponseDTO questionResponse =
            questionThreadMapper.toResponse(
                question);

        applicationEventPublisher.publishEvent(
            new OfficialAnswerPublishedDomainEvent(
                questionResponse,
                messageResponse,
                previousStatus,
                questionResponse.status(),
                Instant.now()));

        return messageResponse;
    }

    @Override
    @Transactional
    public QuestionThreadResponseDTO updateVisibility(
        Long questionId,
        UpdateQuestionVisibilityRequestDTO request) {
        validateModerationRequest(
            questionId,
            request);

        validateModerationValue(
            request.visibility(),
            "Question visibility");

        validateExpectedVersion(
            request.version());

        Long responderUserId =
            requireOrganizationMember();

        QuestionThread currentQuestion =
            loadQuestion(
                questionId);

        requireAssignedReviewerOwnership(
            currentQuestion,
            responderUserId);

        QuestionVisibility previousVisibility =
            currentQuestion.getVisibility();

        Instant mutationTime =
            Instant.now();

        QuestionThread updatedQuestion =
            organizationQuestionModerationCoordinator
                .updateVisibility(
                    currentQuestion,
                    responderUserId,
                    request.visibility(),
                    request.version(),
                    mutationTime);

        QuestionThreadResponseDTO response =
            questionThreadMapper.toResponse(
                updatedQuestion);

        applicationEventPublisher.publishEvent(
            new QuestionVisibilityChangedDomainEvent(
                response,
                previousVisibility,
                response.visibility(),
                mutationTime));

        return response;
    }

    @Override
    @Transactional
    public QuestionThreadResponseDTO updateStatus(
        Long questionId,
        UpdateQuestionStatusRequestDTO request) {
        validateModerationRequest(
            questionId,
            request);

        validateModerationValue(
            request.status(),
            "Question status");

        validateExpectedVersion(
            request.version());

        Long responderUserId =
            requireOrganizationMember();

        QuestionThread currentQuestion =
            loadQuestion(
                questionId);

        requireAssignedReviewerOwnership(
            currentQuestion,
            responderUserId);

        QuestionStatus previousStatus =
            currentQuestion.getStatus();

        Instant mutationTime =
            Instant.now();

        QuestionThread updatedQuestion =
            organizationQuestionModerationCoordinator
                .updateStatus(
                    currentQuestion,
                    responderUserId,
                    request.status(),
                    request.version(),
                    mutationTime);

        QuestionThreadResponseDTO response =
            questionThreadMapper.toResponse(
                updatedQuestion);

        applicationEventPublisher.publishEvent(
            new QuestionStatusChangedDomainEvent(
                response,
                previousStatus,
                response.status(),
                mutationTime));

        return response;
    }

    @Override
    @Transactional
    public QuestionThreadResponseDTO updateState(
        Long questionId,
        UpdateQuestionStateRequestDTO request) {
        validateModerationRequest(
            questionId,
            request);

        validateModerationValue(
            request.state(),
            "Question state");

        validateExpectedVersion(
            request.version());

        Long responderUserId =
            requireOrganizationMember();

        QuestionThread currentQuestion =
            loadQuestion(
                questionId);

        requireAssignedReviewerOwnership(
            currentQuestion,
            responderUserId);

        QuestionState previousState =
            currentQuestion.getState();

        Instant mutationTime =
            Instant.now();

        QuestionThread updatedQuestion =
            organizationQuestionModerationCoordinator
                .updateState(
                    currentQuestion,
                    responderUserId,
                    request.state(),
                    request.version(),
                    mutationTime);

        QuestionThreadResponseDTO response =
            questionThreadMapper.toResponse(
                updatedQuestion);

        applicationEventPublisher.publishEvent(
            new QuestionStateChangedDomainEvent(
                response,
                previousState,
                response.state(),
                mutationTime));

        return response;
    }

    private QuestionThread loadQuestion(
        Long questionId) {
        return questionThreadRepository
            .findById(
                questionId)
            .orElseThrow(() -> new QuestionNotFoundException(
                questionId));
    }

    private void requireAssignedReviewerOwnership(
        QuestionThread question,
        Long responderUserId) {
        if (!Objects.equals(
            responderUserId,
            question.getAssignedReviewerId())) {
            throw new QuestionNotFoundException(
                question.getId());
        }
    }

    private void requireAssignedResponderAccess(
        QuestionThread question,
        Long responderUserId) {
        requireAssignedReviewerOwnership(
            question,
            responderUserId);

        if (!taskAssignmentForumResponderService
            .isResponder(
                question.getTaskAssignmentId(),
                responderUserId)) {
            throw new QuestionNotFoundException(
                question.getId());
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

    private Long requireOrganizationMember() {
        Long currentUserId =
            securityFacade.getCurrentUserId()
                .orElseThrow(() -> new AuthenticationException(
                    "Authentication is required to access "
                        + "organizing committee question queues",
                    ErrorCode.AUTHENTICATION_REQUIRED));

        if (!securityFacade.hasRole(
            ORG_ROLE)) {
            throw new AuthorizationException(
                "Global ORG role is required to access "
                    + "organizing committee question queues",
                ErrorCode.ACCESS_DENIED);
        }

        return currentUserId;
    }

    private void validateModerationRequest(
        Long questionId,
        Object request) {
        validateQuestionId(
            questionId);

        if (request == null) {
            throw new ValidationException(
                "Question moderation request must not be null",
                ErrorCode.COMMON_VALIDATION_FAILED);
        }
    }

    private void validateModerationValue(
        Object value,
        String fieldName) {
        if (value == null) {
            throw new ValidationException(
                fieldName + " must not be null",
                ErrorCode.COMMON_VALIDATION_FAILED);
        }
    }

    private void validateExpectedVersion(
        Long version) {
        if (version == null
            || version < 0) {
            throw new ValidationException(
                "Question version must not be negative",
                ErrorCode.COMMON_VALIDATION_FAILED);
        }
    }

    private void validateQuestionId(
        Long questionId) {
        if (questionId == null
            || questionId <= 0) {
            throw new ValidationException(
                "Question id must be a positive number",
                ErrorCode.COMMON_VALIDATION_FAILED);
        }
    }

    private void validatePageAndSize(
        int page,
        int size) {
        if (page < 0) {
            throw new ValidationException(
                "Page number must not be negative",
                ErrorCode.COMMON_VALIDATION_FAILED);
        }

        if (size < 1
            || size > MAX_PAGE_SIZE) {
            throw new ValidationException(
                "Page size must be between 1 and %d"
                    .formatted(
                        MAX_PAGE_SIZE),
                ErrorCode.COMMON_VALIDATION_FAILED);
        }
    }

    private void validateClaimInput(
        Long questionId,
        Long expectedVersion) {
        validateQuestionId(
            questionId);

        validateExpectedVersion(
            expectedVersion);
    }
}