package com.itasocialacademy.oitassist.competition.service;

import com.itasocialacademy.oitassist.competition.dao.dto.request.CreateStageRequest;
import com.itasocialacademy.oitassist.competition.dao.dto.request.UpdateStageRequest;
import com.itasocialacademy.oitassist.competition.dao.dto.response.CompetitionResponse;
import com.itasocialacademy.oitassist.competition.dao.dto.response.StageResponse;
import com.itasocialacademy.oitassist.competition.dao.model.Stage;
import com.itasocialacademy.oitassist.competition.dao.repository.StageRepository;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionHierarchyValidationException;
import com.itasocialacademy.oitassist.competition.exceptions.StageNotFoundException;
import com.itasocialacademy.oitassist.competition.mapper.StageMapper;
import com.itasocialacademy.oitassist.competition.service.interfaces.CompetitionService;
import com.itasocialacademy.oitassist.competition.service.interfaces.StageService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StageServiceImpl implements StageService {
    private final StageRepository stageRepository;
    private final CompetitionService competitionService;
    private final StageMapper mapper;

    @Override
    @Transactional
    public StageResponse create(Long competitionId, CreateStageRequest request) {
        competitionService.validateHierarchyImmutability(competitionId);

        CompetitionResponse competition = competitionService.getById(competitionId);

        Stage stage = mapper.toEntity(request);
        stage.setCompetitionId(competitionId);

        validateDates(competition, stage);

        if (stageRepository.existsByCompetitionIdAndTitle(competitionId, stage.getTitle())) {
            throw new CompetitionHierarchyValidationException("Stage title already exists in this competition.");
        }

        if (stage.getSortPosition() != null) {
            boolean positionExists =
                stageRepository.existsByCompetitionIdAndSortPosition(competitionId, stage.getSortPosition());
            if (positionExists) {
                throw new CompetitionHierarchyValidationException(
                    "Sort position " + stage.getSortPosition() + " is already taken in this competition.");
            }
        } else {
            Stage lastStage = stageRepository.findTopByCompetitionIdOrderBySortPositionDesc(competitionId);
            stage.setSortPosition(lastStage != null ? (short) (lastStage.getSortPosition() + 1) : 1);
        }
        return mapper.toResponse(stageRepository.save(stage));
    }

    @Override
    @Transactional(readOnly = true)
    public StageResponse getById(Long stageId) {
        return stageRepository.findById(stageId)
            .map(mapper::toResponse)
            .orElseThrow(() -> new StageNotFoundException(stageId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StageResponse> getAllByCompetitionId(Long competitionId) {
        return stageRepository.findAllByCompetitionIdOrderBySortPositionAsc(competitionId)
            .stream()
            .map(mapper::toResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StageResponse update(Long compId, Long stageId, UpdateStageRequest request) {
        Stage stage = stageRepository.findById(stageId)
            .orElseThrow(() -> new StageNotFoundException(stageId));

        Long competitionId = stage.getCompetitionId();
        validateStageEligibility(compId, competitionId);

        competitionService.validateHierarchyImmutability(competitionId);
        CompetitionResponse competition = competitionService.getById(competitionId);

        stage.setTitle(request.title());
        stage.setDescription(request.description());
        stage.setDateStart(request.dateStart());
        stage.setDateFinish(request.dateFinish());
        stage.setScope(request.scope());

        if (request.sortPosition() != null) {
            stage.setSortPosition(request.sortPosition());
        }

        validateDates(competition, stage);

        stageRepository.findAllByCompetitionIdOrderBySortPositionAsc(competitionId)
            .stream()
            .filter(existing -> !existing.getId().equals(stageId))
            .forEach(existing -> {
                if (existing.getTitle().equals(stage.getTitle())) {
                    throw new CompetitionHierarchyValidationException(
                        "Stage title already exists in this competition.");
                }
                if (existing.getSortPosition().equals(stage.getSortPosition())) {
                    throw new CompetitionHierarchyValidationException(
                        "Sort position " + stage.getSortPosition() + " is already taken.");
                }
            });

        return mapper.toResponse(stageRepository.save(stage));
    }

    @Override
    @Transactional
    public void delete(Long compId, Long stageId) {
        Stage stage = stageRepository.findById(stageId)
            .orElseThrow(() -> new StageNotFoundException(stageId));
        Long competitionId = stage.getCompetitionId();

        validateStageEligibility(compId, competitionId);
        competitionService.validateHierarchyImmutability(competitionId);

        //TODO: need invoke method here that delete all tours related to this stage
        // or db manage it (check migration)?
        stageRepository.delete(stage);
    }

    /**
     * Validates that the stage belongs to the competition specified in the request path. Prevents cross-competition
     * manipulation (e.g., updating Stage 5 via /competitions/999/stages/5).
     *
     * @param pathCompetitionId   Competition ID taken from URI as a path variable
     * @param entityCompetitionId Competition ID extracted from the fetched Stage entity
     */
    private void validateStageEligibility(Long pathCompetitionId, Long entityCompetitionId) {
        if (!pathCompetitionId.equals(entityCompetitionId)) {
            throw new CompetitionHierarchyValidationException("Stage does not belong to this competition");
        }
    }

    /**
     * Validates that the child Stage's dates are strictly within the boundaries of the parent Competition.
     */
    private void validateDates(CompetitionResponse parent, Stage child) {
        if (child.getDateStart().isBefore(parent.dateStart())
            || child.getDateFinish().isAfter(parent.dateFinish())) {
            throw new CompetitionHierarchyValidationException(
                "Stage dates (%s - %s) must be within Competition dates (%s - %s)".formatted(
                    child.getDateStart(), child.getDateFinish(), parent.dateStart(), parent.dateFinish()));
        }
    }
}
