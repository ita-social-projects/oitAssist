package com.itasocialacademy.oitassist.competitionhistory.mapper;

import com.itasocialacademy.oitassist.competition.api.dto.CompetitionDetail;
import com.itasocialacademy.oitassist.competitionhistory.dto.response.CompetitionHistoryResponse;
import com.itasocialacademy.oitassist.evaluation.api.dto.CompetitionScore;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Maps competition data and the user's score to
 * {@link CompetitionHistoryResponse}.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface CompetitionHistoryMapper {
    /**
     * Builds a history entry from competition details and the user's score in that
     * competition.
     *
     * @param detail competition details
     * @param score  the user's score and the maximum possible score
     * @return the history entry
     */
    @Mapping(target = "score", source = "score.score")
    @Mapping(target = "maxScore", source = "score.maxScore")
    CompetitionHistoryResponse toResponse(CompetitionDetail detail, CompetitionScore score);
}