package com.itasocialacademy.oitassist.participation.dao.repository;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.participation.dao.model.Participation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ParticipationRepository extends JpaRepository<Participation, Long>,
    JpaSpecificationExecutor<Participation> {
    boolean existsByUserIdAndCompetitionIdAndStageId(
        Long userId,
        Long competitionId,
        Long stageId);

    boolean existsByCompetitionId(Long competitionId);

    boolean existsByStageId(Long stageId);

    List<Participation> findAllByUserIdInAndCompetitionIdAndStageId(
        List<Long> userId,
        Long competitionId,
        Long stageId);

    boolean existsByUserIdAndStageId(Long userId, Long stageId);

    @Query("SELECT DISTINCT p.competitionId FROM Participation p WHERE p.userId = :userId")
    List<Long> findDistinctCompetitionIdsByUserId(@Param("userId") Long userId);
      
    @Query("""
            SELECT CASE
                WHEN COUNT(p) > 0 THEN true
                ELSE false
            END
            FROM Participation p
            JOIN Competition c ON c.id = p.competitionId
            WHERE p.userId = :userId
                AND c.competitionStatus IN :statuses
        """)
    boolean existsByUserIdAndCompetitionStatusIn(
        @Param("userId") Long userId,
        @Param("statuses") List<CompetitionStatus> statuses);
}
