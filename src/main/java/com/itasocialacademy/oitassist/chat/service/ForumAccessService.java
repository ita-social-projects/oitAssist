package com.itasocialacademy.oitassist.chat.service;

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
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ForumAccessService {
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String ORG_ROLE = "ORG";

    private final SecurityFacade securityFacade;
    private final TaskAssignmentFacade taskAssignmentFacade;
    private final CompetitionFacade competitionFacade;
    private final ParticipationFacade participationFacade;
    private final TaskAssignmentForumResponderService forumResponderService;

    public boolean isAdministrator() {
        return securityFacade.hasRole(ADMIN_ROLE);
    }

    public boolean isOrganizationResponder(Long taskAssignmentId) {
        return securityFacade.hasRole(ORG_ROLE)
            && forumResponderService.isResponder(taskAssignmentId, securityFacade.getCurrentUserId().orElse(null));
    }

    public Long requireTaskAssignmentForumAccess(Long taskAssignmentId) {
        return requireTaskAssignmentParticipantAccess(taskAssignmentId).userId();
    }

    public Long requireTaskAssignmentQuestionCreationAccess(Long taskAssignmentId) {
        TaskAssignmentAccessContext context = requireTaskAssignmentParticipantAccess(taskAssignmentId);
        if (context.tour().executionStatus() != IN_PROGRESS) {
            throw new QuestionCreationNotAllowedException(taskAssignmentId, context.tour().executionStatus());
        }
        return context.userId();
    }

    public void requireQuestionViewAccess(QuestionThread question) {
        requireQuestionAccess(question);
    }

    public Long requireQuestionCommentAccess(QuestionThread question) {
        return requireQuestionAccess(question).userId();
    }

    private TaskAssignmentAccessContext requireQuestionAccess(QuestionThread question) {
        TaskAssignmentAccessContext context = resolveTaskAssignmentContext(question.getTaskAssignmentId());
        if (isAdministrator()) {
            return context;
        }
        if (isOrganizationResponder(question.getTaskAssignmentId())) {
            return context;
        }

        boolean author = Objects.equals(context.userId(), question.getAuthorId());
        if (question.getVisibility() == PRIVATE && !author) {
            throw new QuestionNotFoundException(question.getId());
        }

        requireVisibleAssignment(context);
        requireParticipation(context);
        return context;
    }

    private TaskAssignmentAccessContext requireTaskAssignmentParticipantAccess(Long taskAssignmentId) {
        TaskAssignmentAccessContext context = resolveTaskAssignmentContext(taskAssignmentId);
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

    private TaskAssignmentAccessContext resolveTaskAssignmentContext(Long taskAssignmentId) {
        Long currentUserId = securityFacade.getCurrentUserId()
            .orElseThrow(() -> new AuthenticationException(
                "Authentication is required to access the question forum",
                ErrorCode.AUTHENTICATION_REQUIRED));

        TaskAssignmentDetailDTO assignment = taskAssignmentFacade.findAssignmentById(taskAssignmentId)
            .orElseThrow(() -> new TaskAssignmentNotFoundException(taskAssignmentId));
        TourDetail tour = competitionFacade.findTourById(assignment.tourId())
            .orElseThrow(() -> new TourNotFoundException(assignment.tourId()));
        StageDetail stage = competitionFacade.findStageById(tour.stageId())
            .orElseThrow(() -> new StageNotFoundException(tour.stageId()));

        return new TaskAssignmentAccessContext(currentUserId, assignment, tour, stage);
    }

    private void requireVisibleAssignment(TaskAssignmentAccessContext context) {
        if (context.assignment().visibility() != VISIBLE) {
            throw new QuestionForumAccessRestrictedException(context.assignment().id());
        }
    }

    private void requireParticipation(TaskAssignmentAccessContext context) {
        boolean participant = participationFacade.isUserParticipant(
            context.userId(),
            context.stage().competitionId(),
            context.stage().id());
        if (!participant) {
            throw new QuestionForumAccessRestrictedException(context.assignment().id());
        }
    }

    private record TaskAssignmentAccessContext(
        Long userId,
        TaskAssignmentDetailDTO assignment,
        TourDetail tour,
        StageDetail stage) {
    }
}