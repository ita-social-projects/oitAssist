package com.itasocialacademy.oitassist.taskassignment.api;

import com.itasocialacademy.oitassist.taskassignment.api.dto.TaskAssignmentDetailDTO;
import java.util.Optional;

/**
 * Facade providing operations for managing task assignments from other modules. Handles retrieval by id of task
 * assignment entities. And simple check to see if task has any assignments
 */
public interface TaskAssignmentFacade {
    boolean existsByTaskBodyId(Long taskBodyId);

    Optional<TaskAssignmentDetailDTO> findAssignmentById(Long taskAssignmentId);
}
