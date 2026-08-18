package com.itasocialacademy.oitassist.evaluation.api.facade;

import com.itasocialacademy.oitassist.evaluation.api.dto.OlympiadResults;
import java.util.Set;

/**
 * Read-only facade exposing aggregated olympiad results to other modules (e.g.
 * {@code export}). Returns participant totals summed per tour, stage and
 * competition.
 */
public interface EvaluationFacade {
    OlympiadResults getResults(Long competitionId, Set<Long> stageIds, Set<Long> tourIds, String search);
}
