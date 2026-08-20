package com.itasocialacademy.oitassist.participation.dao.repository;

import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import com.itasocialacademy.oitassist.participation.dao.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    boolean existsByIssuedByAndCompetitionIdAndStageIdAndStatus(
        Long studentId,
        Long competitionId,
        Long stageId,
        RequestStatus status);

    List<Application> findAllByCompetitionIdAndStageIdAndStatus(Long competitionId, Long stageId, RequestStatus status);
}
