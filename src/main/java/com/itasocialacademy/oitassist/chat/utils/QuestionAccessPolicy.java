package com.itasocialacademy.oitassist.chat.utils;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus.IN_PROGRESS;
import static com.itasocialacademy.oitassist.taskassignment.dao.enums.AssignmentVisibility.VISIBLE;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionCreationNotAllowedException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionForumAccessRestrictedException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import com.itasocialacademy.oitassist.chat.service.interfaces.TaskAssignmentForumResponderService;
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
    private static final String ORG_ROLE = "ORG";

    private final SecurityFacade securityFacade;
    private final TaskAssignmentFacade taskAssignmentFacade;
    private final CompetitionFacade competitionFacade;
    private final ParticipationFacade participationFacade;
    private final TaskAssignmentForumResponderService forumResponderService;

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

    public boolean isOrganizationResponder() {
        return securityFacade.hasRole(ORG_ROLE);
    }

    public boolean isOrganizationResponder(Long taskAssignmentId) {
        return securityFacade.hasRole(ORG_ROLE)
            && forumResponderService.isResponder(taskAssignmentId,
                securityFacade.getCurrentUserId().orElse(null));
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
     * @param question question whose details or messages are requested
     */
    public void requireQuestionViewAccess(
        QuestionThread question) {
        requireQuestionAccess(question);
    }

    /**
     * Validates that the current user may participate in the supplied question by
     * adding a comment.
     *
     * <p>
     * The method applies the same assignment, visibility, authorship, reviewer and
     * administrator rules as question viewing and returns the authenticated user
     * identifier for server-controlled message authorship.
     * </p>
     *
     * @param question question to which a comment is being added
     * @return identifier of the authenticated and authorized user
     */
    public Long requireQuestionCommentAccess(
        QuestionThread question) {
        return requireQuestionAccess(question).userId();
    }

    private TaskAssignmentAccessContext requireQuestionAccess(
        QuestionThread question) {
        TaskAssignmentAccessContext context =
            resolveTaskAssignmentContext(
                question.getTaskAssignmentId());

        /*
         * Global administrators retain the existing complete bypass, but only after the
         * persisted TaskAssignment hierarchy has been validated.
         */
        if (isAdministrator()) {
            return context;
        }

        /*
         * An assigned ORG responder receives a question-scoped bypass of participant
         * visibility and participation requirements.
         *
         * Ownership alone is insufficient: the matching responder grant for the
         * question's exact TaskAssignment is also required.
         */
        if (isOrganizationResponder(question.getTaskAssignmentId())) {
            return context;
        }

//        if (isAssignedOrganizationResponder(
//            question,
//            context.userId())) {
//            return context;
//        }

        boolean author =
            Objects.equals(
                context.userId(),
                question.getAuthorId());

        /*
         * assignedReviewerId alone must not expose a private question.
         *
         * A reviewer obtains the bypass only through the validated ORG branch above.
         * The remaining branch represents ordinary participant access.
         */
        if (question.getVisibility() == PRIVATE
            && !author) {
            throw new QuestionNotFoundException(
                question.getId());
        }

        requireVisibleAssignment(context);
        requireParticipation(context);

        return context;
    }

    private boolean isAssignedOrganizationResponder(
        QuestionThread question,
        Long currentUserId) {
        /*
         * Check ownership first so ordinary participants and unrelated ORG users do not
         * cause unnecessary responder-assignment queries.
         */
        if (!Objects.equals(
            currentUserId,
            question.getAssignedReviewerId())) {
            return false;
        }

        if (!securityFacade.hasRole(ORG_ROLE)) {
            return false;
        }

        return forumResponderService.isResponder(
            question.getTaskAssignmentId(),
            currentUserId);
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

        if (isOrganizationResponder(taskAssignmentId)) {
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