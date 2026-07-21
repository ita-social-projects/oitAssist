package com.itasocialacademy.oitassist.task.api.dto;

import lombok.Builder;
import org.springframework.modulith.NamedInterface;

/**
 * DTO representing the body of a task for cross-module communication.
 */
@Builder
@NamedInterface("TaskBodyDetail")
public record TaskBodyDetail(
    Long id,
    String title,
    String description,
    Long ownerId) {
}
