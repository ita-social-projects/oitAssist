package com.itasocialacademy.oitassist.competition.api.dto;

import lombok.Builder;

/**
 * Details of an open competition and its initial enrollment stage, exposed to
 * other modules via {@code CompetitionFacade}.
 */
@Builder
public record EnrollmentCompetitionStageDetail(
    CompetitionDetail competition,
    StageDetail stage) {
}
