package com.itasocialacademy.oitassist.taskassignment.api;

import com.itasocialacademy.oitassist.taskassignment.api.dto.TaskAssignmentDetailDTO;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Facade providing operations for managing task assignments from other modules.
 * Handles retrieval of task assignments by id or by a collection of tour ids, *
 * and a simple check to see if a task has any assignments.
 */
public interface TaskAssignmentFacade {
    boolean existsByTaskBodyId(Long taskBodyId);

    Optional<TaskAssignmentDetailDTO> findAssignmentById(Long taskAssignmentId);

    /**
     * Retrieves detailed information about all task assignments that belong to any
     * of the given tours.
     *
     * @param tourIds the ids of the tours to retrieve assignments for; an empty
     *                collection results in an empty list
     * @return a list of {@link TaskAssignmentDetailDTO} for all assignments of the
     *         given tours, or an empty list if there are none
     */
    List<TaskAssignmentDetailDTO> findAssignmentsByTourIds(Collection<Long> tourIds);
}