package com.itasocialacademy.oitassist.chat.service;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.NEW;
import static com.itasocialacademy.oitassist.core.config.PaginationConfig.MAX_PAGE_SIZE;
import com.itasocialacademy.oitassist.chat.dao.dto.response.AdminQuestionInboxItemResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
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

    private final QuestionThreadRepository questionThreadRepository;
    private final QuestionThreadMapper questionThreadMapper;
    private final SecurityFacade securityFacade;

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
}