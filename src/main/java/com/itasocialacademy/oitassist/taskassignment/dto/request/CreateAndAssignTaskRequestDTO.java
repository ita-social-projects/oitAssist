package com.itasocialacademy.oitassist.taskassignment.dto.request;

import com.itasocialacademy.oitassist.taskassignment.dao.enums.AssignmentVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

@Schema(description = "DTO for creating a new task and assigning it to a tour in a single operation")
public record CreateAndAssignTaskRequestDTO(
    @Schema(
        description = "Title of the task to create",
        example = "Excel Задача про графи",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String title,

    @Schema(
        description = "Detailed description of the task",
        example = "Створити у файлі-розв'язку планарний граф ...",
        requiredMode = Schema.RequiredMode.REQUIRED) String description,

    @Schema(
        description = "List of file ids to attach to the task",
        example = "[1, 2]",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotEmpty List<Long> fileIds,

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
