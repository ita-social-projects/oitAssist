package com.itasocialacademy.oitassist.competition.dao.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.ZonedDateTime;

public record CreateCompetitionRequest(
    @NotBlank String title,
    String description,
    @NotNull ZonedDateTime dateStart,
    @NotNull ZonedDateTime dateFinish) {
}
