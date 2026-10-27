package com.itasocialacademy.oitassist.participation.service.interfaces;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import java.util.List;

/**
 * Lightweight check of a user's participation in competitions by competition
 * status.
 *
 * <p>
 * Intentionally kept separate from {@link ParticipationService}: this contract
 * depends only on the participation repository, so it can be used by the
 * {@code user} module without pulling in {@code UserFacade} and creating a bean
 * cycle.
 * </p>
 */
public interface ParticipationStatusChecker {
    /**
     * Checks whether the user participates in at least one competition whose status
     * is among the given ones.
     *
     * @param userId   the ID of the user
     * @param statuses competition statuses to match; if empty, the result is
     *                 {@code false}
     * @return {@code true} if such a participation exists, {@code false} otherwise
     */
    boolean isUserParticipantWithCompetitionStatus(
        Long userId,
        List<CompetitionStatus> statuses);
}
