package com.itasocialacademy.oitassist.competition.service;

import com.itasocialacademy.oitassist.competition.dao.model.Stage;
import com.itasocialacademy.oitassist.competition.dao.repository.StageRepository;
import com.itasocialacademy.oitassist.competition.dto.request.CreateStageRequest;
import com.itasocialacademy.oitassist.competition.dto.request.UpdateStageRequest;
import com.itasocialacademy.oitassist.competition.dto.response.StageResponse;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionHierarchyValidationException;
import com.itasocialacademy.oitassist.competition.exceptions.StageNotFoundException;
import com.itasocialacademy.oitassist.competition.mapper.StageMapper;
import com.itasocialacademy.oitassist.competition.service.interfaces.StageService;
import com.itasocialacademy.oitassist.competition.validation.HierarchyValidator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StageServiceImpl implements StageService {
    private final StageRepository stageRepository;
    private final HierarchyValidator validator;
    private final StageMapper mapper;

    @Override
    @Transactional
    public StageResponse create(Long competitionId, CreateStageRequest request) {
        validator.validateImmutabilityByCompetitionId(competitionId);
        validator.validateStageDates(competitionId, request.dateStart(), request.dateFinish());

        Stage stage = mapper.toEntity(request);
        stage.setCompetitionId(competitionId);

        // TODO: validation by title must be more flexible (e.g. if exists "Lviv 1" ->
        // can't create "Lviv 2")
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
        Stage stage = stageRepository.findById(stageId)
            .orElseThrow(() -> new StageNotFoundException(stageId));

        validator.checkVisibilityAccess(stage.getCompetitionId());
        return mapper.toResponse(stage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StageResponse> getAllByCompetitionId(Long competitionId) {
        validator.checkVisibilityAccess(competitionId);
        return stageRepository.findAllByCompetitionIdOrderBySortPositionAsc(competitionId)
            .stream()
            .map(mapper::toResponse)
            .toList();
    }

    @Override
    @Transactional
    public StageResponse update(Long compId, Long stageId, UpdateStageRequest request) {
        Stage stage = stageRepository.findById(stageId)
            .orElseThrow(() -> new StageNotFoundException(stageId));

        validator.validateStageEligibility(compId, stage.getCompetitionId());
        validator.validateImmutabilityByCompetitionId(stage.getCompetitionId());
        validator.validateStageDates(stage.getCompetitionId(), request.dateStart(), request.dateFinish());

        stage.setTitle(request.title());
        stage.setDescription(request.description());
        stage.setDateStart(request.dateStart());
        stage.setDateFinish(request.dateFinish());
        stage.setScope(request.scope());

        if (request.sortPosition() != null) {
            stage.setSortPosition(request.sortPosition());
        }

        stageRepository.findAllByCompetitionIdOrderBySortPositionAsc(stage.getCompetitionId())
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

        validator.validateStageEligibility(compId, stage.getCompetitionId());
        validator.validateImmutabilityByCompetitionId(stage.getCompetitionId());

        stageRepository.delete(stage);
    }
}
