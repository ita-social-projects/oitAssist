package com.itasocialacademy.oitassist.evaluation.api.facade;

import com.itasocialacademy.oitassist.evaluation.api.dto.CompetitionScore;
import com.itasocialacademy.oitassist.evaluation.api.dto.OlympiadResults;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Read-only facade exposing aggregated olympiad results to other modules (e.g.
 * {@code export}, {@code competitionhistory}). Returns participant totals
 * summed per tour, stage and competition, as well as the result of a single
 * user in competitions.
 */
public interface EvaluationFacade {
    OlympiadResults getResults(Long competitionId, Set<Long> stageIds, Set<Long> tourIds, String search);

    /**
     * Calculates the result of the given user in each of the given competitions.
     *
     * @param userId         the id of the user whose result is calculated
     * @param competitionIds the ids of the competitions; an empty collection
     *                       results in an empty map
     * @return a map from competition id to the user's {@link CompetitionScore}; it
     *         contains an entry for every requested competition
     */
    Map<Long, CompetitionScore> getUserScores(Long userId, Collection<Long> competitionIds);
}
