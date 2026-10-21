package com.itasocialacademy.oitassist.participation.api;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import java.util.List;

/**
 * Read-only facade exposing Competition and Stage Participation lookups to
 * other modules (e.g. {@code competition}). Returns boolean values representing
 * participants existing.
 */
public interface ParticipationFacade {
    boolean competitionHasParticipants(Long competitionId);

    boolean stageHasParticipants(Long stageId);

    boolean isUserParticipant(Long userId, Long competitionId, Long stageId);

    boolean isUserAStageParticipant(Long userId, Long stageId);

//    boolean isUserParticipantWithCompetitionStatus(Long userId, List<CompetitionStatus> competitionStatuses);
}
