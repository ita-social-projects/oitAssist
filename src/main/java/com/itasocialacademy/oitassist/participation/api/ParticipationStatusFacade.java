package com.itasocialacademy.oitassist.participation.api;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import java.util.List;

public interface ParticipationStatusFacade {
    boolean isUserParticipantWithCompetitionStatus(
        Long userId,
        List<CompetitionStatus> statuses);
}