package com.itasocialacademy.oitassist.taskassignment.dto.response;

import com.itasocialacademy.oitassist.taskassignment.dao.enums.AssignmentVisibility;
import com.itasocialacademy.oitassist.taskassignment.dao.model.TaskRequirements;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "DTO representing a task assignment response")
public record TaskAssignmentResponseDTO(
    @Schema(
        description = "Unique identifier of the task assignment",
        example = "1") Long id,

    @Schema(
        description = "Id of the associated task body",
        example = "3") Long taskBodyId,

    @Schema(
        description = "Title of the task",
        example = "PowerPoint Різдвяна зірка") String taskTitle,

    @Schema(
        description = "Id of the tour this assignment belongs to",
        example = "2") Long tourId,

    @Schema(
        description = "Visibility level of the assignment",
        example = "VISIBLE") AssignmentVisibility visibility,

    @Schema(
        description = "Maximum points achievable for this assignment",
        example = "30") Integer maxPoints,

    @Schema(
        description = "File requirements and constraints for submissions") TaskRequirements requirements,

    @Schema(
        description = "Id of the user who created this assignment",
        example = "3") Long createdBy) {
}
