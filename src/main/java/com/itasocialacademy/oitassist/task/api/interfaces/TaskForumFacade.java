package com.itasocialacademy.oitassist.task.api.interfaces;

import com.itasocialacademy.oitassist.task.api.dto.TaskForumContext;
import org.springframework.modulith.NamedInterface;

/**
 * Public task-module contract used by task-bound forum functionality.
 *
 * <p>
 * The current implementation uses TaskBody as a temporary forum context.
 * Assignment-specific access will be introduced when TaskAssignment becomes
 * available.
 * </p>
 */
@NamedInterface("TaskForumFacade")
public interface TaskForumFacade {
    /**
     * Returns a safe task projection for forum use cases.
     *
     * @param taskId task identifier
     * @return safe task forum context
     */
    TaskForumContext getForumContext(Long taskId);

    /**
     * TODO change after TaskAssignment is implemented. Checks whether the specified
     * user may access the forum associated with the task.
     *
     * <p>
     * Until TaskAssignment is implemented, every authenticated user may access the
     * forum of an existing task.
     * </p>
     *
     * @param taskId task identifier
     * @param userId authenticated user identifier
     * @return {@code true} when the task exists and the user is authenticated
     */
    boolean canUserAccessForum(Long taskId, Long userId);
}