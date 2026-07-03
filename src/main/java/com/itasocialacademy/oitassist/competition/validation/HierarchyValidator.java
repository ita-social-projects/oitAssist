package com.itasocialacademy.oitassist.competition.validation;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import com.itasocialacademy.oitassist.competition.dao.model.Stage;
import com.itasocialacademy.oitassist.competition.dao.repository.CompetitionRepository;
import com.itasocialacademy.oitassist.competition.dao.repository.StageRepository;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionHierarchyValidationException;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionNotFoundException;
import com.itasocialacademy.oitassist.competition.exceptions.StageNotFoundException;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class HierarchyValidator {
    private final CompetitionRepository competitionRepository;
    private final StageRepository stageRepository;
    private final SecurityFacade securityFacade;

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
        if (competition.getCompetitionStatus() == CompetitionStatus.PUBLISHED
            || competition.getCompetitionStatus() == CompetitionStatus.FINISHED) {
            // TODO: Epic Requirement - "restricted if active participations exist"
            // STUB for future integration w ParticipationRequest
            boolean hasActiveParticipations = false;

            if (hasActiveParticipations) {
                throw new CompetitionHierarchyValidationException(
                    "Cannot modify hierarchy: The competition is PUBLISHED and has active participations.");
            }
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
}