package com.itasocialacademy.oitassist.participation.service;

import com.itasocialacademy.oitassist.competition.spi.ParticipationInquiryPort;
import com.itasocialacademy.oitassist.participation.api.ParticipationFacade;
import com.itasocialacademy.oitassist.participation.service.interfaces.ParticipationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParticipationFacadeImpl implements ParticipationFacade, ParticipationInquiryPort {
    private final ParticipationService participationService;

    @Override
    public boolean competitionHasParticipants(Long competitionId) {
        return participationService.competitionHasParticipants(competitionId);
    }

    @Override
    public boolean stageHasParticipants(Long stageId) {
        return participationService.stageHasParticipants(stageId);
    }

    @Override
    public boolean isUserParticipant(Long userId, Long competitionId, Long stageId) {
        return participationService.isUserParticipant(userId, competitionId, stageId);
    }

    @Override
    public boolean isUserAStageParticipant(Long userId, Long stageId) {
        return participationService.isUserAStageParticipant(userId, stageId);
    }
}
