package com.itasocialacademy.oitassist.taskassignment.dto.response;

import com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "DTO representing linked tour details of some given task")
public record LinkedToursResponseDTO(
    @Schema(
        description = "Id of the tour the task is linked to",
        example = "1") Long tourId,

    @Schema(
        description = "Title of the tour",
        example = "Тур 1: Алгоритми та структури даних") String title,

    @Schema(
        description = "Description of the tour",
        example = "Розв'язати алгоритмічні задачі") String description,

    @Schema(
        description = "Physical or virtual location where the tour takes place",
        example = "Ліцей №1, Львів") String location,

    @Schema(
        description = "The current state of execution ",
        example = "IN_PROGRESS") ExecutionStatus executionStatus) {
}
