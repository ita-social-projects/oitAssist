package com.itasocialacademy.oitassist.competition.dao.repository;

import com.itasocialacademy.oitassist.competition.dao.model.Tour;
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

    @Query("""
            SELECT t FROM Tour t
            WHERE t.stageId = :stageId
              AND t.sortPosition < :sortPosition
            ORDER BY t.sortPosition DESC
        """)
    Optional<Tour> findFirstPreviousTour(
        @Param("stageId") Long stageId,
        @Param("sortPosition") Short sortPosition);
}
