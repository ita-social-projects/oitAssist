package com.itasocialacademy.oitassist.competition.service;

import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.competition.api.dto.CompetitionDetail;
import com.itasocialacademy.oitassist.competition.api.dto.StageDetail;
import com.itasocialacademy.oitassist.competition.api.dto.TourDetail;
import com.itasocialacademy.oitassist.competition.dao.repository.CompetitionRepository;
import com.itasocialacademy.oitassist.competition.dao.repository.StageRepository;
import com.itasocialacademy.oitassist.competition.dao.repository.TourRepository;
import com.itasocialacademy.oitassist.competition.mapper.CompetitionMapper;
import com.itasocialacademy.oitassist.competition.mapper.StageMapper;
import com.itasocialacademy.oitassist.competition.mapper.TourMapper;
import java.util.Optional;
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
        return competitionRepository.findById(competitionId)
            .map(competitionMapper::toCompetitionDetail);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StageDetail> findStageById(Long stageId) {
        return stageRepository.findById(stageId)
            .map(stageMapper::toStageDetail);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TourDetail> findTourById(Long tourId) {
        return tourRepository.findById(tourId)
            .map(tourMapper::toTourDetail);
    }
}
