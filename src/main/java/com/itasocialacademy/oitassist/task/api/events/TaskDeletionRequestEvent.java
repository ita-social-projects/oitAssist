package com.itasocialacademy.oitassist.task.api.events;

/**
 * Event published before attempting to delete a task, to validate whether the
 * deletion should be permitted.
 *
 * @param taskBodyId the id of the task which is being deleted
 */
public record TaskDeletionRequestEvent(Long taskBodyId) {
}
