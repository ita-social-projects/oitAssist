package com.itasocialacademy.oitassist.competition.dao.dto.request;

import java.time.ZonedDateTime;

public record CreateCompetitionRequest(
    String title,
    String description,
    ZonedDateTime dateStart,
    ZonedDateTime dateFinish
) {}
