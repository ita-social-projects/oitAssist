package com.itasocialacademy.oitassist.evaluation.service.interfaces;

import com.itasocialacademy.oitassist.evaluation.api.dto.OlympiadResults;
import com.itasocialacademy.oitassist.evaluation.api.dto.ParticipantResult;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ResultsService {
    /**
     * Aggregates participants' scores for the selected scope: task scores are
     * summed into tours, tours into stages, and stages into the total. A task that
     * has not been graded yet makes the corresponding tour, stage and total
     * unknown.
     *
     * <p>
     * Scope selection: {@code tourIds} take priority over {@code stageIds}; if both
     * are empty, the whole olympiad is used. Every participant is returned with all
     * tours of the scope, so all results contain the same set of tours.
     * </p>
     *
     * @param competitionId Competition ID
     * @param stageIds      stages to include, empty for no stage filter
     * @param tourIds       tours to include, empty for no tour filter
     * @param search        optional participant's full name, case-insensitive,
     *                      matches any part of the name
     * @return {@link OlympiadResults} with the olympiad title, the selected scope
     *         title and aggregated participants
     */
    OlympiadResults getResults(Long competitionId, Set<Long> stageIds, Set<Long> tourIds, String search);

    /**
     * Same as {@link #getResults(Long, Set, Set, String)}, but returns a single
     * page of participants. Paging is applied after aggregation and filtering.
     *
     * @param competitionId Competition ID
     * @param stageIds      stages to include, empty for no stage filter
     * @param tourIds       tours to include, empty for no tour filter
     * @param search        optional participant name filter, case-insensitive
     * @param pageable      pagination
     * @return a page of {@link ParticipantResult}
     */
    Page<ParticipantResult> getResultsPage(Long competitionId, Set<Long> stageIds, Set<Long> tourIds,
        String search, Pageable pageable);
}