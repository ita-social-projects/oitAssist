package com.itasocialacademy.oitassist.task.dto.request;

import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

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
        description = "File ids to be detached from task",
        example = "[52]",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED) List<Long> removedFileIds,

    @Schema(
        description = "Map of existing file ID → new FileRole for role updates",
        example = "{\"51\": \"SOLUTION\"}",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED) Map<Long, FileRole> roleUpdates,

    @Schema(description = "Optimistic locking version; must be echoed back on updates",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Long version) {
}
