package com.itasocialacademy.oitassist.participation.dao.dto.response;

import com.itasocialacademy.oitassist.competition.api.dto.CompetitionDetail;
import com.itasocialacademy.oitassist.competition.api.dto.StageDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "Олімпіада, відкрита для реєстрації учнів")
@Builder
public record OpenCompetitionEnrollmentResponse(
    @Schema(description = "Дані олімпіади") CompetitionDetail competition,

    @Schema(description = "Дані початкового етапу, на який відкрита реєстрація") StageDetail stage,

    @Schema(
        description = "Інформація про подану заявку учня") UserApplicationSummary userApplication,

    @Schema(description = "Чи зарахований учень як офіційний учасник цього етапу",
        example = "false") boolean isParticipant) {
}
