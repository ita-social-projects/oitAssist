package com.itasocialacademy.oitassist.competition.dto.request;

import com.itasocialacademy.oitassist.competition.dto.validation.HasDateRange;
import com.itasocialacademy.oitassist.competition.dto.validation.ValidDateRange;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;

@Schema(description = "DTO for updating an existing Tour")
@ValidDateRange
public record UpdateTourRequest(
    @Schema(description = "Title of the tour", example = "Оновлений Тур",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String title,

    @Schema(description = "Description of the tour", example = "Новий детальний опис туру") String description,

    @Schema(description = "Start date and time", example = "2026-09-01T09:00:00Z",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotNull ZonedDateTime dateStart,

    @Schema(description = "End date and time", example = "2026-09-02T18:00:00Z",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotNull ZonedDateTime dateFinish,

    @Schema(description = "Location for the tour", example = "Konotop city",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String location,

    @Schema(description = "Order of the tour in the hierarchy", example = "1")
    @Min(1) Short sortPosition) implements HasDateRange {
}