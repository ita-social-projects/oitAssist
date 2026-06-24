package com.itasocialacademy.oitassist.competition.dao.dto.response;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import java.time.ZonedDateTime;
import lombok.Builder;

@Builder
public record CompetitionResponse(
    Long id,
    String title,
    String description,
    ZonedDateTime dateStart,
    ZonedDateTime dateFinish,
    CompetitionStatus status,
    Long createdBy,
    Long updatedBy) {
}