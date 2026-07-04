package com.itasocialacademy.oitassist.task.dao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "DTO for for creating a new task")
public record CreateTaskRequestDTO(
    @Schema(
        description = "Title of the task",
        example = "PowerPoint Різдвяна зірка",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank String title,

    @Schema(
        description = "Optional description of the task",
        example = "Cтворити у файлі-розв’язку на одному слайді ...",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    String description,

    @Schema(
        description = "IDs of files already uploaded (in a temporary, unattached state) via the file management "
            + "module, to be attached to this task once it's created",
        example = "[51, 52]",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotEmpty(message = "At least one file must be provided") List<Long> fileIds
) {
}
