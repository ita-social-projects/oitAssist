package com.itasocialacademy.oitassist.competition.dao.dto.request;

import com.itasocialacademy.oitassist.competition.dao.enums.StageScope;
import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;

public record CreateStageRequest(
    @NotNull String title,
    String description,
    ZonedDateTime dateStart,
    ZonedDateTime dateFinish,
    Short sortPosition,
    StageScope scope) {
}
