package com.itasocialacademy.oitassist.task.api.dto;

import lombok.Builder;
import org.springframework.modulith.NamedInterface;

/**
 * Minimal task projection exposed to use inside forum (chat) module.
 *
 * <p>
 * This is a temporary TaskBody-based forum context. It will be replaced by a
 * TaskAssignment-based context when TaskAssignment is implemented.
 * </p>
 */
@Builder
@NamedInterface("TaskForumContext")
public record TaskForumContext(
    Long taskId,
    String title,
    Long ownerId) {
}