package com.itasocialacademy.oitassist.participation.dao.repository;

import com.itasocialacademy.oitassist.participation.dao.model.Participation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    boolean existsByUserIdAndCompetitionIdAndStageId(
        Long userId,
        Long competitionId,
        Long stageId);

    boolean existsByCompetitionId(Long competitionId);

    boolean existsByStageId(Long stageId);
}
