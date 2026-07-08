package com.itasocialacademy.oitassist.competition.service;

import com.itasocialacademy.oitassist.competition.api.dto.CompetitionDetail;
import com.itasocialacademy.oitassist.competition.api.dto.StageDetail;
import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.competition.dao.repository.CompetitionRepository;
import com.itasocialacademy.oitassist.competition.dao.repository.StageRepository;
import com.itasocialacademy.oitassist.competition.mapper.CompetitionMapper;
import com.itasocialacademy.oitassist.competition.mapper.StageMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class CompetitionFacadeImpl implements CompetitionFacade {
    private final CompetitionRepository competitionRepository;
    private final StageRepository stageRepository;
    private final CompetitionMapper competitionMapper;
    private final StageMapper stageMapper;

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
}
