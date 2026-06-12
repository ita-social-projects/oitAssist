package com.itasocialacademy.oitassist.competition.dao.dto.response;

import java.time.ZonedDateTime;

public record TourResponse(
    Long id,
    Long stageId,
    String title,
    String description,
    ZonedDateTime dateStart,
    ZonedDateTime dateFinish,
    Short sortPosition,
    String location
) {}
