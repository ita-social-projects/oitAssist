package com.itasocialacademy.oitassist.chat.utils;

import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.task.api.TaskBodyFacade;
import java.util.Objects;
import com.itasocialacademy.oitassist.task.exceptions.TaskNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuestionAccessPolicy {
    private static final String ADMIN_ROLE = "ADMIN";

    private final SecurityFacade securityFacade;
    private final TaskBodyFacade taskBodyFacade;

    /**
     * Determines whether the current user created the question.
     */
    public boolean isAuthor(QuestionThread questionThread) {
        return currentUserMatches(questionThread.getAuthorId());
    }

    /**
     * Determines whether the current user is assigned to review the question.
     */
    public boolean isAssignedReviewer(QuestionThread questionThread) {
        return currentUserMatches(questionThread.getAssignedReviewerId());
    }

    /**
     * Determines whether the current user has the global administrator role.
     */
    public boolean isAdministrator() {
        return securityFacade.hasRole(ADMIN_ROLE);
    }

    /**
     * Determines whether the current user may access the temporary TaskBody-based
     * forum context.
     */
    public boolean hasTaskAccess(QuestionThread questionThread) {
        return hasTaskAccess(questionThread.getTaskId());
    }

    /**
     * Determines whether the current user may access the temporary TaskBody-based
     * forum context.
     */
    public boolean hasTaskAccess(Long taskId) {
        // TODO: use the TaskAssignmentFacade to check for the access
        if (taskId == null || taskId <= 0) {
            return false;
        }

        return securityFacade.getCurrentUserId().isPresent()
            && taskBodyFacade.findTaskBodyById(taskId).isPresent();
    }

    public Long requireTaskForumAccess(Long taskId) {
        // TODO: use the TaskAssignmentFacade to check for the access
        Long currentUserId = securityFacade.getCurrentUserId()
            .orElseThrow(() -> new AuthenticationException(
                "Authentication is required to access the question forum",
                ErrorCode.AUTHENTICATION_REQUIRED));

        taskBodyFacade.findTaskBodyById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));

        return currentUserId;
    }

    private boolean currentUserMatches(Long expectedUserId) {
        if (expectedUserId == null) {
            return false;
        }

        return securityFacade.getCurrentUserId()
            .map(currentUserId -> Objects.equals(currentUserId, expectedUserId))
            .orElse(false);
    }
}