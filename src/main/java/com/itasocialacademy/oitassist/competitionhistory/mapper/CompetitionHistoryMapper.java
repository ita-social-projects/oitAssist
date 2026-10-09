package com.itasocialacademy.oitassist.competitionhistory.mapper;

import com.itasocialacademy.oitassist.competition.api.dto.CompetitionDetail;
import com.itasocialacademy.oitassist.competitionhistory.dto.response.CompetitionHistoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface CompetitionHistoryMapper {
    CompetitionHistoryResponse toResponse(CompetitionDetail detail);
}
