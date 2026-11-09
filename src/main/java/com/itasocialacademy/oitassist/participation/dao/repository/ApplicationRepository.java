package com.itasocialacademy.oitassist.participation.dao.repository;

import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import com.itasocialacademy.oitassist.participation.dao.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {
    boolean existsByIssuedByAndCompetitionIdAndStageIdAndStatus(
        Long studentId,
        Long competitionId,
        Long stageId,
        RequestStatus status);

    @Query("SELECT a.issuedBy FROM Application a WHERE a.stageId = :stageId AND a.status = :status")
    List<Long> findIssuedByIdsByStageIdAndStatus(
        @Param("stageId") Long stageId,
        @Param("status") RequestStatus status);
}
