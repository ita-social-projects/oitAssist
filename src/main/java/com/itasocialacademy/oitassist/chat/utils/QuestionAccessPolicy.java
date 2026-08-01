package com.itasocialacademy.oitassist.chat.utils;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus.IN_PROGRESS;
import static com.itasocialacademy.oitassist.taskassignment.dao.enums.AssignmentVisibility.VISIBLE;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionCreationNotAllowedException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionForumAccessRestrictedException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.competition.api.dto.StageDetail;
import com.itasocialacademy.oitassist.competition.api.dto.TourDetail;
import com.itasocialacademy.oitassist.competition.exceptions.StageNotFoundException;
import com.itasocialacademy.oitassist.competition.exceptions.TourNotFoundException;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.participation.api.ParticipationFacade;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.taskassignment.api.TaskAssignmentFacade;
import com.itasocialacademy.oitassist.taskassignment.api.dto.TaskAssignmentDetailDTO;
import com.itasocialacademy.oitassist.taskassignment.exceptions.TaskAssignmentNotFoundException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuestionAccessPolicy {
    private static final String ADMIN_ROLE = "ADMIN";

    private final SecurityFacade securityFacade;
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
    public boolean isAssignedReviewer(
        QuestionThread questionThread) {
        return currentUserMatches(
            questionThread.getAssignedReviewerId());
    }

    /**
     * Determines whether the current user has the global administrator role.
     */
    public boolean isAdministrator() {
        return securityFacade.hasRole(ADMIN_ROLE);
    }

    /**
     * Validates that the authenticated user may view the forum belonging to the
     * specified task assignment.
     *
     * @param taskAssignmentId identifier of the task assignment
     * @return current authenticated user's identifier
     */
    public Long requireTaskAssignmentForumAccess(
        Long taskAssignmentId) {
        return requireTaskAssignmentParticipantAccess(
            taskAssignmentId).userId();
    }

    /**
     * Validates that the authenticated user may create a question for the specified
     * task assignment.
     *
     * <p>
     * Question creation is allowed only while the related tour is in progress.
     * </p>
     *
     * @param taskAssignmentId identifier of the task assignment
     * @return current authenticated user's identifier
     */
    public Long requireTaskAssignmentQuestionCreationAccess(
        Long taskAssignmentId) {
        TaskAssignmentAccessContext context =
            requireTaskAssignmentParticipantAccess(
                taskAssignmentId);

        if (context.tour().executionStatus() != IN_PROGRESS) {
            throw new QuestionCreationNotAllowedException(
                taskAssignmentId,
                context.tour().executionStatus());
        }

        return context.userId();
    }

    /**
     * Validates that the current user may view the supplied question thread.
     *
     * <p>
     * A public question is available to a participant who can access the related
     * task assignment. A private question is available to its author or assigned
     * reviewer when that user also satisfies the assignment-access rules.
     * </p>
     *
     * <p>
     * Administrators bypass assignment visibility and participation checks, but
     * only after the complete assignment hierarchy has been validated. Another
     * participant's private question is masked as not found before the
     * participation lookup is performed.
     * </p>
     *
     * @param question question thread whose details or messages are requested
     * @throws AuthenticationException                if the current user is not
     *                                                authenticated
     * @throws QuestionNotFoundException              if a private question must be
     *                                                masked
     * @throws QuestionForumAccessRestrictedException if assignment access is
     *                                                restricted
     */
    public void requireQuestionViewAccess(
        QuestionThread question) {
        TaskAssignmentAccessContext context =
            resolveTaskAssignmentContext(
                question.getTaskAssignmentId());

        if (isAdministrator()) {
            return;
        }

        boolean author = Objects.equals(
            context.userId(),
            question.getAuthorId());

        boolean assignedReviewer = Objects.equals(
            context.userId(),
            question.getAssignedReviewerId());

        if (question.getVisibility() == PRIVATE
            && !author
            && !assignedReviewer) {
            throw new QuestionNotFoundException(
                question.getId());
        }

        requireVisibleAssignment(context);
        requireParticipation(context);
    }

    private TaskAssignmentAccessContext requireTaskAssignmentParticipantAccess(
        Long taskAssignmentId) {
        TaskAssignmentAccessContext context =
            resolveTaskAssignmentContext(
                taskAssignmentId);

        /*
         * Administrators bypass assignment visibility and participation checks, but
         * only after the complete hierarchy has been validated.
         */
        if (isAdministrator()) {
            return context;
        }

        requireVisibleAssignment(context);
        requireParticipation(context);

        return context;
    }

    private TaskAssignmentAccessContext resolveTaskAssignmentContext(
        Long taskAssignmentId) {
        Long currentUserId = securityFacade
            .getCurrentUserId()
            .orElseThrow(() -> new AuthenticationException(
                "Authentication is required to access the question forum",
                ErrorCode.AUTHENTICATION_REQUIRED));

        TaskAssignmentDetailDTO assignment =
            taskAssignmentFacade
                .findAssignmentById(taskAssignmentId)
                .orElseThrow(() -> new TaskAssignmentNotFoundException(
                    taskAssignmentId));

        TourDetail tour = competitionFacade
            .findTourById(assignment.tourId())
            .orElseThrow(() -> new TourNotFoundException(
                assignment.tourId()));

        StageDetail stage = competitionFacade
            .findStageById(tour.stageId())
            .orElseThrow(() -> new StageNotFoundException(
                tour.stageId()));

        return new TaskAssignmentAccessContext(
            currentUserId,
            assignment,
            tour,
            stage);
    }

    private void requireVisibleAssignment(
        TaskAssignmentAccessContext context) {
        if (context.assignment().visibility() != VISIBLE) {
            throw new QuestionForumAccessRestrictedException(
                context.assignment().id());
        }
    }

    private void requireParticipation(
        TaskAssignmentAccessContext context) {
        boolean participant =
            participationFacade.isUserParticipant(
                context.userId(),
                context.stage().competitionId(),
                context.stage().id());

        if (!participant) {
            throw new QuestionForumAccessRestrictedException(
                context.assignment().id());
        }
    }

    private boolean currentUserMatches(
        Long expectedUserId) {
        if (expectedUserId == null) {
            return false;
        }

        return securityFacade.getCurrentUserId()
            .map(currentUserId -> Objects.equals(
                currentUserId,
                expectedUserId))
            .orElse(false);
    }

    private record TaskAssignmentAccessContext(
        Long userId,
        TaskAssignmentDetailDTO assignment,
        TourDetail tour,
        StageDetail stage) {
    }
}