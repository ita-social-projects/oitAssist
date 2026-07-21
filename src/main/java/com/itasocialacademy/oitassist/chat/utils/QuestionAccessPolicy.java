package com.itasocialacademy.oitassist.chat.utils;

import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.task.api.interfaces.TaskForumFacade;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuestionAccessPolicy {
    private static final String ADMIN_ROLE = "ADMIN";

    private final SecurityFacade securityFacade;
    private final TaskForumFacade taskForumFacade;

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
        return securityFacade.getCurrentUserId()
            .map(userId -> taskForumFacade.canUserAccessForum(taskId, userId))
            .orElse(false);
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