package com.itasocialacademy.oitassist.competition.validate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import com.itasocialacademy.oitassist.competition.dao.model.Stage;
import com.itasocialacademy.oitassist.competition.dao.model.Tour;
import com.itasocialacademy.oitassist.competition.dao.repository.CompetitionRepository;
import com.itasocialacademy.oitassist.competition.dao.repository.StageRepository;
import com.itasocialacademy.oitassist.competition.dao.repository.TourRepository;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionHierarchyValidationException;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionNotFoundException;
import com.itasocialacademy.oitassist.competition.exceptions.StageNotFoundException;
import com.itasocialacademy.oitassist.competition.validation.HierarchyValidator;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @InjectMocks
    private HierarchyValidator validator;

    private Competition draftCompetition;
    private Competition publishedCompetition;

    @BeforeEach
    void setUp() {
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

    // ---- validateImmutabilityByCompetitionId ----

    @Test
    void validateImmutabilityByCompetitionId_notFound_shouldThrow() {
        when(competitionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CompetitionNotFoundException.class,
            () -> validator.validateImmutabilityByCompetitionId(99L));
    }

    @Test
    void validateImmutabilityByCompetitionId_whenArchived_shouldThrow() {
        draftCompetition.setCompetitionStatus(CompetitionStatus.ARCHIVED);
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(draftCompetition));

        CompetitionHierarchyValidationException exception = assertThrows(
            CompetitionHierarchyValidationException.class,
            () -> validator.validateImmutabilityByCompetitionId(1L));

        assertTrue(exception.getMessage().contains("is ARCHIVED (read-only)"));
    }

    @Test
    void validateImmutabilityByCompetitionId_whenDraft_shouldPass() {
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(draftCompetition));

        assertDoesNotThrow(() -> validator.validateImmutabilityByCompetitionId(1L));
    }

    @Test
    void validateImmutabilityByCompetitionId_whenPublished_shouldPass() {
        // hasActiveParticipations is currently hardcoded false (pending
        // ParticipationRequest integration)
        when(competitionRepository.findById(2L)).thenReturn(Optional.of(publishedCompetition));

        assertDoesNotThrow(() -> validator.validateImmutabilityByCompetitionId(2L));
    }

    @Test
    void validateImmutabilityByStageId_delegatesToParentCompetition() {
        Stage stage = Stage.builder().id(10L).competitionId(1L).build();
        draftCompetition.setCompetitionStatus(CompetitionStatus.ARCHIVED);
        when(stageRepository.findById(10L)).thenReturn(Optional.of(stage));
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(draftCompetition));

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
}