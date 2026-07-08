package com.itasocialacademy.oitassist.competition.api.dto;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import java.time.ZonedDateTime;
import lombok.Builder;

/**
 * Full detail view of a Competition, exposed to other modules (e.g.
 * {@code participation}) via {@code CompetitionFacade}.
 */
@Builder
public record CompetitionDetail(
    Long id,
    String title,
    String description,
    ZonedDateTime dateStart,
    ZonedDateTime dateFinish,
    CompetitionStatus competitionStatus,
    Long createdBy,
    Long updatedBy) {
}
