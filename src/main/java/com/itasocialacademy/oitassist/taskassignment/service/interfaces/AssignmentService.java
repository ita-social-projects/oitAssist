package com.itasocialacademy.oitassist.taskassignment.service.interfaces;

import com.itasocialacademy.oitassist.competition.exceptions.TourNotFoundException;
import com.itasocialacademy.oitassist.taskassignment.api.dto.TaskAssignmentDetailDTO;
import com.itasocialacademy.oitassist.taskassignment.dao.enums.AssignmentVisibility;
import com.itasocialacademy.oitassist.taskassignment.dto.request.CreateAndAssignTaskRequestDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.request.CreateTaskAssignmentRequestDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.request.UpdateTaskAssignmentRequestDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.response.DetailedTaskAssignmentResponseDTO;
import com.itasocialacademy.oitassist.taskassignment.dto.response.TaskAssignmentResponseDTO;
import com.itasocialacademy.oitassist.taskassignment.exceptions.TaskAlreadyAssignedException;
import com.itasocialacademy.oitassist.taskassignment.exceptions.TaskAssignmentNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface AssignmentService {
    /**
     * Assigns a task to a specific tour. If no visibility level is provided in the
     * request, it defaults to {@link AssignmentVisibility#HIDDEN}.
     *
     * @param tourId  the id of the tour to assign the task to
     * @param request the task assignment creation request containing task body id,
     *                visibility level, and other assignment details
     * @return the created {@link DetailedTaskAssignmentResponseDTO} with the assigned task
     *         details
     * @throws TaskAlreadyAssignedException if the task is already assigned to the
     *                                      specified tour
     * @throws TourNotFoundException        if the tour or task does not exist
     */
    DetailedTaskAssignmentResponseDTO assignTask(Long tourId, CreateTaskAssignmentRequestDTO request);

    /**
     * Retrieves all task assignments for a specific tour with pagination support.
     *
     * @param pageable the pagination and sorting parameters
     * @param tourId   the id of the tour to retrieve assignments for
     * @return a {@link Page} containing {@link TaskAssignmentResponseDTO} objects
     *         for the specified tour
     * @throws TourNotFoundException if the tour does not exist
     */
    Page<TaskAssignmentResponseDTO> getAssignmentsByTourId(Pageable pageable, Long tourId);

    /**
     * Retrieves a task assignment by its id.
     *
     * @param taskAssignmentId the id of the task assignment to retrieve
     * @return the {@link DetailedTaskAssignmentResponseDTO} with the assignment
     *         details
     * @throws TaskAssignmentNotFoundException if no task assignment exists with the
     *                                         given id
     */
    DetailedTaskAssignmentResponseDTO getTaskAssignmentById(Long taskAssignmentId);

    /**
     * Updates an existing task assignment with new values. Allows partial updates
     * of task assignment properties. Only non-null fields in the request DTO are
     * applied to the assignment.
     *
     * @param taskAssignmentId the id of the task assignment to update
     * @param request          the update request containing the new values for the
     *                         assignment;
     * @return the updated {@link DetailedTaskAssignmentResponseDTO} with the new assignment
     *         details
     * @throws TaskAssignmentNotFoundException if no task assignment exists with the
     *                                         given id
     */
    DetailedTaskAssignmentResponseDTO updateTaskAssignment(Long taskAssignmentId,
        UpdateTaskAssignmentRequestDTO request);

    /**
     * Deletes a task assignment by its id.
     *
     * @param taskAssignmentId the id of the task assignment to delete
     * @throws TaskAssignmentNotFoundException if no task assignment exists with the
     *                                         given id
     */
    void deleteTaskAssignment(Long taskAssignmentId);

    /**
     * Creates a new task body and assigns it to the specified tour in a single
     * transactional operation. If no visibility level is provided, it defaults to
     * {@link AssignmentVisibility#HIDDEN}.
     *
     * @param tourId  the id of the tour to assign the newly created task to
     * @param request the request containing task body fields (title, description,
     *                file ids) and assignment fields (visibility, max points,
     *                requirements)
     * @return the created {@link DetailedTaskAssignmentResponseDTO} with the assignment
     *         details
     * @throws TourNotFoundException if the tour does not exist
     */
    DetailedTaskAssignmentResponseDTO createAndAssignTask(Long tourId, CreateAndAssignTaskRequestDTO request);

    /**
     * Retrieves detailed information about a task assignment by its id, intended
     * for cross-module communication via the facade layer.
     *
     * @param taskAssignmentId the id of the task assignment to retrieve
     * @return an {@link Optional} containing the {@link TaskAssignmentDetailDTO} if
     *         found, or empty if no assignment exists with the given id
     */
    Optional<TaskAssignmentDetailDTO> getTaskAssignmentDetailById(Long taskAssignmentId);

    /**
     * Checks whether a task assignment exists for the given task body id.
     *
     * @param taskBodyId the id of the task body to check
     * @return {@code true} if at least one assignment references the given task
     *         body id, {@code false} otherwise
     */
    boolean existsByTaskBodyId(Long taskBodyId);
}
