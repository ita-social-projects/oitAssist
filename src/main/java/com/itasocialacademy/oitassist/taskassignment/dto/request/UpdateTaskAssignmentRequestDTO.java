package com.itasocialacademy.oitassist.taskassignment.dto.request;

import com.itasocialacademy.oitassist.taskassignment.dao.enums.AssignmentVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "DTO for updating an existing task assignment")
public record UpdateTaskAssignmentRequestDTO(
    @Schema(
        description = "Visibility of the assignment",
        example = "HIDDEN",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED) AssignmentVisibility visibility,

    @Schema(
        description = "Maximum points that can be earned for this assignment",
        example = "30",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED) @Min(1) @Max(120) Integer maxPoints,

    @Schema(
        description = "Requirements and constraints for submitted files",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED) @Valid TaskRequirementsRequestDTO requirements) {
}
