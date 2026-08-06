package com.itasocialacademy.oitassist.chat.utils;

import static com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus.CLOSED;
import static com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus.IN_PROGRESS;
import static com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus.SCHEDULED;
import static com.itasocialacademy.oitassist.taskassignment.dao.enums.AssignmentVisibility.HIDDEN;
import static com.itasocialacademy.oitassist.taskassignment.dao.enums.AssignmentVisibility.VISIBLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionCreationNotAllowedException;
import com.itasocialacademy.oitassist.chat.exceptions.QuestionForumAccessRestrictedException;
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
class QuestionAccessPolicyTest {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String ORG_ROLE = "ORG";

    private static final Long USER_ID = 100L;
    private static final Long TASK_ASSIGNMENT_ID = 200L;
    private static final Long TASK_BODY_ID = 300L;
    private static final Long TOUR_ID = 400L;
    private static final Long STAGE_ID = 500L;
    private static final Long COMPETITION_ID = 600L;

    private static final Long OTHER_STAGE_ID = 501L;
    private static final Long OTHER_COMPETITION_ID = 601L;

    @Mock
    private SecurityFacade securityFacade;

    @Mock
    private TaskAssignmentFacade taskAssignmentFacade;

    @Mock
    private CompetitionFacade competitionFacade;

    @Mock
    private ParticipationFacade participationFacade;

    @InjectMocks
    private QuestionAccessPolicy questionAccessPolicy;

    @Test
    void requireTaskAssignmentForumAccess_visibleAssignmentAndParticipant_shouldReturnUserId() {
        stubParticipantAccess(VISIBLE, SCHEDULED);

        Long result = questionAccessPolicy
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
    }

    @Test
    void requireTaskAssignmentForumAccess_unauthenticated_shouldThrowAuthenticationException() {
        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.empty());

        assertThrows(
            AuthenticationException.class,
            () -> questionAccessPolicy
                .requireTaskAssignmentForumAccess(
                    TASK_ASSIGNMENT_ID));

