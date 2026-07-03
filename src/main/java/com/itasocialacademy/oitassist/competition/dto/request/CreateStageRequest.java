package com.itasocialacademy.oitassist.competition.dto.request;

import com.itasocialacademy.oitassist.competition.dto.validation.HasDateRange;
import com.itasocialacademy.oitassist.competition.dto.validation.ValidDateRange;
import com.itasocialacademy.oitassist.competition.dao.enums.StageScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;

@Schema(description = "DTO for creating a new Stage within a Competition. "
    + "Stage dates must fall within the parent Competition's date range.")
@ValidDateRange
public record CreateStageRequest(
    @Schema(
        description = "Title of the stage",
        example = "Регіональний етап",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String title,

    @Schema(
        description = "Detailed description of the stage",
        example = "Останній етап олімпіади") String description,

    @Schema(
        description = "Start date and time of the stage. Must not be before the parent Competition's start date.",
        example = "2026-09-01T09:00:00Z",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotNull ZonedDateTime dateStart,

    @Schema(
        description = "End date and time of the stage. Must not be after the parent Competition's end date.",
        example = "2026-09-01T18:00:00Z",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotNull ZonedDateTime dateFinish,

    @Schema(
        description = "Explicit sort position of the stage within the competition. "
            + "If omitted, the stage is appended after the last existing stage.",
        example = "1",
        minimum = "1",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED) @Min(1) Short sortPosition,

    @Schema(
        description = "Scope of the stage.",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED) @NotNull StageScope scope) implements HasDateRange {
}
