package com.itasocialacademy.oitassist.chat.service;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.NEW;
import static com.itasocialacademy.oitassist.core.config.PaginationConfig.MAX_PAGE_SIZE;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionReviewInboxItemResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import com.itasocialacademy.oitassist.chat.event.QuestionClaimedDomainEvent;
import com.itasocialacademy.oitassist.chat.mapper.QuestionThreadMapper;
import com.itasocialacademy.oitassist.chat.service.interfaces.OrganizationQuestionService;
import com.itasocialacademy.oitassist.chat.utils.OrganizationQuestionClaimCoordinator;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationQuestionServiceImpl implements OrganizationQuestionService {
    private static final String ORG_ROLE = "ORG";

    private static final Sort RESPONDER_INBOX_SORT =
        Sort.by(
            Sort.Order.asc("createdAt"),
            Sort.Order.asc("id"));

    private static final Sort ASSIGNED_TO_ME_SORT =
        Sort.by(
            Sort.Order.desc("updatedAt"),
            Sort.Order.desc("id"));

    private final QuestionThreadRepository questionThreadRepository;

    private final QuestionThreadMapper questionThreadMapper;

    private final SecurityFacade securityFacade;

    private final OrganizationQuestionClaimCoordinator organizationQuestionClaimCoordinator;

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

        log.debug(
            "Retrieving ORG responder inbox: "
                + "responderUserId={}, page={}, size={}",
            responderUserId,
            page,
            size);

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

        log.debug(
            "Retrieving questions assigned to ORG responder: "
                + "responderUserId={}, status={}, page={}, size={}",
            responderUserId,
            status,
            page,
            size);

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

        Page<QuestionReviewInboxItemResponseDTO> result =
            assignedQuestions.map(
                questionThreadMapper::toReviewInboxItemResponse);

        log.debug(
            "Questions assigned to ORG responder retrieved: "
                + "responderUserId={}, status={}, page={}, "
                + "returnedElements={}, totalElements={}",
            responderUserId,
            status,
            page,
            result.getNumberOfElements(),
            result.getTotalElements());

        return result;
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

        log.debug(
            "Claiming question as ORG responder: "
                + "questionId={}, responderUserId={}, expectedVersion={}",
            questionId,
            responderUserId,
            expectedVersion);

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

        log.debug(
            "Question claimed by ORG responder: "
                + "questionId={}, responderUserId={}, version={}",
            questionId,
            responderUserId,
            response.version());

        return response;
    }

    private Long requireOrganizationMember() {
        Long currentUserId =
            securityFacade.getCurrentUserId()
                .orElseThrow(() -> new AuthenticationException(
                    "Authentication is required to access "
                        + "organizing committee question queues",
                    ErrorCode.AUTHENTICATION_REQUIRED));

        if (!securityFacade.hasRole(ORG_ROLE)) {
            throw new AuthorizationException(
                "Global ORG role is required to access "
                    + "organizing committee question queues",
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
        if (questionId == null || questionId <= 0) {
            throw new ValidationException(
                "Question id must be a positive number",
                ErrorCode.COMMON_VALIDATION_FAILED);
        }

        if (expectedVersion == null
            || expectedVersion < 0) {
            throw new ValidationException(
                "Question version must not be negative",
                ErrorCode.COMMON_VALIDATION_FAILED);
        }
    }
}