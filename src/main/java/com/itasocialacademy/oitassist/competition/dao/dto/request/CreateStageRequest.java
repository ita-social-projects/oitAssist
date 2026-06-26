package com.itasocialacademy.oitassist.competition.dao.dto.request;

import com.itasocialacademy.oitassist.competition.dao.enums.StageScope;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;

public record CreateStageRequest(
    @NotBlank String title,
    String description,
    @NotNull ZonedDateTime dateStart,
    @NotNull ZonedDateTime dateFinish,
    @Min(1) Short sortPosition,
    StageScope scope) {
}
