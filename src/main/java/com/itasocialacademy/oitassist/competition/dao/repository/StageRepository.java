package com.itasocialacademy.oitassist.competition.dao.repository;

import com.itasocialacademy.oitassist.competition.dao.model.Stage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StageRepository extends JpaRepository<Stage, Long> {
    /**
     * Checks whether a stage with that name already exists within a specific
     * Competition.
     */
    boolean existsByCompetitionIdAndTitle(Long competitionId, String title);

    /**
     * Checks whether a competition has at least one stage.
     */
    boolean existsByCompetitionId(Long competitionId);

    /**
     * Returns all stages of the Competition sorted by their position (from smallest
     * to largest).
     */
    List<Stage> findAllByCompetitionIdOrderBySortPositionAsc(Long competitionId);

    /**
     * Find the stage with the maximum sort_position within the Competition.
     */
    Optional<Stage> findTopByCompetitionIdOrderBySortPositionDesc(Long competitionId);

    /**
     * Checks whether a stage with that sort position already exists within a
     * specific Competition.
     */
    boolean existsByCompetitionIdAndSortPosition(Long competitionId, Short sortPosition);

    @Query("""
            SELECT COUNT(s) FROM Stage s
            WHERE s.competitionId = :competitionId
              AND NOT EXISTS (SELECT t FROM Tour t WHERE t.stageId = s.id)
        """)
    long countStagesWithoutTours(@Param("competitionId") Long competitionId);

    Optional<Stage> findByCompetitionIdAndSortPosition(Long competitionId, Short sortPosition);

    Optional<Stage> findFirstByCompetitionIdAndSortPositionLessThanOrderBySortPositionDesc(Long competitionId,
        Short sortPositionIsLessThan);
}