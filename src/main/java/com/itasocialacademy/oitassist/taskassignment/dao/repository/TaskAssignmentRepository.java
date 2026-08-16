package com.itasocialacademy.oitassist.taskassignment.dao.repository;

import com.itasocialacademy.oitassist.taskassignment.dao.model.TaskAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {
    Page<TaskAssignment> findAllByTourId(Long tourId, Pageable pageable);

    boolean existsByTaskBodyIdAndTourId(Long taskBodyId, Long tourId);

    boolean existsByTaskBodyId(Long taskBodyId);
}
