package com.itasocialacademy.oitassist.chat.service;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PUBLIC;
import static com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus.CLOSED;
import static com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus.IN_PROGRESS;
import static com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus.SCHEDULED;
import static com.itasocialacademy.oitassist.taskassignment.dao.enums.AssignmentVisibility.HIDDEN;
import static com.itasocialacademy.oitassist.taskassignment.dao.enums.AssignmentVisibility.VISIBLE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionState;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionCreationNotAllowedException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionForumAccessRestrictedException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionNotFoundException;
import com.itasocialacademy.oitassist.chat.service.interfaces.TaskAssignmentForumResponderService;
import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.competition.api.dto.StageDetail;
import com.itasocialacademy.oitassist.competition.api.dto.TourDetail;
import com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus;
import com.itasocialacademy.oitassist.competition.exceptions.StageNotFoundException;
import com.itasocialacademy.oitassist.competition.exceptions.TourNotFoundException;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.participation.api.ParticipationFacade;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.taskassignment.api.TaskAssignmentFacade;
import com.itasocialacademy.oitassist.taskassignment.api.dto.TaskAssignmentDetailDTO;
import com.itasocialacademy.oitassist.taskassignment.dao.enums.AssignmentVisibility;
import com.itasocialacademy.oitassist.taskassignment.exceptions.TaskAssignmentNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ForumAccessServiceTest {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String ORG_ROLE = "ORG";

    private static final Long USER_ID = 100L;
    private static final Long TASK_ASSIGNMENT_ID = 200L;
    private static final Long TASK_BODY_ID = 300L;
    private static final Long TOUR_ID = 400L;
    private static final Long STAGE_ID = 500L;
    private static final Long COMPETITION_ID = 600L;
    private static final Long QUESTION_ID = 700L;
    private static final Long OTHER_USER_ID = 101L;

    @Mock
    private SecurityFacade securityFacade;

    @Mock
    private TaskAssignmentFacade taskAssignmentFacade;

    @Mock
    private CompetitionFacade competitionFacade;

    @Mock
    private ParticipationFacade participationFacade;

    @Mock
    private TaskAssignmentForumResponderService forumResponderService;

    @InjectMocks
    private ForumAccessService forumAccessService;

    @Test
    void requireTaskAssignmentForumAccess_visibleAssignmentAndParticipant_shouldReturnUserId() {
        stubParticipantAccess(VISIBLE, SCHEDULED);

        Long result = forumAccessService
            .requireTaskAssignmentForumAccess(TASK_ASSIGNMENT_ID);

        assertEquals(USER_ID, result);

        verify(taskAssignmentFacade)
            .findAssignmentById(TASK_ASSIGNMENT_ID);
        verify(competitionFacade).findTourById(TOUR_ID);
        verify(competitionFacade).findStageById(STAGE_ID);
        verify(participationFacade).isUserParticipant(
            USER_ID,
            COMPETITION_ID,
            STAGE_ID);
        verifyNoInteractions(forumResponderService);
    }

    @Test
    void requireTaskAssignmentForumAccess_unauthenticated_shouldThrowAuthenticationException() {
        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.empty());

        assertThrows(
            AuthenticationException.class,
            () -> forumAccessService
                .requireTaskAssignmentForumAccess(
                    TASK_ASSIGNMENT_ID));

        verifyNoInteractions(
            taskAssignmentFacade,
            competitionFacade,
            participationFacade,
            forumResponderService);
    }

    @Test
    void requireTaskAssignmentForumAccess_missingAssignment_shouldThrowTaskAssignmentNotFoundException() {
        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.of(USER_ID));
        when(taskAssignmentFacade.findAssignmentById(
            TASK_ASSIGNMENT_ID)).thenReturn(Optional.empty());

        assertThrows(
            TaskAssignmentNotFoundException.class,
            () -> forumAccessService
                .requireTaskAssignmentForumAccess(
                    TASK_ASSIGNMENT_ID));

        verify(taskAssignmentFacade)
            .findAssignmentById(TASK_ASSIGNMENT_ID);

        verifyNoInteractions(
            competitionFacade,
            participationFacade,
            forumResponderService);
    }

    @Test
    void requireTaskAssignmentForumAccess_missingTour_shouldThrowTourNotFoundException() {
        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.of(USER_ID));
        when(taskAssignmentFacade.findAssignmentById(
            TASK_ASSIGNMENT_ID)).thenReturn(Optional.of(
                createAssignment(VISIBLE)));
        when(competitionFacade.findTourById(TOUR_ID))
            .thenReturn(Optional.empty());

        assertThrows(
            TourNotFoundException.class,
            () -> forumAccessService
                .requireTaskAssignmentForumAccess(
                    TASK_ASSIGNMENT_ID));

        verify(competitionFacade).findTourById(TOUR_ID);
        verify(competitionFacade, never())
            .findStageById(anyLong());
        verifyNoInteractions(
            participationFacade,
            forumResponderService);
    }

    @Test
    void requireTaskAssignmentForumAccess_missingStage_shouldThrowStageNotFoundException() {
        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.of(USER_ID));
        when(taskAssignmentFacade.findAssignmentById(
            TASK_ASSIGNMENT_ID)).thenReturn(Optional.of(
                createAssignment(VISIBLE)));
        when(competitionFacade.findTourById(TOUR_ID))
            .thenReturn(Optional.of(
                createTour(SCHEDULED)));
        when(competitionFacade.findStageById(STAGE_ID))
            .thenReturn(Optional.empty());

        assertThrows(
            StageNotFoundException.class,
            () -> forumAccessService
                .requireTaskAssignmentForumAccess(
                    TASK_ASSIGNMENT_ID));

        verify(competitionFacade).findStageById(STAGE_ID);
        verifyNoInteractions(
            participationFacade,
            forumResponderService);
    }

    @Test
    void requireTaskAssignmentForumAccess_hiddenAssignment_shouldThrowAccessRestrictedException() {
        stubAuthenticatedHierarchy(HIDDEN, SCHEDULED);
        stubRoles(false, false);

        assertThrows(
            QuestionForumAccessRestrictedException.class,
            () -> forumAccessService
                .requireTaskAssignmentForumAccess(
                    TASK_ASSIGNMENT_ID));

        verifyNoInteractions(
            participationFacade,
            forumResponderService);
    }

    @Test
    void requireTaskAssignmentForumAccess_withoutParticipation_shouldThrowAccessRestrictedException() {
        stubAuthenticatedHierarchy(VISIBLE, SCHEDULED);
        stubRoles(false, false);

        when(participationFacade.isUserParticipant(
            USER_ID,
            COMPETITION_ID,
            STAGE_ID)).thenReturn(false);

        assertThrows(
            QuestionForumAccessRestrictedException.class,
            () -> forumAccessService
                .requireTaskAssignmentForumAccess(
                    TASK_ASSIGNMENT_ID));

        verify(participationFacade).isUserParticipant(
            USER_ID,
            COMPETITION_ID,
            STAGE_ID);
    }

    @Test
    void requireTaskAssignmentForumAccess_adminWithoutParticipation_shouldReturnUserId() {
        stubAuthenticatedHierarchy(VISIBLE, SCHEDULED);
        stubRoles(true, false);

        Long result = forumAccessService
            .requireTaskAssignmentForumAccess(
                TASK_ASSIGNMENT_ID);

        assertEquals(USER_ID, result);

        verify(competitionFacade).findTourById(TOUR_ID);
        verify(competitionFacade).findStageById(STAGE_ID);
        verifyNoInteractions(
            participationFacade,
            forumResponderService);
    }

    @Test
    void requireTaskAssignmentForumAccess_adminHiddenAssignment_shouldReturnUserId() {
        stubAuthenticatedHierarchy(HIDDEN, SCHEDULED);
        stubRoles(true, false);

        Long result = forumAccessService
            .requireTaskAssignmentForumAccess(
                TASK_ASSIGNMENT_ID);

        assertEquals(USER_ID, result);

        verify(competitionFacade).findTourById(TOUR_ID);
        verify(competitionFacade).findStageById(STAGE_ID);
        verifyNoInteractions(
            participationFacade,
            forumResponderService);
    }

    @Test
    void requireTaskAssignmentForumAccess_orgRoleWithoutParticipation_shouldThrowAccessRestrictedException() {
        stubAuthenticatedHierarchy(VISIBLE, SCHEDULED);
        stubRoles(false, true);

        when(participationFacade.isUserParticipant(
            USER_ID,
            COMPETITION_ID,
            STAGE_ID)).thenReturn(false);

        assertThrows(
            QuestionForumAccessRestrictedException.class,
            () -> forumAccessService
                .requireTaskAssignmentForumAccess(
                    TASK_ASSIGNMENT_ID));

        verifyNoInteractions(forumResponderService);
    }

    @Test
    void requireTaskAssignmentForumAccess_responderWithoutParticipation_shouldThrowAccessRestrictedException() {
        stubAuthenticatedHierarchy(VISIBLE, SCHEDULED);
        stubRoles(false, true);
        givenResponderGrant();

        when(participationFacade.isUserParticipant(
            USER_ID,
            COMPETITION_ID,
            STAGE_ID)).thenReturn(false);

        assertThrows(
            QuestionForumAccessRestrictedException.class,
            () -> forumAccessService
                .requireTaskAssignmentForumAccess(
                    TASK_ASSIGNMENT_ID));
    }

    @Test
    void requireTaskAssignmentForumAccess_responderHiddenAssignment_shouldThrowAccessRestrictedException() {
        stubAuthenticatedHierarchy(HIDDEN, SCHEDULED);
        stubRoles(false, true);
        givenResponderGrant();

        assertThrows(
            QuestionForumAccessRestrictedException.class,
            () -> forumAccessService
                .requireTaskAssignmentForumAccess(
                    TASK_ASSIGNMENT_ID));

        verifyNoInteractions(participationFacade);
    }

    @Test
    void requireTaskAssignmentQuestionCreationAccess_inProgressTour_shouldReturnUserId() {
        stubParticipantAccess(VISIBLE, IN_PROGRESS);

        Long result = forumAccessService
            .requireTaskAssignmentQuestionCreationAccess(
                TASK_ASSIGNMENT_ID);

        assertEquals(USER_ID, result);

        verify(participationFacade).isUserParticipant(
            USER_ID,
            COMPETITION_ID,
            STAGE_ID);
    }

    @Test
    void requireTaskAssignmentQuestionCreationAccess_scheduledTour_shouldThrowCreationNotAllowedException() {
        stubParticipantAccess(VISIBLE, SCHEDULED);

        assertThrows(
            QuestionCreationNotAllowedException.class,
            () -> forumAccessService
                .requireTaskAssignmentQuestionCreationAccess(
                    TASK_ASSIGNMENT_ID));

        verify(participationFacade).isUserParticipant(
            USER_ID,
            COMPETITION_ID,
            STAGE_ID);
    }

    @Test
    void requireTaskAssignmentQuestionCreationAccess_closedTour_shouldThrowCreationNotAllowedException() {
        stubParticipantAccess(VISIBLE, CLOSED);

        assertThrows(
            QuestionCreationNotAllowedException.class,
            () -> forumAccessService
                .requireTaskAssignmentQuestionCreationAccess(
                    TASK_ASSIGNMENT_ID));

        verify(participationFacade).isUserParticipant(
            USER_ID,
            COMPETITION_ID,
            STAGE_ID);
    }

    @Test
    void requireQuestionViewAccess_publicQuestionAndParticipant_shouldAllow() {
        stubParticipantAccess(VISIBLE, SCHEDULED);

        QuestionThread question = createQuestion(OTHER_USER_ID, PUBLIC);

        assertDoesNotThrow(
            () -> forumAccessService.requireQuestionViewAccess(question));

        verify(participationFacade).isUserParticipant(
            USER_ID,
            COMPETITION_ID,
            STAGE_ID);
    }

    @Test
    void requireQuestionViewAccess_publicQuestionWithoutParticipation_shouldThrowAccessRestrictedException() {
        stubAuthenticatedHierarchy(VISIBLE, SCHEDULED);
        stubRoles(false, false);

        when(participationFacade.isUserParticipant(
            USER_ID,
            COMPETITION_ID,
            STAGE_ID)).thenReturn(false);

        QuestionThread question = createQuestion(OTHER_USER_ID, PUBLIC);

        assertThrows(
            QuestionForumAccessRestrictedException.class,
            () -> forumAccessService.requireQuestionViewAccess(question));
    }

    @Test
    void requireQuestionViewAccess_ownPrivateQuestion_shouldAllow() {
        stubParticipantAccess(VISIBLE, SCHEDULED);

        QuestionThread question = createQuestion(USER_ID, PRIVATE);

        assertDoesNotThrow(
            () -> forumAccessService.requireQuestionViewAccess(question));
    }

    @Test
    void requireQuestionViewAccess_othersPrivateQuestion_shouldMaskAsNotFound() {
        stubAuthenticatedHierarchy(VISIBLE, SCHEDULED);
        stubRoles(false, false);

        QuestionThread question = createQuestion(OTHER_USER_ID, PRIVATE);

        assertThrows(
            QuestionNotFoundException.class,
            () -> forumAccessService.requireQuestionViewAccess(question));

        verifyNoInteractions(participationFacade);
    }

    @Test
    void requireQuestionViewAccess_othersPrivateQuestionInHiddenAssignment_shouldMaskAsNotFound() {
        stubAuthenticatedHierarchy(HIDDEN, SCHEDULED);
        stubRoles(false, false);

        QuestionThread question = createQuestion(OTHER_USER_ID, PRIVATE);

        assertThrows(
            QuestionNotFoundException.class,
            () -> forumAccessService.requireQuestionViewAccess(question));

        verifyNoInteractions(participationFacade);
    }

    @Test
    void requireQuestionViewAccess_closedQuestion_shouldRemainReadable() {
        stubParticipantAccess(VISIBLE, SCHEDULED);

        QuestionThread question = createQuestion(OTHER_USER_ID, PUBLIC);
        question.setState(QuestionState.CLOSED);

        assertDoesNotThrow(
            () -> forumAccessService.requireQuestionViewAccess(question));
    }

    @Test
    void requireQuestionViewAccess_administratorAndOthersPrivateQuestion_shouldAllowWithoutParticipation() {
        stubAuthenticatedHierarchy(VISIBLE, SCHEDULED);
        stubRoles(true, false);

        QuestionThread question = createQuestion(OTHER_USER_ID, PRIVATE);

        assertDoesNotThrow(
            () -> forumAccessService.requireQuestionViewAccess(question));

        verifyNoInteractions(
            participationFacade,
            forumResponderService);
    }

    @Test
    void requireQuestionViewAccess_assignedResponderAndPrivateQuestion_shouldAllowWithoutParticipation() {
        stubAuthenticatedHierarchy(HIDDEN, SCHEDULED);
        stubRoles(false, true);

        when(forumResponderService.isResponder(
            TASK_ASSIGNMENT_ID,
            USER_ID)).thenReturn(true);

        QuestionThread question = createQuestion(OTHER_USER_ID, PRIVATE);
        question.setAssignedReviewerId(USER_ID);

        assertDoesNotThrow(
            () -> forumAccessService.requireQuestionViewAccess(question));

        verifyNoInteractions(participationFacade);
    }

    @Test
    void requireQuestionCommentAccess_assignedResponder_shouldReturnCurrentUserId() {
        stubAuthenticatedHierarchy(VISIBLE, SCHEDULED);
        stubRoles(false, true);

        when(forumResponderService.isResponder(
            TASK_ASSIGNMENT_ID,
            USER_ID)).thenReturn(true);

        QuestionThread question = createQuestion(OTHER_USER_ID, PRIVATE);
        question.setAssignedReviewerId(USER_ID);

        assertEquals(USER_ID, forumAccessService.requireQuestionCommentAccess(question));
    }

    @Test
    void requireQuestionViewAccess_responderAndQuestionAssignedToAnotherReviewer_shouldMaskAsNotFound() {
        stubAuthenticatedHierarchy(VISIBLE, SCHEDULED);
        stubRoles(false, true);
        givenResponderGrant();

        QuestionThread question = createQuestion(OTHER_USER_ID, PRIVATE);
        question.setAssignedReviewerId(OTHER_USER_ID);

        assertThrows(
            QuestionNotFoundException.class,
            () -> forumAccessService.requireQuestionViewAccess(question));

        verifyNoInteractions(participationFacade);
    }

    @Test
    void requireQuestionViewAccess_responderAndUnclaimedPrivateQuestion_shouldMaskAsNotFound() {
        stubAuthenticatedHierarchy(VISIBLE, SCHEDULED);
        stubRoles(false, true);
        givenResponderGrant();

        QuestionThread question = createQuestion(OTHER_USER_ID, PRIVATE);

        assertThrows(
            QuestionNotFoundException.class,
            () -> forumAccessService.requireQuestionViewAccess(question));

        verifyNoInteractions(participationFacade);
    }

    @Test
    void requireQuestionViewAccess_assignedReviewerWithoutResponderGrant_shouldMaskAsNotFound() {
        stubAuthenticatedHierarchy(VISIBLE, SCHEDULED);
        stubRoles(false, true);

        when(forumResponderService.isResponder(
            TASK_ASSIGNMENT_ID,
            USER_ID)).thenReturn(false);

        QuestionThread question = createQuestion(OTHER_USER_ID, PRIVATE);
        question.setAssignedReviewerId(USER_ID);

        assertThrows(
            QuestionNotFoundException.class,
            () -> forumAccessService.requireQuestionViewAccess(question));

        verifyNoInteractions(participationFacade);
    }

    @Test
    void requireQuestionViewAccess_assignedReviewerWithoutOrgRole_shouldUseParticipantRules() {
        stubAuthenticatedHierarchy(VISIBLE, SCHEDULED);
        stubRoles(false, false);

        QuestionThread question = createQuestion(OTHER_USER_ID, PRIVATE);
        question.setAssignedReviewerId(USER_ID);

        assertThrows(
            QuestionNotFoundException.class,
            () -> forumAccessService.requireQuestionViewAccess(question));

        verifyNoInteractions(
            participationFacade,
            forumResponderService);
    }

    @Test
    void requireQuestionViewAccess_unauthenticated_shouldThrowAuthenticationException() {
        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.empty());

        QuestionThread question = createQuestion(OTHER_USER_ID, PUBLIC);

        assertThrows(
            AuthenticationException.class,
            () -> forumAccessService.requireQuestionViewAccess(question));

        verifyNoInteractions(
            taskAssignmentFacade,
            competitionFacade,
            participationFacade,
            forumResponderService);
    }

    @Test
    void requireQuestionCommentAccess_publicQuestionAndParticipant_shouldReturnCurrentUserId() {
        stubParticipantAccess(VISIBLE, IN_PROGRESS);

        Long result = forumAccessService.requireQuestionCommentAccess(
            createQuestion(OTHER_USER_ID, PUBLIC));

        assertEquals(USER_ID, result);
    }

    @Test
    void requireQuestionCommentAccess_ownPrivateQuestion_shouldReturnCurrentUserId() {
        stubParticipantAccess(VISIBLE, IN_PROGRESS);

        Long result = forumAccessService.requireQuestionCommentAccess(
            createQuestion(USER_ID, PRIVATE));

        assertEquals(USER_ID, result);
    }

    @Test
    void requireQuestionCommentAccess_othersPrivateQuestion_shouldMaskAsNotFound() {
        stubAuthenticatedHierarchy(VISIBLE, IN_PROGRESS);
        stubRoles(false, false);

        QuestionThread question = createQuestion(OTHER_USER_ID, PRIVATE);

        assertThrows(
            QuestionNotFoundException.class,
            () -> forumAccessService.requireQuestionCommentAccess(question));

        verifyNoInteractions(participationFacade);
    }

    @Test
    void requireQuestionCommentAccess_publicQuestionWithoutParticipation_shouldThrowAccessRestrictedException() {
        stubAuthenticatedHierarchy(VISIBLE, IN_PROGRESS);
        stubRoles(false, false);

        when(participationFacade.isUserParticipant(
            USER_ID,
            COMPETITION_ID,
            STAGE_ID)).thenReturn(false);

        QuestionThread question = createQuestion(OTHER_USER_ID, PUBLIC);

        assertThrows(
            QuestionForumAccessRestrictedException.class,
            () -> forumAccessService.requireQuestionCommentAccess(question));
    }

    private void stubParticipantAccess(
        AssignmentVisibility visibility,
        ExecutionStatus executionStatus) {
        stubAuthenticatedHierarchy(
            visibility,
            executionStatus);

        stubRoles(false, false);

        when(participationFacade.isUserParticipant(
            USER_ID,
            COMPETITION_ID,
            STAGE_ID)).thenReturn(true);
    }

    private void givenResponderGrant() {
        lenient().when(forumResponderService.isResponder(
            TASK_ASSIGNMENT_ID,
            USER_ID)).thenReturn(true);
    }

    private void stubRoles(
        boolean administrator,
        boolean organization) {
        when(securityFacade.hasRole(anyString()))
            .thenAnswer(invocation -> {
                String role = invocation.getArgument(0);

                return ADMIN_ROLE.equals(role) && administrator
                    || ORG_ROLE.equals(role) && organization;
            });
    }

    private void stubAuthenticatedHierarchy(
        AssignmentVisibility visibility,
        ExecutionStatus executionStatus) {
        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.of(USER_ID));

        when(taskAssignmentFacade.findAssignmentById(
            TASK_ASSIGNMENT_ID)).thenReturn(Optional.of(
                createAssignment(visibility)));

        when(competitionFacade.findTourById(TOUR_ID))
            .thenReturn(Optional.of(
                createTour(executionStatus)));

        when(competitionFacade.findStageById(STAGE_ID))
            .thenReturn(Optional.of(
                createStage()));
    }

    private QuestionThread createQuestion(
        Long authorId,
        QuestionVisibility visibility) {
        return QuestionThread.builder()
            .id(QUESTION_ID)
            .taskAssignmentId(TASK_ASSIGNMENT_ID)
            .authorId(authorId)
            .title("Clarification about input format")
            .content("May the input contain duplicate values?")
            .visibility(visibility)
            .build();
    }

    private TaskAssignmentDetailDTO createAssignment(
        AssignmentVisibility visibility) {
        return new TaskAssignmentDetailDTO(
            TASK_ASSIGNMENT_ID,
            TASK_BODY_ID,
            TOUR_ID,
            visibility,
            100,
            null);
    }

    private TourDetail createTour(
        ExecutionStatus executionStatus) {
        return TourDetail.builder()
            .id(TOUR_ID)
            .stageId(STAGE_ID)
            .title("Final tour")
            .executionStatus(executionStatus)
            .build();
    }

    private StageDetail createStage() {
        return StageDetail.builder()
            .id(STAGE_ID)
            .competitionId(COMPETITION_ID)
            .title("Final stage")
            .build();
    }
}
