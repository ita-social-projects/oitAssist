package com.itasocialacademy.oitassist.competition.dto.response;

import com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus;
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
    String location,
    ExecutionStatus executionStatus) {
}
