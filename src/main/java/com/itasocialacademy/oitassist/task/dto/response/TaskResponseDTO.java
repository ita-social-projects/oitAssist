package com.itasocialacademy.oitassist.task.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "DTO representing a Task entity response")
@Builder
public record TaskResponseDTO(
    @Schema(
        description = "Unique identifier of the task",
        example = "3") Long id,

    @Schema(
        description = "Title of the task",
        example = "PowerPoint Різдвяна зірка") String title,

    @Schema(
        description = "Description of the task",
        example = "Cтворити у файлі-розв’язку на одному слайді ...") String description,

    @Schema(
        description = "Id of task creator",
        example = "1") Long createdBy,

    @Schema(
        description = "Id of task's current owner",
        example = "5") Long ownerId) {
}
