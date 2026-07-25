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
import com.itasocialacademy.oitassist.chat.exceptions.QuestionCreationNotAllowedException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionForumAccessRestrictedException;
import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.competition.api.dto.StageDetail;
import com.itasocialacademy.oitassist.competition.api.dto.TourDetail;
import com.itasocialacademy.oitassist.competition.exceptions.StageNotFoundException;
import com.itasocialacademy.oitassist.competition.exceptions.TourNotFoundException;
import com.itasocialacademy.oitassist.participation.api.ParticipationFacade;
import com.itasocialacademy.oitassist.taskassignment.api.TaskAssignmentFacade;
import com.itasocialacademy.oitassist.taskassignment.api.dto.TaskAssignmentDetailDTO;
import com.itasocialacademy.oitassist.taskassignment.exceptions.TaskAssignmentNotFoundException;
import static com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus.IN_PROGRESS;
import static com.itasocialacademy.oitassist.taskassignment.dao.enums.AssignmentVisibility.VISIBLE;

@Component
@RequiredArgsConstructor
public class QuestionAccessPolicy {
    private static final String ADMIN_ROLE = "ADMIN";

    private final SecurityFacade securityFacade;
    private final TaskBodyFacade taskBodyFacade;
    private final TaskAssignmentFacade taskAssignmentFacade;
    private final CompetitionFacade competitionFacade;
    private final ParticipationFacade participationFacade;

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
        return hasTaskAccess(questionThread.getTaskAssignmentId());
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

    /**
     * Requires the current user to be authenticated and verifies that the temporary
     * TaskBody-based forum context exists.
     *
     * <p>
     * This method is used by participant forum operations that need both the
     * authenticated user identifier and validation of the requested task. The
     * TaskBody-based access check is temporary and will be replaced with
     * TaskAssignment hierarchy access.
     * </p>
     *
     * @param taskId identifier of the task whose forum is being accessed
     * @return identifier of the currently authenticated user
     * @throws AuthenticationException if the current user is not authenticated
     */
    public Long requireTaskForumAccess(Long taskId) {
        // TODO: replace temporary TaskBody access with TaskAssignment hierarchy access.
        Long currentUserId = securityFacade.getCurrentUserId()
            .orElseThrow(() -> new AuthenticationException("Authentication is required to access the question forum",
                ErrorCode.AUTHENTICATION_REQUIRED));

        taskBodyFacade.findTaskBodyById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        return currentUserId;
    }

    /**
     * Validates that the authenticated user may view the forum belonging to the
     * specified task assignment.
     *
     * @return current authenticated user's identifier
     */
    public Long requireTaskAssignmentForumAccess(Long taskAssignmentId) {
        return resolveTaskAssignmentAccess(taskAssignmentId).userId();
    }

    /**
     * Validates that the authenticated user may create a question for the
     * specified task assignment.
     *
     * <p>Question creation is allowed only while the related tour is in progress.
     *
     * @return current authenticated user's identifier
     */
    public Long requireTaskAssignmentQuestionCreationAccess(Long taskAssignmentId) {
        TaskAssignmentAccessContext context =
                resolveTaskAssignmentAccess(taskAssignmentId);

        if (context.tour().executionStatus() != IN_PROGRESS) {
            throw new QuestionCreationNotAllowedException(
                    taskAssignmentId,
                    context.tour().executionStatus()
            );
        }

        return context.userId();
    }

    private TaskAssignmentAccessContext resolveTaskAssignmentAccess(Long taskAssignmentId) {
        Long currentUserId = securityFacade.getCurrentUserId()
                .orElseThrow(() -> new AuthenticationException(
                        "Authentication is required to access the question forum",
                        ErrorCode.AUTHENTICATION_REQUIRED
                ));

        TaskAssignmentDetailDTO assignment = taskAssignmentFacade.findAssignmentById(taskAssignmentId)
                .orElseThrow(() ->
                        new TaskAssignmentNotFoundException(taskAssignmentId)
                );

        TourDetail tour = competitionFacade.findTourById(assignment.tourId())
                .orElseThrow(() ->
                        new TourNotFoundException(assignment.tourId())
                );

        StageDetail stage = competitionFacade.findStageById(tour.stageId())
                .orElseThrow(() ->
                        new StageNotFoundException(tour.stageId())
                );

        /*
         * Administrators bypass assignment visibility and participation checks,
         * but only after the complete assignment hierarchy has been validated.
         */
        if (isAdministrator()) {
            return new TaskAssignmentAccessContext(
                    currentUserId,
                    assignment,
                    tour,
                    stage
            );
        }

        if (assignment.visibility() != VISIBLE) {
            throw new QuestionForumAccessRestrictedException(
                    taskAssignmentId
            );
        }

        boolean isParticipant = participationFacade.isUserParticipant(
                currentUserId,
                stage.competitionId(),
                stage.id()
        );

        if (!isParticipant) {
            throw new QuestionForumAccessRestrictedException(
                    taskAssignmentId
            );
        }

        return new TaskAssignmentAccessContext(
                currentUserId,
                assignment,
                tour,
                stage
        );
    }

    private record TaskAssignmentAccessContext(
            Long userId,
            TaskAssignmentDetailDTO assignment,
            TourDetail tour,
            StageDetail stage
    ) {
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