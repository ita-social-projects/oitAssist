package com.itasocialacademy.oitassist.competition.validation;

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
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class HierarchyValidator {
    private final CompetitionRepository competitionRepository;
    private final StageRepository stageRepository;
    private final TourRepository tourRepository;
    private final SecurityFacade securityFacade;
    private final ParticipationInquiryPort participationInquiryPort;

    @Transactional(readOnly = true)
    public void checkVisibilityAccess(Long competitionId) {
        Competition competition = competitionRepository.findById(competitionId)
            .orElseThrow(() -> new CompetitionNotFoundException(competitionId));

        if (competition.getCompetitionStatus() == CompetitionStatus.DRAFT) {
            boolean hasAccessToDraft = securityFacade.hasRole("ADMIN") || securityFacade.hasRole("ORG");
            if (!hasAccessToDraft) {
                throw new AccessDeniedException("You do not have permission to view this draft competition");
            }
        }
    }

    @Transactional(readOnly = true)
    public void checkVisibilityAccessByStageId(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
            .orElseThrow(() -> new StageNotFoundException(stageId));
        checkVisibilityAccess(stage.getCompetitionId());
    }

    @Transactional(readOnly = true)
    public void checkIfCompetitionPublishedByStageId(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
            .orElseThrow(() -> new StageNotFoundException(stageId));
        checkIfCompetitionPublishedByCompetitionId(stage.getCompetitionId());
    }

    @Transactional(readOnly = true)
    public void checkIfCompetitionPublishedByCompetitionId(Long competitionId) {
        Competition competition = competitionRepository.findById(competitionId)
            .orElseThrow(() -> new CompetitionNotFoundException(competitionId));
        if (competition.getCompetitionStatus() != CompetitionStatus.PUBLISHED) {
            throw new CompetitionHierarchyValidationException(
                "Cannot modify execution status: Competition must be PUBLISHED. Current status: %s"
                    .formatted(competition.getCompetitionStatus()));
        }
    }

    @Transactional(readOnly = true)
    public void checkIfStageInProgress(Long stageId, ExecutionStatus targetTourStatus) {
        Stage stage = stageRepository.findById(stageId)
            .orElseThrow(() -> new StageNotFoundException(stageId));

        if (targetTourStatus == ExecutionStatus.CANCELLED) {
            return;
        }

        if (stage.getStatus() != StageStatus.IN_PROGRESS) {
            throw new CompetitionHierarchyValidationException(
                "Cannot modify tour status to %s: Stage must be IN_PROGRESS. Current stage status: %s"
                    .formatted(targetTourStatus, stage.getStatus()));
        }
    }

    /**
     * Checks whether it is allowed to change the hierarchy (add/remove stages and
     * tours).
     *
     * @param competitionId ID of a competition
     */
    @Transactional(readOnly = true)
    public void validateImmutabilityByCompetitionId(Long competitionId) {
        Competition competition = competitionRepository.findById(competitionId)
            .orElseThrow(() -> new CompetitionNotFoundException(competitionId));

        if (competition.getCompetitionStatus() == CompetitionStatus.ARCHIVED) {
            throw new CompetitionHierarchyValidationException(
                "Cannot modify hierarchy: Competition is ARCHIVED (read-only).");
        }
        boolean isActiveLifecycleStatus = competition.getCompetitionStatus() == CompetitionStatus.ENROLLMENT
            || competition.getCompetitionStatus() == CompetitionStatus.PUBLISHED
            || competition.getCompetitionStatus() == CompetitionStatus.FINISHED;

        if (isActiveLifecycleStatus && participationInquiryPort.competitionHasParticipants(competitionId)) {
            throw new CompetitionHierarchyValidationException(
                "Cannot modify hierarchy: The competition is %s and has active participations."
                    .formatted(competition.getCompetitionStatus()));
        }
    }

    @Transactional(readOnly = true)
    public void validateImmutabilityByStageId(Long stageId) {
        Stage stage = stageRepository.findById(stageId)
            .orElseThrow(() -> new StageNotFoundException(stageId));
        validateImmutabilityByCompetitionId(stage.getCompetitionId());
    }

    /**
     * Validates that the stage belongs to the competition specified in the request
     * path. Prevents cross-competition manipulation (e.g., updating Stage 5 via
     * /competitions/999/stages/5).
     *
     * @param pathCompetitionId   Competition ID taken from URI as a path variable
     * @param entityCompetitionId Competition ID extracted from the fetched Stage
     *                            entity
     */
    public void validateStageEligibility(Long pathCompetitionId, Long entityCompetitionId) {
        if (!pathCompetitionId.equals(entityCompetitionId)) {
            throw new CompetitionHierarchyValidationException("Stage does not belong to this competition");
        }
    }

    public void validateTourEligibility(Long pathStageId, Long entityStageId) {
        if (!pathStageId.equals(entityStageId)) {
            throw new CompetitionHierarchyValidationException("Tour does not belong to this stage");
        }
    }

    @Transactional(readOnly = true)
    public void validateStageDates(Long competitionId, ZonedDateTime start, ZonedDateTime finish) {
        Competition parent = competitionRepository.findById(competitionId)
            .orElseThrow(() -> new CompetitionNotFoundException(competitionId));

        if (start.isBefore(parent.getDateStart()) || finish.isAfter(parent.getDateFinish())) {
            throw new CompetitionHierarchyValidationException(
                "Stage dates must be within Competition dates (" + parent.getDateStart() + " - "
                    + parent.getDateFinish() + ")");
        }
    }

    @Transactional(readOnly = true)
    public void validateTourDates(Long stageId, ZonedDateTime start, ZonedDateTime finish) {
        Stage parent = stageRepository.findById(stageId)
            .orElseThrow(() -> new StageNotFoundException(stageId));

        if (start.isBefore(parent.getDateStart()) || finish.isAfter(parent.getDateFinish())) {
            throw new CompetitionHierarchyValidationException(
                "Tour dates must be within Stage dates (" + parent.getDateStart() + " - " + parent.getDateFinish()
                    + ")");
        }
    }

    /**
     * Validates that narrowing a Stage's date range does not orphan any of its
     * already-existing Tours — i.e. that every Tour currently under this Stage
     * would still fall within the proposed new {@code newStart}/{@code newFinish}
     * window.
     */
    @Transactional(readOnly = true)
    public void validateStageDatesAgainstExistingTours(Long stageId, ZonedDateTime newStart, ZonedDateTime newFinish) {
        List<Tour> tours = tourRepository.findAllByStageIdOrderBySortPositionAsc(stageId);

        List<String> violatingTitles = tours.stream()
            .filter(tour -> tour.getDateStart().isBefore(newStart) || tour.getDateFinish().isAfter(newFinish))
            .map(Tour::getTitle)
            .toList();

        if (!violatingTitles.isEmpty()) {
            throw new CompetitionHierarchyValidationException(
                "Cannot update stage dates to (%s - %s): %d existing tour(s) would fall outside the new range: %s"
                    .formatted(newStart, newFinish, violatingTitles.size(), String.join(", ", violatingTitles)));
        }
    }

    @Transactional(readOnly = true)
    public void validateToursNotStartedByStageId(Long stageId) {
        if (!stageRepository.existsById(stageId)) {
            throw new StageNotFoundException(stageId);
        }
        List<Tour> tours = tourRepository.findAllByStageIdOrderBySortPositionAsc(stageId);

        boolean anyStarted = tours.stream()
            .anyMatch(tour -> tour.getExecutionStatus() != ExecutionStatus.SCHEDULED);

        if (anyStarted) {
            throw new CompetitionHierarchyValidationException(
                "Cannot reorder tours: one or more tours have already started execution.");
        }
    }

    public void validateTourStatusTransition(ExecutionStatus current, ExecutionStatus target) {
        if (current == target) {
            return;
        }

        boolean isValid = switch (current) {
            case SCHEDULED -> target == ExecutionStatus.IN_PROGRESS || target == ExecutionStatus.CANCELLED;
            case IN_PROGRESS -> target == ExecutionStatus.CLOSED || target == ExecutionStatus.FINISHED
                || target == ExecutionStatus.CANCELLED;
            case CLOSED -> target == ExecutionStatus.IN_PROGRESS || target == ExecutionStatus.FINISHED
                || target == ExecutionStatus.CANCELLED;
            case FINISHED, CANCELLED -> false;
        };

        if (!isValid) {
            throw new CompetitionHierarchyValidationException(
                "Invalid tour execution status transition from " + current + " to " + target);
        }
    }

    public void validateStageStatusTransition(StageStatus current, StageStatus target) {
        if (current == target) {
            return;
        }

        boolean isValid = switch (current) {
            case SCHEDULED -> target == StageStatus.IN_PROGRESS || target == StageStatus.CANCELLED;
            case IN_PROGRESS -> target == StageStatus.FINISHED || target == StageStatus.CANCELLED;
            case FINISHED, CANCELLED -> false;
        };

        if (!isValid) {
            throw new CompetitionHierarchyValidationException(
                "Invalid stage status transition from " + current + " to " + target);
        }
    }

    public void validateCompetitionStatusTransition(CompetitionStatus current, CompetitionStatus target) {
        if (current == target) {
            return;
        }

        // todo: add ability to rollback status (e.g. from PUBLISHED to ENROLLMENT)
        // & rollback, including all dependent objects
        boolean isValid = switch (current) {
            case DRAFT -> target == CompetitionStatus.ENROLLMENT;
            case ENROLLMENT -> target == CompetitionStatus.PUBLISHED;
            case PUBLISHED -> target == CompetitionStatus.FINISHED;
            case FINISHED -> target == CompetitionStatus.ARCHIVED;
            case ARCHIVED -> false;
        };

        if (!isValid) {
            throw new CompetitionHierarchyValidationException(
                "Invalid status transition from " + current + " to " + target);
        }
    }

    @Transactional(readOnly = true)
    public void validateTourEligibilityToStart(Tour currentTour) {
        Optional<Tour> previousTourOpt = tourRepository
            .findFirstByStageIdAndSortPositionLessThanOrderBySortPositionDesc(currentTour.getStageId(),
                currentTour.getSortPosition());

        if (previousTourOpt.isEmpty()) {
            return;
        }

        Tour previousTour = previousTourOpt.get();

        if (previousTour.getExecutionStatus() != ExecutionStatus.FINISHED) {
            throw new CompetitionHierarchyValidationException(
                "Cannot start tour '%s'. The previous tour '%s' is not yet FINISHED (Current status: %s)."
                    .formatted(currentTour.getTitle(), previousTour.getTitle(), previousTour.getExecutionStatus()));
        }
    }

    @Transactional(readOnly = true)
    public void validateStageEligibilityToStart(Stage currentStage) {
        Optional<Stage> previousStageOpt = stageRepository
            .findFirstByCompetitionIdAndSortPositionLessThanOrderBySortPositionDesc(currentStage.getCompetitionId(),
                currentStage.getSortPosition());

        if (previousStageOpt.isEmpty()) {
            return;
        }

        Stage previousStage = previousStageOpt.get();

        if (previousStage.getStatus() != StageStatus.FINISHED) {
            throw new CompetitionHierarchyValidationException(
                "Cannot start stage '%s'. The previous stage '%s' is not yet FINISHED (Current status: %s)."
                    .formatted(currentStage.getTitle(), previousStage.getTitle(), previousStage.getStatus()));
        }
    }

    @Transactional(readOnly = true)
    public void validateAllStagesCompletedForCompetition(Long competitionId) {
        List<Stage> stages = stageRepository.findAllByCompetitionIdOrderBySortPositionAsc(competitionId);

        if (stages.isEmpty()) {
            throw new CompetitionHierarchyValidationException(
                "Cannot finish competition: Competition must have at least one stage.");
        }
        List<String> incompleteStages = stages.stream()
            .filter(stage -> stage.getStatus() != StageStatus.FINISHED
                && stage.getStatus() != StageStatus.CANCELLED)
            .map(stage -> "'%s' (Status: %s)".formatted(stage.getTitle(), stage.getStatus()))
            .toList();

        if (!incompleteStages.isEmpty()) {
            throw new CompetitionHierarchyValidationException(
                "Cannot finish competition: Not all stages are completed. Incomplete stages: "
                    + String.join(", ", incompleteStages));
        }
    }

    @Transactional(readOnly = true)
    public void validateAllToursCompletedForStage(Long stageId) {
        List<Tour> tours = tourRepository.findAllByStageIdOrderBySortPositionAsc(stageId);

        if (tours.isEmpty()) {
            throw new CompetitionHierarchyValidationException(
                "Cannot finish stage: Stage must contain at least one tour.");
        }

        List<String> incompleteTours = tours.stream()
            .filter(tour -> tour.getExecutionStatus() != ExecutionStatus.FINISHED
                && tour.getExecutionStatus() != ExecutionStatus.CANCELLED)
            .map(tour -> "'%s' (Status: %s)".formatted(tour.getTitle(), tour.getExecutionStatus()))
            .toList();

        if (!incompleteTours.isEmpty()) {
            throw new CompetitionHierarchyValidationException(
                "Cannot finish stage: Not all tours are completed. Incomplete tours: "
                    + String.join(", ", incompleteTours));
        }
    }

    @Transactional(readOnly = true)
    public void validateTourEligibilityToResume(Tour tour) {
        String errorMessage =
            "Cannot resume tour '%s'. The finish date (%s) is in the past. "
                + "Please update the tour dates first to provide extra time.";

        if (tour.getDateFinish().isBefore(ZonedDateTime.now())) {
            throw new CompetitionHierarchyValidationException(
                errorMessage
                    .formatted(tour.getTitle(), tour.getDateFinish()));
        }
    }

    public void validateEntityVersion(Long expectedVersion, Long actualVersion, Class<?> entityClass, Long entityId) {
        if (!Objects.equals(expectedVersion, actualVersion)) {
            throw new StaleEntityVersionException(entityClass, entityId);
        }
    }
}