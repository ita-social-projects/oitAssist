package com.itasocialacademy.oitassist.competition.dao.repository;

import com.itasocialacademy.oitassist.competition.dao.model.Tour;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
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

    Tour findTopByStageIdOrderBySortPositionDesc(Long stageId);
}
