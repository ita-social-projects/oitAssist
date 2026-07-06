package com.itasocialacademy.oitassist.competition.api.dto;

import com.itasocialacademy.oitassist.competition.dao.enums.StageScope;
import java.time.ZonedDateTime;
import lombok.Builder;

/**
 * Full detail view of a Stage, exposed to other modules via {@code CompetitionFacade}.
 */
@Builder
public record StageDetail(
    Long id,
    Long competitionId,
    String title,
    String description,
    ZonedDateTime dateStart,
    ZonedDateTime dateFinish,
    Short sortPosition,
    StageScope scope,
    Long createdBy,
    Long updatedBy) {
}
