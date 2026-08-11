package com.itasocialacademy.oitassist.taskassignment.dto.request;

import com.itasocialacademy.oitassist.taskassignment.dao.enums.AssignmentVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "DTO for creating a new task assignment")
public record CreateTaskAssignmentRequestDTO(
    @Schema(
        description = "Unique identifier of the task body to assign",
        example = "3",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Long taskBodyId,

    @Schema(
        description = "Visibility of the assignment",
        example = "VISIBLE",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED) AssignmentVisibility visibility,

    @Schema(
        description = "Maximum points that can be earned for this assignment",
        example = "25",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotNull @Min(1) @Max(120) Integer maxPoints,

    @Schema(
        description = "Requirements and constraints for submitted files",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotNull @Valid TaskRequirementsRequestDTO requirements) {
}
