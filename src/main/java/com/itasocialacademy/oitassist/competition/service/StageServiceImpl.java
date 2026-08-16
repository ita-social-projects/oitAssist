package com.itasocialacademy.oitassist.competition.service;

import com.itasocialacademy.oitassist.competition.dao.enums.StageStatus;
import com.itasocialacademy.oitassist.competition.dao.model.Stage;
import com.itasocialacademy.oitassist.competition.dao.repository.StageRepository;
import com.itasocialacademy.oitassist.competition.dto.request.ChangeStageStatusRequest;
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

        if (stageRepository.existsByCompetitionIdAndTitle(competitionId, stage.getTitle())) {
            throw new CompetitionHierarchyValidationException("Stage title already exists in this competition.");
        }

        if (stageRepository.existsByCompetitionIdAndScope(competitionId, stage.getScope())) {
            throw new CompetitionHierarchyValidationException(
                "A stage with scope %s already exists in this competition.".formatted(stage.getScope()));
        }

        if (stage.getSortPosition() != null) {
            boolean positionExists =
                stageRepository.existsByCompetitionIdAndSortPosition(competitionId, stage.getSortPosition());
            if (positionExists) {
                throw new CompetitionHierarchyValidationException(
                    "Sort position " + stage.getSortPosition() + " is already taken in this competition.");
            }
        } else {
            Stage lastStage = stageRepository.findTopByCompetitionIdOrderBySortPositionDesc(competitionId)
                .orElse(null);
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
        validator.validateStageDatesAgainstExistingTours(stageId, request.dateStart(), request.dateFinish());

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
                if (existing.getScope() == request.scope()) {
                    throw new CompetitionHierarchyValidationException(
                        "A stage with scope %s already exists in this competition.".formatted(request.scope()));
                }
                if (existing.getSortPosition().equals(stage.getSortPosition())) {
                    throw new CompetitionHierarchyValidationException(
                        "Sort position %s is already taken.".formatted(stage.getSortPosition()));
                }
            });

        return mapper.toResponse(stageRepository.save(stage));
    }

    @Override
    @Transactional
    public StageResponse changeStatus(Long compId, Long stageId, ChangeStageStatusRequest request) {
        Stage stage = stageRepository.findById(stageId)
            .orElseThrow(() -> new StageNotFoundException(stageId));

        validator.validateStageEligibility(compId, stage.getCompetitionId());
        validator.checkIfCompetitionPublishedByCompetitionId(compId);
        validator.validateStageStatusTransition(stage.getStatus(), request.status());

        if (request.status() == StageStatus.IN_PROGRESS) {
            validator.validateStageEligibilityToStart(stage);
        } else if (request.status() == StageStatus.FINISHED) {
            validator.validateAllToursCompletedForStage(stageId);
        }

        stage.setStatus(request.status());

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
