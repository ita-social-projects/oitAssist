package com.itasocialacademy.oitassist.taskassignment.dao.repository;

import com.itasocialacademy.oitassist.taskassignment.dao.model.TaskAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaskAssignmentRepository extends JpaRepository<TaskAssignment, Long> {
    List<TaskAssignment> findAllByTourId(Long tourId);

    boolean existsByTaskBodyIdAndTourId(Long taskBodyId, Long tourId);

    boolean existsByTaskBodyId(Long taskBodyId);
}
