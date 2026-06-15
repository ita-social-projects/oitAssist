package com.itasocialacademy.oitassist.competition.service;

import com.itasocialacademy.oitassist.competition.dao.dto.request.CreateStageRequest;
import com.itasocialacademy.oitassist.competition.dao.dto.response.CompetitionResponse;
import com.itasocialacademy.oitassist.competition.dao.dto.response.StageResponse;
import com.itasocialacademy.oitassist.competition.dao.model.Stage;
import com.itasocialacademy.oitassist.competition.dao.repository.StageRepository;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionHierarchyValidationException;
import com.itasocialacademy.oitassist.competition.exceptions.StageNotFoundException;
import com.itasocialacademy.oitassist.competition.mapper.StageMapper;
import com.itasocialacademy.oitassist.competition.service.interfaces.CompetitionService;
import com.itasocialacademy.oitassist.competition.service.interfaces.StageService;
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

        if (stage.getSortPosition() == null) {
            Stage lastStage = stageRepository.findTopByCompetitionIdOrderBySortPositionDesc(competitionId);
            stage.setSortPosition(lastStage != null ? (short) (lastStage.getSortPosition() + 1) : 1);
        }

        return mapper.toResponse(stageRepository.save(stage));
    }

    @Override
    @Transactional(readOnly = true)
    public StageResponse getById(Long id) {
        return stageRepository.findById(id)
            .map(mapper::toResponse)
            .orElseThrow(() -> new StageNotFoundException("Stage not found"));
    }

    private void validateDates(CompetitionResponse parent, Stage child) {
        if (child.getDateStart().isBefore(parent.dateStart())
            || child.getDateFinish().isAfter(parent.dateFinish())) {
            throw new CompetitionHierarchyValidationException(
                "Stage dates (%s - %s) must be within Competition dates (%s - %s)".formatted(
                    child.getDateStart(), child.getDateFinish(), parent.dateStart(), parent.dateFinish()));
        }
    }
}
