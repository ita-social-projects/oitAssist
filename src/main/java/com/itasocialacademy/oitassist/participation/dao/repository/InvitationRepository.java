package com.itasocialacademy.oitassist.participation.dao.repository;

import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import com.itasocialacademy.oitassist.participation.dao.model.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long>, JpaSpecificationExecutor<Invitation> {
    List<Invitation> findByStudentIdInAndCompetitionIdAndStageIdAndStatus(
        List<Long> studentId,
        Long competitionId,
        Long stageId,
        RequestStatus status);

    @Query("SELECT i.studentId FROM Invitation i WHERE i.stageId = :stageId AND i.status = :status")
    List<Long> findStudentIdsByStageIdAndStatus(
        @Param("stageId") Long stageId,
        @Param("status") RequestStatus status);
}
