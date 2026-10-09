package com.itasocialacademy.oitassist.participation.api;

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

    /**
     * Retrieves the IDs of all competitions in which the user is a participant.
     *
     * @param userId User ID, must not be {@code null}
     * @return distinct competition IDs, or an empty list if the user has not
     *         participated in any competition
     */
    List<Long> findCompetitionIdsByUserId(Long userId);
}
