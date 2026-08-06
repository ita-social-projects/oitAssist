package com.itasocialacademy.oitassist.task.api.dto;

import java.util.Set;
import lombok.Builder;

/**
 * DTO representing the body of a task for cross-module communication.
 */
@Builder
public record TaskBodyDetail(
    Long id,
    String title,
    String description,
    Set<Long> ownerIds) {
}
