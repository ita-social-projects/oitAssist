package com.itasocialacademy.oitassist.competition.dao.dto.request;

import java.time.ZonedDateTime;

public record CreateTourRequest(
    String title,
    String description,
    ZonedDateTime dateStart,
    ZonedDateTime dateFinish,
    Short sortPosition,
    String location
) {}
