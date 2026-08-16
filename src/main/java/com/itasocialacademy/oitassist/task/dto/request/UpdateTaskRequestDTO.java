package com.itasocialacademy.oitassist.task.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Schema(description = "DTO for updating an already existing task")
public record UpdateTaskRequestDTO(
    @Schema(
        description = "Updated title of the task",
        example = "Оновлена назва завдання",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String title,
    @Schema(
        description = "Updated description of the task",
        example = "Оновлений опис завдання",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED) String description,
    @Schema(
        description = "New and already attached ids of files",
        example = "[51,62]",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED) List<Long> fileIds,
    @Schema(
        description = "File ids to be detached from task",
        example = "[52]",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED) List<Long> removedFileIds) {
}
