package com.itasocialacademy.oitassist.participation.components.checker;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.participation.service.interfaces.ParticipationStatusChecker;
import com.itasocialacademy.oitassist.user.api.interfaces.ActiveParticipationChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
class ParticipationActiveChecker implements ActiveParticipationChecker {
    private final ParticipationStatusChecker participationStatusChecker;

    @Override
    public boolean hasActiveParticipation(Long userId) {
        return participationStatusChecker.isUserParticipantWithCompetitionStatus(
            userId, List.of(CompetitionStatus.PUBLISHED, CompetitionStatus.ENROLLMENT));
    }
}