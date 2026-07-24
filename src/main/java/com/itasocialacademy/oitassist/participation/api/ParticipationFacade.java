package com.itasocialacademy.oitassist.participation.api;

/**
 * Read-only facade exposing Competition and Stage Participation lookups to
 * other modules (e.g. {@code competition}). Returns boolean values representing
 * participants existing.
 */
public interface ParticipationFacade {
    boolean competitionHasParticipants(Long competitionId);

    boolean stageHasParticipants(Long stageId);

    boolean isUserParticipant(
            Long userId,
            Long competitionId,
            Long stageId
    );
}
