package com.itasocialacademy.oitassist.competition.dto.response;

import java.time.ZonedDateTime;
import lombok.Builder;

@Builder
public record TourResponse(
    Long id,
    Long stageId,
    String title,
    String description,
    ZonedDateTime dateStart,
    ZonedDateTime dateFinish,
    Short sortPosition,
    String location) {
}
