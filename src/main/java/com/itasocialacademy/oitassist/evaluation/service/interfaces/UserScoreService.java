package com.itasocialacademy.oitassist.evaluation.service.interfaces;

import com.itasocialacademy.oitassist.evaluation.api.dto.CompetitionScore;
import java.util.Collection;
import java.util.Map;

public interface UserScoreService {
    /**
     * Calculates the result of the given user in each of the given competitions.
     * Submissions without an evaluation are counted as zero.
     *
     * @param userId         the id of the user whose result is calculated
     * @param competitionIds the ids of the competitions; an empty collection
     *                       results in an empty map
     * @return a map from competition id to the user's {@link CompetitionScore}; it
     *         contains an entry for every requested competition
     */
    Map<Long, CompetitionScore> getUserScores(Long userId, Collection<Long> competitionIds);
}