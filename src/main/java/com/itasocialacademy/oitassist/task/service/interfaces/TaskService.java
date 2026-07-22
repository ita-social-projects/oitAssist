package com.itasocialacademy.oitassist.task.service.interfaces;

import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.task.api.dto.TaskBodyDetail;
import com.itasocialacademy.oitassist.task.dto.request.ChangeOwnerRequestDTO;
import com.itasocialacademy.oitassist.task.dto.request.CreateTaskRequestDTO;
import com.itasocialacademy.oitassist.task.dto.request.UpdateTaskRequestDTO;
import com.itasocialacademy.oitassist.task.dto.response.TaskResponseDTO;
import com.itasocialacademy.oitassist.task.exceptions.TaskAccessRestrictedException;
import com.itasocialacademy.oitassist.task.exceptions.TaskNotFoundException;
import com.itasocialacademy.oitassist.user.exceptions.UserNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
     * Retrieves all tasks belonging to the currently authenticated user with
     * pagination support.
     *
     * @param pageable pagination and sorting criteria
     * @return a page of tasks for the current user according to the specified
     *         pagination parameters
     * @throws AuthorizationException if the user is not authenticated or logged in
     */
    Page<TaskResponseDTO> getAllMyTasks(Pageable pageable);

    /**
     * Updates a task's title, description and manages file attachments. Only the
     * task owner or an admin can update a task.
     *
     * @param taskId     the task id to update
     * @param requestDTO the updated task data and file ids
     * @return the updated task
     * @throws TaskNotFoundException         if the task does not exist
     * @throws TaskAccessRestrictedException if the user is not authorized to update
     *                                       this task
     */
    TaskResponseDTO updateTask(Long taskId, UpdateTaskRequestDTO requestDTO);

    /**
     * Changes the owner of a task to a new user. The new owner must have ADMIN or
     * ORG role. Only users with ADMIN role can perform this operation.
     *
     * @param taskId             the ID of the task to reassign
     * @param changeOwnerRequest the request containing the new owner's email
     *                           address
     * @return the updated task with the new owner
     * @throws TaskNotFoundException if the task does not exist
     * @throws UserNotFoundException if the new owner user does not exist
     * @throws ValidationException   if the new owner does not have ADMIN or ORG
     *                               role
     */
    TaskResponseDTO changeTaskOwner(Long taskId, ChangeOwnerRequestDTO changeOwnerRequest);

    /**
     * Deletes a task by its id. Only the task owner or users with ADMIN role can
     * delete a task.
     *
     * @param taskId the ID of the task to delete
     * @throws TaskNotFoundException         if the task does not exist
     * @throws TaskAccessRestrictedException if the user is neither the task owner
     *                                       nor an ADMIN
     */
    void deleteTask(Long taskId);

    /**
     * Retrieves the body information of a task by its id.
     *
     * @param taskId the unique identifier of the task to retrieve
     * @return an {@link Optional} containing the {@link TaskBodyDetail} if the task
     *         exists, or an empty {@code Optional} if no task is found for the
     *         given id
     */
    Optional<TaskBodyDetail> getTaskBodyDetailById(Long taskId);

    /**
     * Retrieves a mapping of task IDs to their corresponding titles.
     *
     * <p>
     * If the provided list of IDs is {@code null} or empty, this method immediately
     * returns an empty map without querying the database.
     * </p>
     *
     * @param taskIds a list of task ids whose titles are to be fetched
     * @return a {@link Map} containing the task ids as keys and their titles as
     *         values. Tasks from the list that are not found in the database will
     *         simply not be included in the map.
     */
    Map<Long, String> getTaskTitlesByIds(List<Long> taskIds);
}
