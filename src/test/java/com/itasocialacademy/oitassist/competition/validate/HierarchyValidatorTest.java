package com.itasocialacademy.oitassist.competition.validate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus;
import com.itasocialacademy.oitassist.competition.dao.enums.StageStatus;
import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import com.itasocialacademy.oitassist.competition.dao.model.Stage;
import com.itasocialacademy.oitassist.competition.dao.model.Tour;
import com.itasocialacademy.oitassist.competition.dao.repository.CompetitionRepository;
import com.itasocialacademy.oitassist.competition.dao.repository.StageRepository;
import com.itasocialacademy.oitassist.competition.dao.repository.TourRepository;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionHierarchyValidationException;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionNotFoundException;
import com.itasocialacademy.oitassist.competition.exceptions.StageNotFoundException;
import com.itasocialacademy.oitassist.competition.exceptions.StaleEntityVersionException;
import com.itasocialacademy.oitassist.competition.spi.ParticipationInquiryPort;
import com.itasocialacademy.oitassist.competition.validation.HierarchyValidator;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class HierarchyValidatorTest {

    @Mock
    private CompetitionRepository competitionRepository;
    @Mock
    private StageRepository stageRepository;
    @Mock
    private SecurityFacade securityFacade;
    @Mock
    private TourRepository tourRepository;
    @Mock
    private ParticipationInquiryPort participationSPI;
    @Mock
    private EntityManager entityManager;
    @Mock
    private Query nativeQuery;

    @InjectMocks
    private HierarchyValidator validator;

    private Competition draftCompetition;
    private Competition publishedCompetition;

    @BeforeEach
    void setUp() {
        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);

        ZonedDateTime start = ZonedDateTime.of(2026, 6, 25, 10, 0, 0, 0, ZoneId.of("UTC"));

        draftCompetition = Competition.builder()
            .id(1L)
            .competitionStatus(CompetitionStatus.DRAFT)
            .dateStart(start)
            .dateFinish(start.plusDays(10))
            .build();

        publishedCompetition = Competition.builder()
            .id(2L)
            .competitionStatus(CompetitionStatus.PUBLISHED)
            .dateStart(start)
            .dateFinish(start.plusDays(10))
            .build();
    }

    // ---- checkVisibilityAccess ----

    @Test
    void checkVisibilityAccess_notFound_shouldThrow() {
        when(competitionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CompetitionNotFoundException.class, () -> validator.checkVisibilityAccess(99L));
    }

    @Test
    void checkVisibilityAccess_draftAndAdmin_shouldPass() {
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(draftCompetition));
        when(securityFacade.hasRole("ADMIN")).thenReturn(true);

        assertDoesNotThrow(() -> validator.checkVisibilityAccess(1L));
    }

    @Test
    void checkVisibilityAccess_draftAndOrg_shouldPass() {
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(draftCompetition));
        when(securityFacade.hasRole("ADMIN")).thenReturn(false);
        when(securityFacade.hasRole("ORG")).thenReturn(true);

        assertDoesNotThrow(() -> validator.checkVisibilityAccess(1L));
    }

    @Test
    void checkVisibilityAccess_draftAndRegularUser_shouldThrowAccessDenied() {
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(draftCompetition));
        when(securityFacade.hasRole("ADMIN")).thenReturn(false);
        when(securityFacade.hasRole("ORG")).thenReturn(false);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
            () -> validator.checkVisibilityAccess(1L));

        assertTrue(exception.getMessage().contains("permission to view this draft"));
    }

    @Test
    void checkVisibilityAccess_publishedCompetition_shouldPassRegardlessOfRole() {
        when(competitionRepository.findById(2L)).thenReturn(Optional.of(publishedCompetition));

        assertDoesNotThrow(() -> validator.checkVisibilityAccess(2L));
    }

    @Test
    void checkVisibilityAccessByStageId_delegatesToParentCompetition() {
        Stage stage = Stage.builder().id(10L).competitionId(2L).build();
        when(stageRepository.findById(10L)).thenReturn(Optional.of(stage));
        when(competitionRepository.findById(2L)).thenReturn(Optional.of(publishedCompetition));

        assertDoesNotThrow(() -> validator.checkVisibilityAccessByStageId(10L));
    }

    @Test
    void checkVisibilityAccessByStageId_stageNotFound_shouldThrow() {
        when(stageRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(StageNotFoundException.class, () -> validator.checkVisibilityAccessByStageId(999L));
    }

    // ---- lockCompetitionForUpdate ----

    @Test
    void lockCompetitionForUpdate_found_shouldReturnCompetition() {
        when(competitionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(draftCompetition));

        Competition result = validator.lockCompetitionForUpdate(1L);

        assertEquals(draftCompetition, result);
    }

    @Test
    void lockCompetitionForUpdate_notFound_shouldThrow() {
        when(competitionRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(CompetitionNotFoundException.class, () -> validator.lockCompetitionForUpdate(99L));
    }

    // ---- validateImmutabilityByCompetitionId ----

    @Test
    void validateImmutabilityByCompetitionId_notFound_shouldThrow() {
        when(competitionRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(CompetitionNotFoundException.class,
            () -> validator.validateImmutabilityByCompetitionId(99L));
    }

    @Test
    void validateImmutabilityByCompetitionId_whenArchived_shouldThrow() {
        draftCompetition.setCompetitionStatus(CompetitionStatus.ARCHIVED);
        when(competitionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(draftCompetition));

        CompetitionHierarchyValidationException exception = assertThrows(
            CompetitionHierarchyValidationException.class,
            () -> validator.validateImmutabilityByCompetitionId(1L));

        assertTrue(exception.getMessage().contains("is ARCHIVED (read-only)"));
    }

    @Test
    void validateImmutabilityByCompetitionId_whenDraft_shouldPass() {
        when(competitionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(draftCompetition));

        assertDoesNotThrow(() -> validator.validateImmutabilityByCompetitionId(1L));
    }

    @Test
    void validateImmutabilityByCompetitionId_whenEnrollment_shouldPass() {
        draftCompetition.setCompetitionStatus(CompetitionStatus.ENROLLMENT);
        when(competitionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(draftCompetition));

        assertDoesNotThrow(() -> validator.validateImmutabilityByCompetitionId(1L));
    }

    @Test
    void validateImmutabilityByCompetitionId_whenPublished_shouldPass() {
        when(competitionRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(publishedCompetition));

        assertDoesNotThrow(() -> validator.validateImmutabilityByCompetitionId(2L));
    }

    @Test
    void validateImmutabilityByCompetitionId_whenEnrollmentAndHasActiveParticipants_shouldThrow() {
        draftCompetition.setCompetitionStatus(CompetitionStatus.ENROLLMENT);
        when(competitionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(draftCompetition));
        when(participationSPI.competitionHasParticipants(anyLong())).thenReturn(true);

        assertThrows(CompetitionHierarchyValidationException.class,
            () -> validator.validateImmutabilityByCompetitionId(1L));
    }

    @Test
    void validateImmutabilityByCompetitionId_whenPublishedHasActiveParticipants_shouldThrow() {
        when(competitionRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(publishedCompetition));
        when(participationSPI.competitionHasParticipants(anyLong())).thenReturn(true);

        assertThrows(CompetitionHierarchyValidationException.class,
            () -> validator.validateImmutabilityByCompetitionId(2L));
    }

    @Test
    void validateImmutabilityByStageId_delegatesToParentCompetition() {
        Stage stage = Stage.builder().id(10L).competitionId(1L).build();
        draftCompetition.setCompetitionStatus(CompetitionStatus.ARCHIVED);
        when(stageRepository.findById(10L)).thenReturn(Optional.of(stage));
        when(competitionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(draftCompetition));

        assertThrows(CompetitionHierarchyValidationException.class,
            () -> validator.validateImmutabilityByStageId(10L));
    }

    // ---- eligibility checks ----

    @Test
    void validateStageEligibility_matchingCompetition_shouldPass() {
        assertDoesNotThrow(() -> validator.validateStageEligibility(1L, 1L));
    }

    @Test
    void validateStageEligibility_mismatchedCompetition_shouldThrow() {
        assertThrows(CompetitionHierarchyValidationException.class,
            () -> validator.validateStageEligibility(1L, 2L));
    }

    @Test
    void validateTourEligibility_matchingStage_shouldPass() {
        assertDoesNotThrow(() -> validator.validateTourEligibility(1L, 1L));
    }

    @Test
    void validateTourEligibility_mismatchedStage_shouldThrow() {
        assertThrows(CompetitionHierarchyValidationException.class,
            () -> validator.validateTourEligibility(1L, 2L));
    }

    // ---- date-range checks ----

    @Test
    void validateStageDates_withinCompetitionRange_shouldPass() {
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(draftCompetition));

        ZonedDateTime start = draftCompetition.getDateStart().plusDays(1);
        ZonedDateTime finish = draftCompetition.getDateFinish().minusDays(1);

        assertDoesNotThrow(() -> validator.validateStageDates(1L, start, finish));
    }

    @Test
    void validateStageDates_startBeforeCompetitionStart_shouldThrow() {
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(draftCompetition));

        ZonedDateTime start = draftCompetition.getDateStart().minusDays(1);
        ZonedDateTime finish = draftCompetition.getDateFinish().minusDays(1);

        assertThrows(CompetitionHierarchyValidationException.class,
            () -> validator.validateStageDates(1L, start, finish));
    }

    @Test
    void validateStageDates_finishAfterCompetitionFinish_shouldThrow() {
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(draftCompetition));

        ZonedDateTime start = draftCompetition.getDateStart().plusDays(1);
        ZonedDateTime finish = draftCompetition.getDateFinish().plusDays(1);

        assertThrows(CompetitionHierarchyValidationException.class,
            () -> validator.validateStageDates(1L, start, finish));
    }

    @Test
    void validateStageDates_competitionNotFound_shouldThrow() {
        when(competitionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CompetitionNotFoundException.class,
            () -> validator.validateStageDates(99L, ZonedDateTime.now(), ZonedDateTime.now().plusDays(1)));
    }

    @Test
    void validateTourDates_withinStageRange_shouldPass() {
        Stage stage = Stage.builder()
            .id(10L)
            .dateStart(draftCompetition.getDateStart())
            .dateFinish(draftCompetition.getDateFinish())
            .build();
        when(stageRepository.findById(10L)).thenReturn(Optional.of(stage));

        ZonedDateTime start = stage.getDateStart().plusHours(1);
        ZonedDateTime finish = stage.getDateFinish().minusHours(1);

        assertDoesNotThrow(() -> validator.validateTourDates(10L, start, finish));
    }

    @Test
    void validateTourDates_outsideStageRange_shouldThrow() {
        Stage stage = Stage.builder()
            .id(10L)
            .dateStart(draftCompetition.getDateStart())
            .dateFinish(draftCompetition.getDateFinish())
            .build();
        when(stageRepository.findById(10L)).thenReturn(Optional.of(stage));

        ZonedDateTime start = stage.getDateStart().minusHours(1);
        ZonedDateTime finish = stage.getDateFinish();

        assertThrows(CompetitionHierarchyValidationException.class,
            () -> validator.validateTourDates(10L, start, finish));
    }

    @Test
    void validateTourDates_stageNotFound_shouldThrow() {
        when(stageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(StageNotFoundException.class,
            () -> validator.validateTourDates(99L, ZonedDateTime.now(), ZonedDateTime.now().plusHours(1)));
    }

    @Test
    void validateStageDatesAgainstExistingTours_allToursWithinNewRange_shouldPass() {
        ZonedDateTime newStart = draftCompetition.getDateStart();
        ZonedDateTime newFinish = draftCompetition.getDateFinish();

        Tour tour = Tour.builder()
            .title("Tour 1")
            .dateStart(newStart.plusHours(1))
            .dateFinish(newFinish.minusHours(1))
            .build();

        when(tourRepository.findAllByStageIdOrderBySortPositionAsc(10L)).thenReturn(List.of(tour));

        assertDoesNotThrow(() -> validator.validateStageDatesAgainstExistingTours(10L, newStart, newFinish));
    }

    @Test
    void validateStageDatesAgainstExistingTours_tourStartsBeforeNewStart_shouldThrow() {
        ZonedDateTime newStart = draftCompetition.getDateStart().plusDays(2);
        ZonedDateTime newFinish = draftCompetition.getDateFinish();

        Tour orphanedTour = Tour.builder()
            .title("Early Tour")
            .dateStart(draftCompetition.getDateStart())
            .dateFinish(newStart.plusHours(1))
            .build();

        when(tourRepository.findAllByStageIdOrderBySortPositionAsc(10L)).thenReturn(List.of(orphanedTour));

        CompetitionHierarchyValidationException exception = assertThrows(
            CompetitionHierarchyValidationException.class,
            () -> validator.validateStageDatesAgainstExistingTours(10L, newStart, newFinish));

        assertTrue(exception.getMessage().contains("Early Tour"));
    }

    @Test
    void validateStageDatesAgainstExistingTours_tourFinishesAfterNewFinish_shouldThrow() {
        ZonedDateTime newStart = draftCompetition.getDateStart();
        ZonedDateTime newFinish = draftCompetition.getDateFinish().minusDays(2);

        Tour orphanedTour = Tour.builder()
            .title("Late Tour")
            .dateStart(newFinish.minusHours(1))
            .dateFinish(draftCompetition.getDateFinish())
            .build();

        when(tourRepository.findAllByStageIdOrderBySortPositionAsc(10L)).thenReturn(List.of(orphanedTour));

        assertThrows(CompetitionHierarchyValidationException.class,
            () -> validator.validateStageDatesAgainstExistingTours(10L, newStart, newFinish));
    }

    @Test
    void validateStageDatesAgainstExistingTours_multipleViolations_listsAllTitles() {
        ZonedDateTime newStart = draftCompetition.getDateStart().plusDays(3);
        ZonedDateTime newFinish = draftCompetition.getDateFinish().minusDays(3);

        Tour early = Tour.builder().title("Early Tour")
            .dateStart(draftCompetition.getDateStart()).dateFinish(newStart.plusHours(1)).build();
        Tour late = Tour.builder().title("Late Tour")
            .dateStart(newFinish.minusHours(1)).dateFinish(draftCompetition.getDateFinish()).build();

        when(tourRepository.findAllByStageIdOrderBySortPositionAsc(10L)).thenReturn(List.of(early, late));

        CompetitionHierarchyValidationException exception = assertThrows(
            CompetitionHierarchyValidationException.class,
            () -> validator.validateStageDatesAgainstExistingTours(10L, newStart, newFinish));

        assertTrue(exception.getMessage().contains("Early Tour"));
        assertTrue(exception.getMessage().contains("Late Tour"));
    }

    @Test
    void validateStageDatesAgainstExistingTours_noTours_shouldPass() {
        when(tourRepository.findAllByStageIdOrderBySortPositionAsc(10L)).thenReturn(List.of());

        assertDoesNotThrow(() -> validator.validateStageDatesAgainstExistingTours(10L, draftCompetition.getDateStart(),
            draftCompetition.getDateFinish()));
    }

    // ---- Execution Readiness (Draft/Published) ----

    @Test
    void checkIfCompetitionPublishedByCompetitionId_whenPublished_shouldPass() {
        when(competitionRepository.findById(2L)).thenReturn(Optional.of(publishedCompetition));
        assertDoesNotThrow(() -> validator.checkIfCompetitionPublishedByCompetitionId(2L));
    }

    @Test
    void checkIfCompetitionPublishedByCompetitionId_whenDraft_shouldThrow() {
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(draftCompetition));
        CompetitionHierarchyValidationException exception = assertThrows(
            CompetitionHierarchyValidationException.class,
            () -> validator.checkIfCompetitionPublishedByCompetitionId(1L));
        assertTrue(exception.getMessage().contains("Competition must be PUBLISHED"));
    }

    @Test
    void checkIfStageInProgress_targetIsCancelled_shouldPassRegardlessOfStageStatus() {
        Stage stage = Stage.builder().id(10L).status(StageStatus.SCHEDULED).build();
        when(stageRepository.findById(10L)).thenReturn(Optional.of(stage));

        assertDoesNotThrow(() -> validator.checkIfStageInProgress(10L, ExecutionStatus.CANCELLED));
    }

    @Test
    void checkIfStageInProgress_targetIsInProgressAndStageIsInProgress_shouldPass() {
        Stage stage = Stage.builder().id(10L).status(StageStatus.IN_PROGRESS).build();
        when(stageRepository.findById(10L)).thenReturn(Optional.of(stage));

        assertDoesNotThrow(() -> validator.checkIfStageInProgress(10L, ExecutionStatus.IN_PROGRESS));
    }

    @Test
    void checkIfStageInProgress_targetIsInProgressAndStageIsScheduled_shouldThrow() {
        Stage stage = Stage.builder().id(10L).status(StageStatus.SCHEDULED).build();
        when(stageRepository.findById(10L)).thenReturn(Optional.of(stage));

        CompetitionHierarchyValidationException exception = assertThrows(
            CompetitionHierarchyValidationException.class,
            () -> validator.checkIfStageInProgress(10L, ExecutionStatus.IN_PROGRESS));
        assertTrue(exception.getMessage().contains("Stage must be IN_PROGRESS"));
    }

    // ---- State Machine Transitions ----

    @ParameterizedTest
    @CsvSource({
        "SCHEDULED, IN_PROGRESS",
        "SCHEDULED, CANCELLED",
        "IN_PROGRESS, CLOSED",
        "IN_PROGRESS, FINISHED",
        "CLOSED, IN_PROGRESS",
        "CLOSED, FINISHED"
    })
    void validateTourStatusTransition_validTransitions_shouldPass(ExecutionStatus current, ExecutionStatus target) {
        assertDoesNotThrow(() -> validator.validateTourStatusTransition(current, target));
    }

    @ParameterizedTest
    @CsvSource({
        "SCHEDULED, FINISHED",
        "CLOSED, SCHEDULED",
        "FINISHED, IN_PROGRESS",
        "CANCELLED, IN_PROGRESS"
    })
    void validateTourStatusTransition_invalidTransitions_shouldThrow(ExecutionStatus current, ExecutionStatus target) {
        assertThrows(CompetitionHierarchyValidationException.class,
            () -> validator.validateTourStatusTransition(current, target));
    }

    @ParameterizedTest
    @CsvSource({
        "SCHEDULED, IN_PROGRESS",
        "IN_PROGRESS, FINISHED",
        "SCHEDULED, CANCELLED"
    })
    void validateStageStatusTransition_validTransitions_shouldPass(StageStatus current, StageStatus target) {
        assertDoesNotThrow(() -> validator.validateStageStatusTransition(current, target));
    }

    @ParameterizedTest
    @CsvSource({
        "DRAFT, ENROLLMENT",
        "ENROLLMENT, PUBLISHED",
        "PUBLISHED, FINISHED",
        "FINISHED, ARCHIVED"
    })
    void validateCompetitionStatusTransition_validTransitions_shouldPass(CompetitionStatus current,
        CompetitionStatus target) {
        assertDoesNotThrow(() -> validator.validateCompetitionStatusTransition(current, target));
    }

    // ---- Eligibility To Start (Sequence Logic) ----

    @Test
    void validateTourEligibilityToStart_firstTour_shouldPass() {
        Tour tour = Tour.builder().stageId(10L).sortPosition((short) 1).build();

        when(tourRepository.findFirstByStageIdAndSortPositionLessThanOrderBySortPositionDesc(10L, (short) 1))
            .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> validator.validateTourEligibilityToStart(tour));
    }

    @Test
    void validateTourEligibilityToStart_previousTourFinished_shouldPass() {
        Tour currentTour = Tour.builder().stageId(10L).sortPosition((short) 2).build();
        Tour prevTour = Tour.builder().executionStatus(ExecutionStatus.FINISHED).build();

        when(tourRepository.findFirstByStageIdAndSortPositionLessThanOrderBySortPositionDesc(10L, (short) 2))
            .thenReturn(Optional.of(prevTour));

        assertDoesNotThrow(() -> validator.validateTourEligibilityToStart(currentTour));
    }

    @Test
    void validateTourEligibilityToStart_previousTourNotFinished_shouldThrow() {
        Tour currentTour = Tour.builder().stageId(10L).sortPosition((short) 2).title("Tour 2").build();
        Tour prevTour = Tour.builder().executionStatus(ExecutionStatus.CLOSED).title("Tour 1").build();

        when(tourRepository.findFirstByStageIdAndSortPositionLessThanOrderBySortPositionDesc(10L, (short) 2))
            .thenReturn(Optional.of(prevTour));

        CompetitionHierarchyValidationException exception = assertThrows(
            CompetitionHierarchyValidationException.class,
            () -> validator.validateTourEligibilityToStart(currentTour));
        assertTrue(exception.getMessage().contains("The previous tour 'Tour 1' is not yet FINISHED"));
    }

    @Test
    void validateStageEligibilityToStart_firstStage_shouldPass() {
        Stage stage = Stage.builder().competitionId(5L).sortPosition((short) 1).build();

        when(stageRepository.findFirstByCompetitionIdAndSortPositionLessThanOrderBySortPositionDesc(5L, (short) 1))
            .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> validator.validateStageEligibilityToStart(stage));
    }

    @Test
    void validateStageEligibilityToStart_previousStageNotFinished_shouldThrow() {
        Stage currentStage = Stage.builder().competitionId(5L).sortPosition((short) 2).title("Stage 2").build();
        Stage prevStage = Stage.builder().status(StageStatus.IN_PROGRESS).title("Stage 1").build();

        when(stageRepository.findFirstByCompetitionIdAndSortPositionLessThanOrderBySortPositionDesc(5L, (short) 2))
            .thenReturn(Optional.of(prevStage));

        assertThrows(CompetitionHierarchyValidationException.class,
            () -> validator.validateStageEligibilityToStart(currentStage));
    }

    // ---- Eligibility To Resume & Finish ----

    @Test
    void validateTourEligibilityToResume_dateInFuture_shouldPass() {
        Tour tour = Tour.builder().dateFinish(ZonedDateTime.now().plusHours(1)).build();
        assertDoesNotThrow(() -> validator.validateTourEligibilityToResume(tour));
    }

    @Test
    void validateTourEligibilityToResume_dateInPast_shouldThrow() {
        Tour tour = Tour.builder().title("Expired Tour").dateFinish(ZonedDateTime.now().minusHours(1)).build();

        CompetitionHierarchyValidationException exception = assertThrows(
            CompetitionHierarchyValidationException.class,
            () -> validator.validateTourEligibilityToResume(tour));
        assertTrue(exception.getMessage().contains("The finish date"));
    }

    @Test
    void validateAllStagesCompletedForCompetition_allTerminal_shouldNotThrow() {
        Stage finishedStage = Stage.builder().id(1L).title("Stage 1").status(StageStatus.FINISHED).build();
        Stage cancelledStage = Stage.builder().id(2L).title("Stage 2").status(StageStatus.CANCELLED).build();

        when(stageRepository.findAllByCompetitionIdOrderBySortPositionAsc(1L))
            .thenReturn(List.of(finishedStage, cancelledStage));

        assertDoesNotThrow(() -> validator.validateAllStagesCompletedForCompetition(1L));
    }

    @Test
    void validateAllStagesCompletedForCompetition_someScheduled_shouldThrow() {
        Stage finishedStage = Stage.builder().id(1L).title("Stage 1").status(StageStatus.FINISHED).build();
        Stage scheduledStage = Stage.builder().id(2L).title("Stage 2").status(StageStatus.SCHEDULED).build();

        when(stageRepository.findAllByCompetitionIdOrderBySortPositionAsc(1L))
            .thenReturn(List.of(finishedStage, scheduledStage));

        CompetitionHierarchyValidationException exception = assertThrows(
            CompetitionHierarchyValidationException.class,
            () -> validator.validateAllStagesCompletedForCompetition(1L));

        assertTrue(exception.getMessage().contains("Stage 2"));
    }

    @Test
    void validateAllStagesCompletedForCompetition_noStages_shouldThrow() {
        when(stageRepository.findAllByCompetitionIdOrderBySortPositionAsc(1L))
            .thenReturn(List.of());

        CompetitionHierarchyValidationException exception = assertThrows(
            CompetitionHierarchyValidationException.class,
            () -> validator.validateAllStagesCompletedForCompetition(1L));

        assertTrue(exception.getMessage().contains("must have at least one stage"));
    }

    @Test
    void validateAllToursCompletedForStage_allFinishedOrCancelled_shouldPass() {
        Tour t1 = Tour.builder().executionStatus(ExecutionStatus.FINISHED).build();
        Tour t2 = Tour.builder().executionStatus(ExecutionStatus.CANCELLED).build();

        when(tourRepository.findAllByStageIdOrderBySortPositionAsc(10L)).thenReturn(List.of(t1, t2));

        assertDoesNotThrow(() -> validator.validateAllToursCompletedForStage(10L));
    }

    @Test
    void validateAllToursCompletedForStage_oneTourInProgress_shouldThrow() {
        Tour t1 = Tour.builder().title("Tour 1").executionStatus(ExecutionStatus.FINISHED).build();
        Tour t2 = Tour.builder().title("Tour 2").executionStatus(ExecutionStatus.IN_PROGRESS).build();

        when(tourRepository.findAllByStageIdOrderBySortPositionAsc(10L)).thenReturn(List.of(t1, t2));

        CompetitionHierarchyValidationException exception = assertThrows(
            CompetitionHierarchyValidationException.class,
            () -> validator.validateAllToursCompletedForStage(10L));
        assertTrue(exception.getMessage().contains("Not all tours are completed"));
        assertTrue(exception.getMessage().contains("Tour 2"));
    }

    @Test
    void validateAllToursCompletedForStage_noTours_shouldThrow() {
        when(tourRepository.findAllByStageIdOrderBySortPositionAsc(10L)).thenReturn(List.of());

        CompetitionHierarchyValidationException exception = assertThrows(
            CompetitionHierarchyValidationException.class,
            () -> validator.validateAllToursCompletedForStage(10L));

        assertTrue(exception.getMessage().contains("must contain at least one tour"));
    }

    // ---- validateToursNotStartedByStageId ----

    @Test
    void validateToursNotStartedByStageId_allScheduled_shouldPass() {
        Tour t1 = Tour.builder().id(1L).executionStatus(ExecutionStatus.SCHEDULED).build();
        Tour t2 = Tour.builder().id(2L).executionStatus(ExecutionStatus.SCHEDULED).build();

        when(stageRepository.existsById(anyLong())).thenReturn(true);
        when(tourRepository.findAllByStageIdOrderBySortPositionAsc(10L)).thenReturn(List.of(t1, t2));

        assertDoesNotThrow(() -> validator.validateToursNotStartedByStageId(10L));
    }

    @Test
    void validateToursNotStartedByStageId_oneInProgress_shouldThrow() {
        Tour t1 = Tour.builder().id(1L).executionStatus(ExecutionStatus.SCHEDULED).build();
        Tour t2 = Tour.builder().id(2L).executionStatus(ExecutionStatus.IN_PROGRESS).build();

        when(stageRepository.existsById(anyLong())).thenReturn(true);
        when(tourRepository.findAllByStageIdOrderBySortPositionAsc(10L)).thenReturn(List.of(t1, t2));

        CompetitionHierarchyValidationException exception = assertThrows(
            CompetitionHierarchyValidationException.class,
            () -> validator.validateToursNotStartedByStageId(10L));

        assertTrue(exception.getMessage().contains("already started execution"));
    }

    @Test
    void validateToursNotStartedByStageId_oneFinished_shouldThrow() {
        Tour t1 = Tour.builder().id(1L).executionStatus(ExecutionStatus.SCHEDULED).build();
        Tour t2 = Tour.builder().id(2L).executionStatus(ExecutionStatus.FINISHED).build();

        when(stageRepository.existsById(anyLong())).thenReturn(true);
        when(tourRepository.findAllByStageIdOrderBySortPositionAsc(10L)).thenReturn(List.of(t1, t2));

        assertThrows(CompetitionHierarchyValidationException.class,
            () -> validator.validateToursNotStartedByStageId(10L));
    }

    @Test
    void validateToursNotStartedByStageId_oneCancelled_shouldThrow() {
        Tour t1 = Tour.builder().id(1L).executionStatus(ExecutionStatus.SCHEDULED).build();
        Tour t2 = Tour.builder().id(2L).executionStatus(ExecutionStatus.CANCELLED).build();

        when(stageRepository.existsById(anyLong())).thenReturn(true);
        when(tourRepository.findAllByStageIdOrderBySortPositionAsc(10L)).thenReturn(List.of(t1, t2));

        assertThrows(CompetitionHierarchyValidationException.class,
            () -> validator.validateToursNotStartedByStageId(10L));
    }

    @Test
    void validateToursNotStartedByStageId_noTours_shouldPass() {
        when(stageRepository.existsById(anyLong())).thenReturn(true);
        when(tourRepository.findAllByStageIdOrderBySortPositionAsc(10L)).thenReturn(List.of());

        assertDoesNotThrow(() -> validator.validateToursNotStartedByStageId(10L));
    }

    @Test
    void validateToursNotStartedByStageId_noStage_shouldThrow() {
        when(stageRepository.existsById(anyLong())).thenReturn(false);

        assertThrows(StageNotFoundException.class, () -> validator.validateToursNotStartedByStageId(10L));
    }

    // ---- validateEntityVersion ----

    @Test
    void validateEntityVersion_matchingVersions_shouldNotThrow() {
        assertDoesNotThrow(() -> validator.validateEntityVersion(1L, 1L, Competition.class, 1L));
    }

    @Test
    void validateEntityVersion_mismatchedVersions_shouldThrowStaleEntityVersionException() {
        assertThrows(StaleEntityVersionException.class,
            () -> validator.validateEntityVersion(5L, 1L, Competition.class, 1L));
    }

    @Test
    void validateEntityVersion_nullActualVersion_shouldThrow() {
        assertThrows(StaleEntityVersionException.class,
            () -> validator.validateEntityVersion(1L, null, Competition.class, 1L));
    }
}