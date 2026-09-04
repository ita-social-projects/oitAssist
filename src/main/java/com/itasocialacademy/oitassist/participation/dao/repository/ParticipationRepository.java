package com.itasocialacademy.oitassist.participation.dao.repository;

import com.itasocialacademy.oitassist.participation.dao.model.Participation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ParticipationRepository extends JpaRepository<Participation, Long> {
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
}
