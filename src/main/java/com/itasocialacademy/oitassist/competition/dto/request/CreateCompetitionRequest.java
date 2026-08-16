package com.itasocialacademy.oitassist.competition.dto.request;

import com.itasocialacademy.oitassist.competition.dto.validation.HasDateRange;
import com.itasocialacademy.oitassist.competition.dto.validation.ValidDateRange;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;

@Schema(description = "DTO for creating a new Competition. Newly created competitions always receive DRAFT status.")
@ValidDateRange
public record CreateCompetitionRequest(
    @Schema(
        description = "Title of the competition",
        example = "Всеукраїнська Олімпіада з програмування 2026",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String title,

    @Schema(
        description = "Detailed description of the competition rules and subjects",
        example = "Олімпіада складатиметься з 3 етапів...") String description,

    @Schema(
        description = "Start date and time of the competition",
        example = "2026-09-01T09:00:00Z",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotNull ZonedDateTime dateStart,

    @Schema(
        description = "End date and time of the competition",
        example = "2026-09-02T09:00:00Z",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotNull ZonedDateTime dateFinish)
    implements HasDateRange {
}
