package com.itasocialacademy.oitassist.competition.dto.response;

import com.itasocialacademy.oitassist.competition.dao.enums.StageScope;
import com.itasocialacademy.oitassist.competition.dao.enums.StageStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.ZonedDateTime;
import lombok.Builder;

@Schema(description = "DTO representing a Stage entity response")
@Builder
public record StageResponse(
    Long id,
    Long competitionId,
    String title,
    String description,
    ZonedDateTime dateStart,
    ZonedDateTime dateFinish,
    Short sortPosition,
    StageScope scope,
    StageStatus status,
    Long version) {
}
