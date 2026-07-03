package com.itasocialacademy.oitassist.competition.dto.request;

import com.itasocialacademy.oitassist.competition.dto.validation.HasDateRange;
import com.itasocialacademy.oitassist.competition.dto.validation.ValidDateRange;
import com.itasocialacademy.oitassist.competition.dao.enums.StageScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;

@Schema(description = "DTO for updating an existing Stage")
@ValidDateRange
public record UpdateStageRequest(
    @Schema(description = "Title of the stage", example = "Оновлений Етап",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String title,

    @Schema(description = "Description of the stage", example = "Новий детальний опис") String description,

    @Schema(description = "Start date and time", example = "2026-09-01T09:00:00Z",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotNull ZonedDateTime dateStart,

    @Schema(description = "End date and time", example = "2026-09-05T18:00:00Z",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotNull ZonedDateTime dateFinish,

    @Schema(description = "Scope of the stage", example = "NATIONAL",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotNull StageScope scope,

    @Schema(description = "Order of the stage in the hierarchy", example = "2")
    @Min(1) Short sortPosition) implements HasDateRange {
}