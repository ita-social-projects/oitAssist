package com.itasocialacademy.oitassist.competition.dao.dto;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionLevel;
import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record CompetitionFilter(
    @Schema(description = "search by name", example = "Competition") String search,
    @Schema(description = "filter by level", examples = {
        "CITY", "REGION", "NATIONAL", "OPEN"}) CompetitionLevel level,
    @Schema(description = "filter by status",
        examples = {"INCOMING", "INPROGRESS", "FINISED", "ARCHIVED"}) CompetitionStatus status,
    @Schema(description = "filter by year", example = "2026") Integer year){

}
