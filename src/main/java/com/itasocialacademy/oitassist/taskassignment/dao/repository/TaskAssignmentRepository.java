package com.itasocialacademy.oitassist.taskassignment.dao.repository;

import com.itasocialacademy.oitassist.taskassignment.dao.model.TaskAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {
    Page<TaskAssignment> findAllByTourId(Long tourId, Pageable pageable);

    boolean existsByTaskBodyIdAndTourId(Long taskBodyId, Long tourId);

    boolean existsByTaskBodyId(Long taskBodyId);

    @Query("SELECT t.tourId FROM TaskAssignment t WHERE t.taskBodyId = :taskBodyId")
    List<Long> findTourIdsByTaskBodyId(Long taskBodyId);
}
