package com.itasocialacademy.oitassist.competition.dto.request;

import com.itasocialacademy.oitassist.competition.dto.validation.HasDateRange;
import com.itasocialacademy.oitassist.competition.dto.validation.ValidDateRange;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;

@Schema(description = "DTO for creating a new Tour within a Stage. "
    + "Tour dates must fall within the parent Stage's date range.")
@ValidDateRange
public record CreateTourRequest(
    @Schema(
        description = "Title of the tour",
        example = "Тур 1: Алгоритми та структури даних",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String title,

    @Schema(
        description = "Detailed description of the tour",
        example = "Розв'язання 5 алгоритмічних задач за 3 години") String description,

    @Schema(
        description = "Start date and time of the tour. Must not be before the parent Stage's start date.",
        example = "2026-09-01T10:00:00Z",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotNull ZonedDateTime dateStart,

    @Schema(
        description = "End date and time of the tour. Must not be after the parent Stage's end date.",
        example = "2026-09-01T13:00:00Z",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotNull ZonedDateTime dateFinish,

    @Schema(
        description = "Explicit sort position of the tour within the stage. "
            + "If omitted, the tour is appended after the last existing tour.",
        example = "1",
        minimum = "1",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED) @Min(1) Short sortPosition,

    @Schema(
        description = "Physical or virtual location where the tour takes place",
        example = "Actual online platform") @NotBlank String location) implements HasDateRange {
}
