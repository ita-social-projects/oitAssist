package com.itasocialacademy.oitassist.competition.dao.repository;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CompetitionRepository extends JpaRepository<Competition, Long>, JpaSpecificationExecutor<Competition> {
    Page<Competition> findAllByCompetitionStatus(CompetitionStatus status, Pageable pageable);

    /**
     * Fetches a Competition with a pessimistic write lock (SELECT ... FOR UPDATE), used as the entry point for any
     * structural hierarchy mutation or status transition, to serialize concurrent changes on the same competition and
     * close the write-skew window between publish/finish and delete of a Stage/Tour.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c from Competition c where c.id = :id")
    Optional<Competition> findByIdForUpdate(@Param("id") Long id);
}
