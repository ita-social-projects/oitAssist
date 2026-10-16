package com.itasocialacademy.oitassist.competitionhistory.mapper;

import com.itasocialacademy.oitassist.competition.api.dto.CompetitionDetail;
import com.itasocialacademy.oitassist.competitionhistory.dto.response.CompetitionHistoryResponse;
import com.itasocialacademy.oitassist.evaluation.api.dto.CompetitionScore;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface CompetitionHistoryMapper {
    @Mapping(target = "score", source = "score.score")
    @Mapping(target = "maxScore", source = "score.maxScore")
    CompetitionHistoryResponse toResponse(CompetitionDetail detail, CompetitionScore score);
}