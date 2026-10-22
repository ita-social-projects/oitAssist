package com.itasocialacademy.oitassist.task.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "DTO for creating a new task")
public record CreateTaskRequestDTO(
    @Schema(
        description = "Title of the task",
        example = "PowerPoint Різдвяна зірка",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String title,

    @Schema(
        description = "Optional description of the task",
        example = "Cтворити у файлі-розв’язку на одному слайді ...",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED) String description) {
}
