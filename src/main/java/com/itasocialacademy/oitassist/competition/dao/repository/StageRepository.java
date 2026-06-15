package com.itasocialacademy.oitassist.competition.dao.repository;

import com.itasocialacademy.oitassist.competition.dao.model.Stage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StageRepository extends JpaRepository<Stage, Long> {
    /**
     * Checks whether a stage with that name already exists within a specific
     * Competition.
     */
    boolean existsByCompetitionIdAndTitle(Long competitionId, String title);

    /**
     * Returns all stages of the Competition sorted by their position (from smallest
     * to largest).
     */
    List<Stage> findAllByCompetitionIdOrderBySortPositionAsc(Long competitionId);

    /**
     * Find the stage with the maximum sort_position within the Competition.
     */
    Stage findTopByCompetitionIdOrderBySortPositionDesc(Long competitionId);
}