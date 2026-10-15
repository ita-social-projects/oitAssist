package com.itasocialacademy.oitassist.competition.dao.repository;

import com.itasocialacademy.oitassist.competition.dao.model.CompetitionTourView;
import com.itasocialacademy.oitassist.competition.dao.model.Tour;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TourRepository extends JpaRepository<Tour, Long> {
    /**
     * Checks whether a tour with that name already exists within a specific stage.
     */
    boolean existsByStageIdAndTitle(Long stageId, String title);

    /**
     * Returns all tours of a specific stage, following their logical sequence.
     */
    List<Tour> findAllByStageIdOrderBySortPositionAsc(Long stageId);

    Optional<Tour> findTopByStageIdOrderBySortPositionDesc(Long stageId);

    List<Tour> findAllByStageIdInOrderBySortPositionAsc(List<Long> stageIds);

    Optional<Tour> findByStageIdAndSortPosition(Long stageId, Short sortPosition);

    Optional<Tour> findFirstByStageIdAndSortPositionLessThanOrderBySortPositionDesc(Long stageId, Short sortPosition);

    long countByStageId(Long stageId);

    /**
     * Returns pairs of (competition ID, tour ID) for all tours that belong to the
     * given competitions. A tour belongs to a competition through its stage.
     * Competitions without tours produce no rows.
     */
    @Query("""
            SELECT s.competitionId AS competitionId, t.id AS tourId
            FROM Tour t
            JOIN Stage s ON s.id = t.stageId
            WHERE s.competitionId IN :competitionIds
        """)
    List<CompetitionTourView> findTourIdsByCompetitionIds(
        @Param("competitionIds") Collection<Long> competitionIds);
}
