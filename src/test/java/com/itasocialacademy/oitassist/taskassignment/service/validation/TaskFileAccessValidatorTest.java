package com.itasocialacademy.oitassist.taskassignment.service.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.competition.api.dto.TourDetail;
import com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.participation.api.ParticipationFacade;
import com.itasocialacademy.oitassist.task.api.TaskBodyFacade;
import com.itasocialacademy.oitassist.task.api.dto.TaskBodyDetail;
import com.itasocialacademy.oitassist.taskassignment.dao.repository.TaskAssignmentRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskFileAccessValidatorTest {
    private static final Long TASK_ID = 10L;
    private static final Long USER_ID = 5L;
    private static final Long STAGE_ID = 7L;
    private static final Long TOUR_ID = 3L;

    private static final Predicate<String> ADMIN = role -> role.equals("ADMIN");
    private static final Predicate<String> ORG = role -> role.equals("ORG");
    private static final Predicate<String> USER = role -> role.equals("USER");

    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;
    @Mock
    private TaskBodyFacade taskBodyFacade;
    @Mock
    private CompetitionFacade competitionFacade;
    @Mock
    private ParticipationFacade participationFacade;

    @InjectMocks
    private TaskFileAccessValidator validator;

    private static TourDetail tour(Long id, Long stageId, ExecutionStatus status) {
        return TourDetail.builder().id(id).stageId(stageId).executionStatus(status).build();
    }

    private static TaskBodyDetail task(Set<Long> ownerIds) {
        return TaskBodyDetail.builder().id(TASK_ID).title("t").ownerIds(ownerIds).build();
    }

    private void givenVisibleTour(ExecutionStatus status) {
        when(taskAssignmentRepository.findVisibleTourIdsByTaskBodyId(TASK_ID)).thenReturn(List.of(TOUR_ID));
        when(competitionFacade.findToursByIds(List.of(TOUR_ID))).thenReturn(List.of(tour(TOUR_ID, STAGE_ID, status)));
    }

    @Test
    void getEntityType_ShouldReturnTask() {
        assertEquals(RelatedEntityType.TASK, validator.getEntityType());
    }

    // --- guests ---

    @Test
    void canAccess_ShouldDeny_WhenUserIsNull() {
        assertFalse(validator.canAccess(TASK_ID, FileRole.PROBLEM, null, USER));

        verifyNoInteractions(taskAssignmentRepository, taskBodyFacade, competitionFacade, participationFacade);
    }

    // --- ADMIN ---

    @ParameterizedTest
    @EnumSource(value = FileRole.class, names = {"PROBLEM", "REFERENCE", "SOLUTION"})
    void canAccess_ShouldAllowAdmin_ForEveryTaskFileRole(FileRole role) {
        assertTrue(validator.canAccess(TASK_ID, role, USER_ID, ADMIN));

        verifyNoInteractions(taskAssignmentRepository, competitionFacade, participationFacade);
    }

    // --- ORG ---

    @Test
    void canAccess_ShouldAllowOrg_WhenOwnerOfTask() {
        when(taskBodyFacade.findTaskBodyById(TASK_ID)).thenReturn(Optional.of(task(Set.of(USER_ID))));

        assertTrue(validator.canAccess(TASK_ID, FileRole.SOLUTION, USER_ID, ORG));
    }

    @Test
    void canAccess_ShouldDenyOrg_WhenNotOwnerAndNotParticipant() {
        when(taskBodyFacade.findTaskBodyById(TASK_ID)).thenReturn(Optional.of(task(Set.of(99L))));
        when(taskAssignmentRepository.findVisibleTourIdsByTaskBodyId(TASK_ID)).thenReturn(List.of());

        assertFalse(validator.canAccess(TASK_ID, FileRole.PROBLEM, USER_ID, ORG));
    }

    @Test
    void canAccess_ShouldDenyOrg_WhenTaskDoesNotExist() {
        when(taskBodyFacade.findTaskBodyById(TASK_ID)).thenReturn(Optional.empty());
        when(taskAssignmentRepository.findVisibleTourIdsByTaskBodyId(TASK_ID)).thenReturn(List.of());

        assertFalse(validator.canAccess(TASK_ID, FileRole.PROBLEM, USER_ID, ORG));
    }

    // --- participants ---

    @ParameterizedTest
    @EnumSource(value = FileRole.class, names = {"PROBLEM", "REFERENCE"})
    void canAccess_ShouldAllowParticipant_WhenTourInProgressAndRoleIsPublic(FileRole role) {
        givenVisibleTour(ExecutionStatus.IN_PROGRESS);
        when(participationFacade.isUserAStageParticipant(USER_ID, STAGE_ID)).thenReturn(true);

        assertTrue(validator.canAccess(TASK_ID, role, USER_ID, USER));
    }

    @ParameterizedTest
    @EnumSource(value = ExecutionStatus.class, names = {"SCHEDULED", "CLOSED", "FINISHED", "CANCELLED"})
    void canAccess_ShouldDenyParticipant_WhenTourIsNotInProgress(ExecutionStatus status) {
        givenVisibleTour(status);

        assertFalse(validator.canAccess(TASK_ID, FileRole.PROBLEM, USER_ID, USER));

        verify(participationFacade, never()).isUserAStageParticipant(USER_ID, STAGE_ID);
    }

    @Test
    void canAccess_ShouldDenyParticipant_WhenFileIsSolution() {
        assertFalse(validator.canAccess(TASK_ID, FileRole.SOLUTION, USER_ID, USER));

        verifyNoInteractions(taskAssignmentRepository, competitionFacade, participationFacade);
    }

    @Test
    void canAccess_ShouldDenyParticipant_WhenFileRoleIsGeneric() {
        assertFalse(validator.canAccess(TASK_ID, FileRole.GENERIC, USER_ID, USER));

        verifyNoInteractions(taskAssignmentRepository, competitionFacade, participationFacade);
    }

    @Test
    void canAccess_ShouldDenyParticipant_WhenFileRoleIsUnknown() {
        assertFalse(validator.canAccess(TASK_ID, null, USER_ID, USER));

        verifyNoInteractions(taskAssignmentRepository, competitionFacade, participationFacade);
    }

    @Test
    void canAccess_ShouldDenyParticipant_WhenTaskHasNoVisibleAssignment() {
        when(taskAssignmentRepository.findVisibleTourIdsByTaskBodyId(TASK_ID)).thenReturn(List.of());

        assertFalse(validator.canAccess(TASK_ID, FileRole.PROBLEM, USER_ID, USER));

        verify(competitionFacade, never()).findToursByIds(anyList());
    }

    @Test
    void canAccess_ShouldDenyParticipant_WhenNotParticipantOfStage() {
        givenVisibleTour(ExecutionStatus.IN_PROGRESS);
        when(participationFacade.isUserAStageParticipant(USER_ID, STAGE_ID)).thenReturn(false);

        assertFalse(validator.canAccess(TASK_ID, FileRole.PROBLEM, USER_ID, USER));
    }

    @Test
    void canAccess_ShouldAllowParticipant_WhenOneOfSeveralToursQualifies() {
        Long closedTourId = 4L;
        Long otherStageId = 8L;
        when(taskAssignmentRepository.findVisibleTourIdsByTaskBodyId(TASK_ID))
            .thenReturn(List.of(closedTourId, TOUR_ID));
        when(competitionFacade.findToursByIds(List.of(closedTourId, TOUR_ID))).thenReturn(List.of(
            tour(closedTourId, otherStageId, ExecutionStatus.CLOSED),
            tour(TOUR_ID, STAGE_ID, ExecutionStatus.IN_PROGRESS)));
        when(participationFacade.isUserAStageParticipant(USER_ID, STAGE_ID)).thenReturn(true);

        assertTrue(validator.canAccess(TASK_ID, FileRole.REFERENCE, USER_ID, USER));
    }

    @Test
    void canAccess_ShouldDenyParticipant_WhenParticipatesOnlyInClosedTourStage() {
        Long closedTourId = 4L;
        Long closedStageId = 8L;
        when(taskAssignmentRepository.findVisibleTourIdsByTaskBodyId(TASK_ID))
            .thenReturn(List.of(closedTourId, TOUR_ID));
        when(competitionFacade.findToursByIds(List.of(closedTourId, TOUR_ID))).thenReturn(List.of(
            tour(closedTourId, closedStageId, ExecutionStatus.CLOSED),
            tour(TOUR_ID, STAGE_ID, ExecutionStatus.IN_PROGRESS)));
        when(participationFacade.isUserAStageParticipant(USER_ID, STAGE_ID)).thenReturn(false);

        assertFalse(validator.canAccess(TASK_ID, FileRole.PROBLEM, USER_ID, USER));

        verify(participationFacade, never()).isUserAStageParticipant(USER_ID, closedStageId);
    }

    // --- three-argument variant (role unknown) ---

    @Test
    void canAccess_ThreeArgs_ShouldAllowAdmin() {
        assertTrue(validator.canAccess(TASK_ID, USER_ID, ADMIN));
    }

    @Test
    void canAccess_ThreeArgs_ShouldDenyParticipant_BecauseRoleIsUnknown() {
        assertFalse(validator.canAccess(TASK_ID, USER_ID, USER));
    }
}
