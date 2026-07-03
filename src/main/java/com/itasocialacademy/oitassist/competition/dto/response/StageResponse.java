package com.itasocialacademy.oitassist.competition.dto.response;

import com.itasocialacademy.oitassist.competition.dao.enums.StageScope;
import java.time.ZonedDateTime;
import lombok.Builder;

@Builder
public record StageResponse(
    Long id,
    Long competitionId,
    String title,
    String description,
    ZonedDateTime dateStart,
    ZonedDateTime dateFinish,
    Short sortPosition,
    StageScope scope) {
}
