package com.itasocialacademy.oitassist.participation.service;

import com.itasocialacademy.oitassist.competition.spi.ParticipationInquiryPort;
import com.itasocialacademy.oitassist.participation.api.ParticipationFacade;
import com.itasocialacademy.oitassist.participation.dao.repository.ParticipationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParticipationFacadeImpl implements ParticipationFacade, ParticipationInquiryPort {
    private final ParticipationRepository participationRepository;

    @Override
    public boolean competitionHasParticipants(Long competitionId) {
        return participationRepository.existsByCompetitionId(competitionId);
    }

    @Override
    public boolean stageHasParticipants(Long stageId) {
        return participationRepository.existsByStageId(stageId);
    }

    @Override
    public boolean isUserParticipant(Long userId, Long competitionId, Long stageId) {
        return participationRepository.existsByUserIdAndCompetitionIdAndStageId(userId, competitionId, stageId);
    }
      
    @Override
    public boolean isUserAStageParticipant(Long userId, Long stageId) {
        return participationRepository.existsByUserIdAndStageId(userId, stageId);
    }
}
