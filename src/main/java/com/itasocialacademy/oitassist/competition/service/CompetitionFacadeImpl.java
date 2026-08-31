package com.itasocialacademy.oitassist.competition.service;

import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.competition.api.dto.CompetitionDetail;
import com.itasocialacademy.oitassist.competition.api.dto.CompetitionTreeDetail;
import com.itasocialacademy.oitassist.competition.api.dto.StageDetail;
import com.itasocialacademy.oitassist.competition.api.dto.StageTreeDetail;
import com.itasocialacademy.oitassist.competition.api.dto.TourDetail;
import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import com.itasocialacademy.oitassist.competition.dao.model.Stage;
import com.itasocialacademy.oitassist.competition.dao.model.Tour;
import com.itasocialacademy.oitassist.competition.dao.repository.CompetitionRepository;
import com.itasocialacademy.oitassist.competition.dao.repository.StageRepository;
import com.itasocialacademy.oitassist.competition.dao.repository.TourRepository;
import com.itasocialacademy.oitassist.competition.mapper.CompetitionMapper;
import com.itasocialacademy.oitassist.competition.mapper.StageMapper;
import com.itasocialacademy.oitassist.competition.mapper.TourMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class CompetitionFacadeImpl implements CompetitionFacade {
    private final CompetitionRepository competitionRepository;
    private final StageRepository stageRepository;
    private final TourRepository tourRepository;
    private final CompetitionMapper competitionMapper;
    private final StageMapper stageMapper;
    private final TourMapper tourMapper;

    @Override
    @Transactional(readOnly = true)
    public Optional<CompetitionDetail> findCompetitionById(Long competitionId) {
        Objects.requireNonNull(competitionId, "competitionId must not be null");
        return competitionRepository.findById(competitionId)
            .map(competitionMapper::toCompetitionDetail);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StageDetail> findStageById(Long stageId) {
        Objects.requireNonNull(stageId, "stageId must not be null");
        return stageRepository.findById(stageId)
            .map(stageMapper::toStageDetail);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TourDetail> findTourById(Long tourId) {
        Objects.requireNonNull(tourId, "tourId must not be null");
        return tourRepository.findById(tourId)
            .map(tourMapper::toTourDetail);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StageDetail> findStagesByCompetitionId(Long competitionId) {
        Objects.requireNonNull(competitionId, "competitionId must not be null");
        return stageRepository.findAllByCompetitionIdOrderBySortPositionAsc(competitionId)
            .stream()
            .map(stageMapper::toStageDetail)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CompetitionTreeDetail> findCompetitionTreeByCompetitionId(Long competitionId) {
        Objects.requireNonNull(competitionId, "competitionId must not be null");

        return competitionRepository.findById(competitionId)
            .map(this::buildTreeDetail);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TourDetail> findToursByIds(List<Long> tourIds) {
        Objects.requireNonNull(tourIds, "tourIds must not be null");
        if (tourIds.isEmpty()) {
            return List.of();
        }
        return tourRepository.findAllById(tourIds).stream()
            .map(tourMapper::toTourDetail)
            .toList();
    }

    private CompetitionTreeDetail buildTreeDetail(Competition competitionEntity) {
        CompetitionDetail competitionDetail = competitionMapper.toCompetitionDetail(competitionEntity);

        List<Stage> stages = stageRepository
            .findAllByCompetitionIdOrderBySortPositionAsc(competitionEntity.getId());

        if (stages.isEmpty()) {
            return new CompetitionTreeDetail(competitionDetail, Collections.emptyList());
        }

        List<Long> stageIds = stages.stream().map(Stage::getId).toList();
        List<Tour> allTours = tourRepository.findAllByStageIdInOrderBySortPositionAsc(stageIds);

        Map<Long, List<TourDetail>> toursByStage = allTours.stream()
            .collect(Collectors.groupingBy(
                Tour::getStageId,
                Collectors.mapping(tourMapper::toTourDetail, Collectors.toList())));

        List<StageTreeDetail> stageTrees = stages.stream()
            .map(stage -> new StageTreeDetail(
                stageMapper.toStageDetail(stage),
                toursByStage.getOrDefault(stage.getId(), Collections.emptyList())))
            .toList();

        return new CompetitionTreeDetail(competitionDetail, stageTrees);
    }
}