        verifyNoInteractions(
            taskAssignmentFacade,
            competitionFacade,
            participationFacade);
    }

    @Test
    void requireTaskAssignmentForumAccess_missingAssignment_shouldThrowTaskAssignmentNotFoundException() {
        when(securityFacade.getCurrentUserId())
            .thenReturn(Optional.of(USER_ID));
        when(taskAssignmentFacade.findAssignmentById(
            TASK_ASSIGNMENT_ID)).thenReturn(Optional.empty());

        assertThrows(
            TaskAssignmentNotFoundException.class,
            () -> questionAccessPolicy
                .requireTaskAssignmentForumAccess(
                    TASK_ASSIGNMENT_ID));

        verify(taskAssignmentFacade)
            .findAssignmentById(TASK_ASSIGNMENT_ID);

        verifyNoInteractions(
            competitionFacade,
            participationFacade);
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
            () -> questionAccessPolicy
                .requireTaskAssignmentForumAccess(
                    TASK_ASSIGNMENT_ID));

        verify(competitionFacade).findTourById(TOUR_ID);
        verify(competitionFacade, never())
            .findStageById(anyLong());
        verifyNoInteractions(participationFacade);
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
            () -> questionAccessPolicy
                .requireTaskAssignmentForumAccess(
                    TASK_ASSIGNMENT_ID));

        verify(competitionFacade).findStageById(STAGE_ID);
        verifyNoInteractions(participationFacade);
    }

    @Test
    void requireTaskAssignmentForumAccess_hiddenAssignment_shouldThrowAccessRestrictedException() {
        stubAuthenticatedHierarchy(HIDDEN, SCHEDULED);

        when(securityFacade.hasRole(ADMIN_ROLE))
            .thenReturn(false);

        assertThrows(
            QuestionForumAccessRestrictedException.class,
            () -> questionAccessPolicy
                .requireTaskAssignmentForumAccess(
                    TASK_ASSIGNMENT_ID));

        verifyNoInteractions(participationFacade);
    }

    @Test
    void requireTaskAssignmentForumAccess_withoutParticipation_shouldThrowAccessRestrictedException() {
        stubAuthenticatedHierarchy(VISIBLE, SCHEDULED);

        when(securityFacade.hasRole(ADMIN_ROLE))
            .thenReturn(false);
        when(participationFacade.isUserParticipant(
            USER_ID,
            COMPETITION_ID,
            STAGE_ID)).thenReturn(false);

        assertThrows(
            QuestionForumAccessRestrictedException.class,
            () -> questionAccessPolicy
                .requireTaskAssignmentForumAccess(
                    TASK_ASSIGNMENT_ID));

        verify(participationFacade).isUserParticipant(
            USER_ID,
            COMPETITION_ID,
            STAGE_ID);
    }

    @Test
    void requireTaskAssignmentForumAccess_participantFromAnotherStage_shouldThrowAccessRestrictedException() {
        stubAuthenticatedHierarchy(VISIBLE, SCHEDULED);

        when(securityFacade.hasRole(ADMIN_ROLE))
            .thenReturn(false);

        /*
         * The user participates only in OTHER_STAGE_ID. The exact assignment stage
         * therefore returns false.
         */
        when(participationFacade.isUserParticipant(
            eq(USER_ID),
            eq(COMPETITION_ID),
            anyLong())).thenAnswer(invocation -> OTHER_STAGE_ID.equals(invocation.getArgument(2)));

        assertThrows(
            QuestionForumAccessRestrictedException.class,
            () -> questionAccessPolicy
                .requireTaskAssignmentForumAccess(
                    TASK_ASSIGNMENT_ID));

        verify(participationFacade).isUserParticipant(
            USER_ID,
            COMPETITION_ID,
            STAGE_ID);
    }

    @Test
    void requireTaskAssignmentForumAccess_participantFromAnotherCompetition_shouldThrowAccessRestrictedException() {
        stubAuthenticatedHierarchy(VISIBLE, SCHEDULED);

        when(securityFacade.hasRole(ADMIN_ROLE))
            .thenReturn(false);

        /*
         * The user participates only in OTHER_COMPETITION_ID. The assignment
         * competition therefore returns false.
         */
        when(participationFacade.isUserParticipant(
            eq(USER_ID),
            anyLong(),
            eq(STAGE_ID))).thenAnswer(invocation -> OTHER_COMPETITION_ID.equals(invocation.getArgument(1)));

        assertThrows(
            QuestionForumAccessRestrictedException.class,
            () -> questionAccessPolicy
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

        when(securityFacade.hasRole(ADMIN_ROLE))
            .thenReturn(true);

        Long result = questionAccessPolicy
            .requireTaskAssignmentForumAccess(
                TASK_ASSIGNMENT_ID);

        assertEquals(USER_ID, result);

        verify(competitionFacade).findTourById(TOUR_ID);
        verify(competitionFacade).findStageById(STAGE_ID);
        verifyNoInteractions(participationFacade);
    }

    @Test
    void requireTaskAssignmentForumAccess_adminHiddenAssignment_shouldReturnUserId() {
        stubAuthenticatedHierarchy(HIDDEN, SCHEDULED);

        when(securityFacade.hasRole(ADMIN_ROLE))
            .thenReturn(true);

        Long result = questionAccessPolicy
            .requireTaskAssignmentForumAccess(
                TASK_ASSIGNMENT_ID);

        assertEquals(USER_ID, result);

        verify(competitionFacade).findTourById(TOUR_ID);
        verify(competitionFacade).findStageById(STAGE_ID);
        verifyNoInteractions(participationFacade);
    }

    @Test
    void requireTaskAssignmentForumAccess_orgWithoutParticipation_shouldThrowAccessRestrictedException() {
        stubAuthenticatedHierarchy(VISIBLE, SCHEDULED);

        /*
         * This mock represents a user who has ORG but not ADMIN. The policy asks only
         * whether ADMIN is present.
         */
        when(securityFacade.hasRole(anyString()))
            .thenAnswer(invocation -> ORG_ROLE.equals(invocation.getArgument(0)));

        when(participationFacade.isUserParticipant(
            USER_ID,
            COMPETITION_ID,
            STAGE_ID)).thenReturn(false);

        assertThrows(
            QuestionForumAccessRestrictedException.class,
            () -> questionAccessPolicy
                .requireTaskAssignmentForumAccess(
                    TASK_ASSIGNMENT_ID));

        verify(securityFacade).hasRole(ADMIN_ROLE);
        verify(participationFacade).isUserParticipant(
            USER_ID,
            COMPETITION_ID,
            STAGE_ID);
    }

    @Test
    void requireTaskAssignmentQuestionCreationAccess_inProgressTour_shouldReturnUserId() {
        stubParticipantAccess(VISIBLE, IN_PROGRESS);

        Long result = questionAccessPolicy
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
            () -> questionAccessPolicy
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
            () -> questionAccessPolicy
                .requireTaskAssignmentQuestionCreationAccess(
                    TASK_ASSIGNMENT_ID));

        verify(participationFacade).isUserParticipant(
            USER_ID,
            COMPETITION_ID,
            STAGE_ID);
    }

    private void stubParticipantAccess(
        AssignmentVisibility visibility,
        ExecutionStatus executionStatus) {
        stubAuthenticatedHierarchy(
            visibility,
            executionStatus);

        when(securityFacade.hasRole(ADMIN_ROLE))
            .thenReturn(false);

        when(participationFacade.isUserParticipant(
            USER_ID,
            COMPETITION_ID,
            STAGE_ID)).thenReturn(true);
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