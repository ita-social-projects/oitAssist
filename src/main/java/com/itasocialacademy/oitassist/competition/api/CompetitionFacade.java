package com.itasocialacademy.oitassist.competition.api;

import com.itasocialacademy.oitassist.competition.api.dto.CompetitionDetail;
import com.itasocialacademy.oitassist.competition.api.dto.CompetitionTreeDetail;
import com.itasocialacademy.oitassist.competition.api.dto.StageDetail;
import com.itasocialacademy.oitassist.competition.api.dto.TourDetail;
import java.util.List;
import java.util.Optional;

/**
 * Read-only facade exposing minimal Competition/Stage/Tour lookups to other
 * modules (e.g. {@code participation}). Deliberately returns DTOs rather than
 * JPA entities to keep {@code competition}'s persistence model private and to
 * ensure callers can't bypass the business rules enforced by
 * {@code HierarchyValidator} / {@code CompetitionServiceImpl}.
 */
public interface CompetitionFacade {
    /**
     * Retrieves a competition by its ID.
     *
     * @param competitionId Competition ID, must not be {@code null}
     * @return the competition details, or empty if no competition with this ID
     *         exists
     */
    Optional<CompetitionDetail> findCompetitionById(Long competitionId);

    /**
     * Retrieves a stage by its ID.
     *
     * @param stageId Stage ID, must not be {@code null}
     * @return the stage details, or empty if no stage with this ID exists
     */
    Optional<StageDetail> findStageById(Long stageId);

    /**
     * Retrieves a tour by its ID.
     *
     * @param tourId Tour ID, must not be {@code null}
     * @return the tour details, or empty if no tour with this ID exists
     */
    Optional<TourDetail> findTourById(Long tourId);

    /**
     * Retrieves all stages belonging to a competition, ordered by their sort
     * position.
     *
     * @param competitionId Competition ID, must not be {@code null}
     * @return stages of the competition in order, or an empty list if the
     *         competition has no stages (or does not exist)
     */
    List<StageDetail> findStagesByCompetitionId(Long competitionId);

    /**
     * Retrieves the full hierarchy of a competition — the competition itself
     * together with all its stages and, for each stage, its tours — in a single
     * nested response.
     *
     * @param competitionId Competition ID, must not be {@code null}
     * @return the competition tree, or empty if no competition with this ID exists
     */
    Optional<CompetitionTreeDetail> findCompetitionTreeByCompetitionId(Long competitionId);

    /**
     * Retrieves tours by their IDs.
     *
     * @param tourIds Tour IDs, must not be {@code null} (an empty list yields an
     *                empty result)
     * @return the tours found for the given IDs, in unspecified order; IDs with no
     *         matching tour are silently omitted, so the result may be smaller than
     *         {@code tourIds}
     */
    List<TourDetail> findToursByIds(List<Long> tourIds);
}
