package com.itasocialacademy.oitassist.participation.service;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.participation.api.ParticipationStatusFacade;
import com.itasocialacademy.oitassist.participation.service.interfaces.ParticipationStatusChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ParticipationStatusFacadeImpl implements ParticipationStatusFacade {
    private final ParticipationStatusChecker participationStatusChecker;

    @Override
    public boolean isUserParticipantWithCompetitionStatus(Long userId, List<CompetitionStatus> statuses) {
        return participationStatusChecker.isUserParticipantWithCompetitionStatus(userId, statuses);
    }
}