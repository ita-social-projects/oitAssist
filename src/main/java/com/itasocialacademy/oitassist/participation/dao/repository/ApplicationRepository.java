package com.itasocialacademy.oitassist.participation.dao.repository;

import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import com.itasocialacademy.oitassist.participation.dao.model.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {
    boolean existsByIssuedByAndCompetitionIdAndStageIdAndStatus(
        Long studentId,
        Long competitionId,
        Long stageId,
        RequestStatus status);

    Page<Application> findAllByCompetitionIdAndStageIdAndStatus(
        Long competitionId,
        Long stageId,
        RequestStatus status,
        Pageable pageable);
}
