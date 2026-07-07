package com.itasocialacademy.oitassist.task.service.interfaces;

import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.task.dto.request.CreateTaskRequestDTO;
import com.itasocialacademy.oitassist.task.dto.response.TaskResponseDTO;
import com.itasocialacademy.oitassist.task.exceptions.TaskNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskService {
    /**
     * Creates a new task based on the provided request data.
     *
     * @param requestDTO the request containing task details
     * @return the created task as a {@link TaskResponseDTO}
     */
    TaskResponseDTO createTask(CreateTaskRequestDTO requestDTO);

    /**
     * Retrieves a task by its id.
     *
     * @param id the unique identifier of the task to retrieve
     * @return {@link TaskResponseDTO}
     * @throws TaskNotFoundException if no task with the specified ID exists
     */
    TaskResponseDTO getTaskById(Long id);

    /**
     * Returns all tasks in the system across all users, respecting the pagination
     * and sorting criteria provided.
     *
     * @param pageable pagination and sorting criteria
     * @return a page of tasks according to the specified pagination parameters
     */
    Page<TaskResponseDTO> getAllTasks(Pageable pageable);

    /**
     * Retrieves all tasks belonging to the currently authenticated user with pagination support.
     *
     * @param pageable pagination and sorting criteria
     * @return a page of tasks for the current user according to the specified pagination parameters
     * @throws AuthorizationException if the user is not authenticated or logged in
     */
    Page<TaskResponseDTO> getAllMyTasks(Pageable pageable);
}
