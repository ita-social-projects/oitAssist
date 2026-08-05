package com.itasocialacademy.oitassist.chat.service;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType.OFFICIAL_ANSWER;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.ANSWERED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.NEW;
import static com.itasocialacademy.oitassist.core.config.PaginationConfig.MAX_PAGE_SIZE;
import com.itasocialacademy.oitassist.chat.dao.dto.request.CreateOfficialAnswerRequestDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionReviewInboxItemResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionMessage;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionMessageRepository;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import com.itasocialacademy.oitassist.chat.event.OfficialAnswerPublishedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionClaimedDomainEvent;
import com.itasocialacademy.oitassist.chat.exceptions.InvalidQuestionStateException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import com.itasocialacademy.oitassist.chat.mapper.QuestionMessageMapper;
import com.itasocialacademy.oitassist.chat.mapper.QuestionThreadMapper;
import com.itasocialacademy.oitassist.chat.service.interfaces.OrganizationQuestionService;
import com.itasocialacademy.oitassist.chat.service.interfaces.TaskAssignmentForumResponderService;
import com.itasocialacademy.oitassist.chat.utils.OrganizationQuestionClaimCoordinator;
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

    @Override
    @Transactional
    public QuestionMessageResponseDTO publishOfficialAnswer(
        Long questionId,
        CreateOfficialAnswerRequestDTO request) {
        validateQuestionId(
            questionId);

        /*
         * @Transactional opens the transaction before this method body runs. Therefore
         * authentication, ownership and eligibility are all evaluated inside the
         * publication transaction.
         */
        Long responderUserId =
            requireOrganizationMember();

        log.debug(
            "Publishing official answer as ORG responder: "
                + "questionId={}, responderUserId={}",
            questionId,
            responderUserId);

        /*
         * The write lock coordinates this operation with lifecycle mutations. A
         * concurrent close cannot be committed between the OPEN check and
         * official-answer persistence.
         */
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

        /*
         * The request controls only content. Every remaining persisted field is
         * overwritten by the backend even if a mapper implementation returns a polluted
         * entity.
         */
        officialAnswer.setId(null);
        officialAnswer.setQuestionThreadId(
            questionId);
        officialAnswer.setAuthorId(
            responderUserId);
        officialAnswer.setType(
            OFFICIAL_ANSWER);
        officialAnswer.setCreatedAt(null);

        QuestionStatus previousStatus =
            question.getStatus();

        /*
         * NEW and IN_REVIEW become ANSWERED. An already ANSWERED question accepts an
         * additional official answer without changing its status.
         */
        if (question.getStatus() != ANSWERED) {
            question.setStatus(
                ANSWERED);
        }

        QuestionMessage savedAnswer =
            questionMessageRepository.save(
                officialAnswer);

        /*
         * QuestionThread is managed. Flush persists the dirty-checked status transition
         * and updates the optimistic-lock version before the immutable question
         * snapshot is created.
         */
        questionThreadRepository.flush();

        QuestionMessageResponseDTO messageResponse =
            questionMessageMapper.toResponse(
                savedAnswer);

        QuestionThreadResponseDTO questionResponse =
            questionThreadMapper.toResponse(
                question);

        Instant eventTime =
            Instant.now();

        applicationEventPublisher.publishEvent(
            new OfficialAnswerPublishedDomainEvent(
                questionResponse,
                messageResponse,
                previousStatus,
                questionResponse.status(),
                eventTime));

        log.info(
            "Official answer published by ORG responder: "
                + "messageId={}, questionId={}, responderUserId={}",
            savedAnswer.getId(),
            questionId,
            responderUserId);

        return messageResponse;
    }

    private void requireAssignedResponderAccess(
        QuestionThread question,
        Long responderUserId) {
        /*
         * Ownership is checked first. This avoids querying responder eligibility for a
         * question assigned to another reviewer.
         */
        if (!Objects.equals(
            question.getAssignedReviewerId(),
            responderUserId)) {
            throw new QuestionNotFoundException(
                question.getId());
        }

        /*
         * A stale or revoked responder grant removes assigned-review access. Not-found
         * masking prevents disclosure of protected question state.
         */
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

        if (expectedVersion == null
            || expectedVersion < 0) {
            throw new ValidationException(
                "Question version must not be negative",
                ErrorCode.COMMON_VALIDATION_FAILED);
        }
    }
}