package com.itasocialacademy.oitassist.chat.service;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.core.config.PaginationConfig.MAX_PAGE_SIZE;
import com.itasocialacademy.oitassist.chat.dao.dto.response.TaskAssignmentForumResponderResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.TaskAssignmentForumResponderGrantResult;
import com.itasocialacademy.oitassist.chat.dao.model.TaskAssignmentForumResponder;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import com.itasocialacademy.oitassist.chat.dao.repository.TaskAssignmentForumResponderRepository;
import com.itasocialacademy.oitassist.chat.exceptions.ForumResponderAccessRestrictedException;
import com.itasocialacademy.oitassist.chat.exceptions.ForumResponderActiveReviewException;
import com.itasocialacademy.oitassist.chat.exceptions.InvalidForumResponderCandidateException;
import com.itasocialacademy.oitassist.chat.mapper.TaskAssignmentForumResponderMapper;
import com.itasocialacademy.oitassist.chat.service.interfaces.TaskAssignmentForumResponderService;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.NotFoundException;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.taskassignment.api.TaskAssignmentFacade;
import com.itasocialacademy.oitassist.taskassignment.exceptions.TaskAssignmentNotFoundException;
import com.itasocialacademy.oitassist.user.api.dto.ForumResponderCandidate;
import com.itasocialacademy.oitassist.user.api.interfaces.UserFacade;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.dao.enums.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskAssignmentForumResponderServiceImpl implements TaskAssignmentForumResponderService {
    private static final String ADMIN_ROLE = "ADMIN";

    private static final Sort RESPONDER_SORT =
        Sort.by(
            Sort.Order.desc("assignedAt"),
            Sort.Order.desc("id"));

    private final TaskAssignmentForumResponderRepository responderRepository;
    private final QuestionThreadRepository questionThreadRepository;
    private final TaskAssignmentForumResponderMapper responderMapper;
    private final TaskAssignmentFacade taskAssignmentFacade;
    private final UserFacade userFacade;
    private final SecurityFacade securityFacade;

    @Override
    @Transactional
    public TaskAssignmentForumResponderGrantResult grantResponder(
        Long taskAssignmentId,
        Long responderUserId) {
        validateIdentifiers(
            taskAssignmentId,
            responderUserId);

        Long administratorId =
            requireAdministrator();

        requireTaskAssignmentExists(
            taskAssignmentId);

        ForumResponderCandidate candidate =
            requireResponderCandidate(
                responderUserId);

        validateResponderCandidate(
            candidate);

        int insertedRows =
            responderRepository.insertIfAbsent(
                taskAssignmentId,
                responderUserId,
                administratorId,
                Instant.now());

        if (insertedRows < 0 || insertedRows > 1) {
            throw new IllegalStateException(
                ("Unexpected responder assignment insert result: "
                    + "taskAssignmentId=%s, responderUserId=%s, "
                    + "insertedRows=%s").formatted(
                        taskAssignmentId,
                        responderUserId,
                        insertedRows));
        }

        TaskAssignmentForumResponder assignment =
            responderRepository
                .findByTaskAssignmentIdAndResponderUserId(
                    taskAssignmentId,
                    responderUserId)
                .orElseThrow(() -> new IllegalStateException(
                    ("Forum responder assignment was not found "
                        + "after idempotent grant: "
                        + "taskAssignmentId=%s, "
                        + "responderUserId=%s").formatted(
                            taskAssignmentId,
                            responderUserId)));

        TaskAssignmentForumResponderResponseDTO responder =
            responderMapper.toResponse(
                assignment,
                candidate);

        boolean created = insertedRows == 1;

        log.info(
            "Forum responder grant completed: "
                + "taskAssignmentId={}, responderUserId={}, "
                + "administratorId={}, created={}",
            taskAssignmentId,
            responderUserId,
            administratorId,
            created);

        return new TaskAssignmentForumResponderGrantResult(
            created,
            responder);
    }

    @Override
    @Transactional
    public void revokeResponder(
        Long taskAssignmentId,
        Long responderUserId) {
        validateIdentifiers(
            taskAssignmentId,
            responderUserId);

        Long administratorId =
            requireAdministrator();

        requireTaskAssignmentExists(
            taskAssignmentId);

        /*
         * Validate only that the user still exists.
         *
         * Do not require ORG + ACTIVE here. An administrator must be able to revoke an
         * assignment after the user's role or account status has changed.
         */
        requireResponderCandidate(
            responderUserId);

        Optional<TaskAssignmentForumResponder> assignment =
            responderRepository
                .findByTaskAssignmentIdAndResponderUserIdForUpdate(
                    taskAssignmentId,
                    responderUserId);

        if (assignment.isEmpty()) {
            log.debug(
                "Forum responder revoke completed as no-op: "
                    + "taskAssignmentId={}, responderUserId={}, "
                    + "administratorId={}",
                taskAssignmentId,
                responderUserId,
                administratorId);

            return;
        }

        boolean ownsActiveReview =
            questionThreadRepository
                .existsByTaskAssignmentIdAndAssignedReviewerIdAndState(
                    taskAssignmentId,
                    responderUserId,
                    OPEN);

        if (ownsActiveReview) {
            log.warn(
                "Forum responder revoke rejected: "
                    + "taskAssignmentId={}, responderUserId={}, "
                    + "administratorId={}, reason=active-review",
                taskAssignmentId,
                responderUserId,
                administratorId);

            throw new ForumResponderActiveReviewException(
                taskAssignmentId,
                responderUserId);
        }

        responderRepository.delete(
            assignment.orElseThrow());

        log.info(
            "Forum responder revoked: "
                + "taskAssignmentId={}, responderUserId={}, "
                + "administratorId={}",
            taskAssignmentId,
            responderUserId,
            administratorId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isResponder(
        Long taskAssignmentId,
        Long userId) {
        validateIdentifiers(
            taskAssignmentId,
            userId);

        return responderRepository
            .existsByTaskAssignmentIdAndResponderUserId(
                taskAssignmentId,
                userId);
    }

    @Override
    @Transactional(readOnly = true)
    public void requireResponder(
        Long taskAssignmentId,
        Long userId) {
        validateIdentifiers(
            taskAssignmentId,
            userId);

        boolean responderExists =
            responderRepository
                .existsByTaskAssignmentIdAndResponderUserId(
                    taskAssignmentId,
                    userId);

        if (!responderExists) {
            throw new ForumResponderAccessRestrictedException(
                taskAssignmentId,
                userId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findTaskAssignmentIdsByResponder(
        Long responderUserId) {
        validateIdentifier(
            responderUserId,
            "Responder user id");

        return responderRepository
            .findTaskAssignmentIdsByResponderUserId(
                responderUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskAssignmentForumResponderResponseDTO> getResponders(
        Long taskAssignmentId,
        int page,
        int size) {
        validateIdentifier(
            taskAssignmentId,
            "Task assignment id");

        validatePageAndSize(page, size);

        Long administratorId =
            requireAdministrator();

        requireTaskAssignmentExists(
            taskAssignmentId);

        Pageable pageable =
            PageRequest.of(
                page,
                size,
                RESPONDER_SORT);

        Page<TaskAssignmentForumResponder> assignments =
            responderRepository.findAllByTaskAssignmentId(
                taskAssignmentId,
                pageable);

        if (assignments.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> responderIds =
            assignments.getContent()
                .stream()
                .map(
                    TaskAssignmentForumResponder::getResponderUserId)
                .distinct()
                .toList();

        Map<Long, ForumResponderCandidate> candidatesById =
            userFacade
                .findForumResponderCandidatesByIds(
                    responderIds)
                .stream()
                .collect(
                    Collectors.toMap(
                        ForumResponderCandidate::id,
                        Function.identity()));

        List<TaskAssignmentForumResponderResponseDTO> content =
            assignments.getContent()
                .stream()
                .map(assignment -> {
                    ForumResponderCandidate candidate =
                        candidatesById.get(
                            assignment.getResponderUserId());

                    if (candidate == null) {
                        throw new IllegalStateException(
                            ("User summary is missing for persisted "
                                + "forum responder %s").formatted(
                                    assignment.getResponderUserId()));
                    }

                    return responderMapper.toResponse(
                        assignment,
                        candidate);
                })
                .toList();

        log.debug(
            "Forum responders retrieved: "
                + "taskAssignmentId={}, administratorId={}, "
                + "page={}, returnedElements={}, totalElements={}",
            taskAssignmentId,
            administratorId,
            page,
            content.size(),
            assignments.getTotalElements());

        return new PageImpl<>(
            content,
            assignments.getPageable(),
            assignments.getTotalElements());
    }

    private Long requireAdministrator() {
        Long administratorId =
            securityFacade.getCurrentUserId()
                .orElseThrow(() -> new AuthenticationException(
                    "Authentication is required to manage "
                        + "TaskAssignment forum responders",
                    ErrorCode.AUTHENTICATION_REQUIRED));

        if (!securityFacade.hasRole(ADMIN_ROLE)) {
            throw new AuthorizationException(
                "Global administrator role is required "
                    + "to manage TaskAssignment forum responders",
                ErrorCode.ACCESS_DENIED);
        }

        return administratorId;
    }

    private void requireTaskAssignmentExists(
        Long taskAssignmentId) {
        taskAssignmentFacade
            .findAssignmentById(taskAssignmentId)
            .orElseThrow(() -> new TaskAssignmentNotFoundException(
                taskAssignmentId));
    }

    private ForumResponderCandidate requireResponderCandidate(
        Long responderUserId) {
        return userFacade
            .findForumResponderCandidateById(
                responderUserId)
            .orElseThrow(() -> new NotFoundException(
                "User with id %s was not found"
                    .formatted(responderUserId),
                ErrorCode.USER_NOT_FOUND));
    }

    private void validateResponderCandidate(
        ForumResponderCandidate candidate) {
        if (candidate.role() != Role.ORG
            || candidate.status() != UserStatus.ACTIVE) {
            throw new InvalidForumResponderCandidateException(
                candidate.id(),
                candidate.role(),
                candidate.status());
        }
    }

    private void validateIdentifiers(
        Long taskAssignmentId,
        Long responderUserId) {
        validateIdentifier(
            taskAssignmentId,
            "Task assignment id");

        validateIdentifier(
            responderUserId,
            "Responder user id");
    }

    private void validateIdentifier(
        Long identifier,
        String fieldName) {
        if (identifier == null || identifier <= 0) {
            throw new ValidationException(
                "%s must be a positive number"
                    .formatted(fieldName),
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

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ValidationException(
                "Page size must be between 1 and %d"
                    .formatted(MAX_PAGE_SIZE),
                ErrorCode.COMMON_VALIDATION_FAILED);
        }
    }
}