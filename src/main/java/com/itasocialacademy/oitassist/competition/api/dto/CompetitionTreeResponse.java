package com.itasocialacademy.oitassist.competition.api.dto;

import com.itasocialacademy.oitassist.competition.dao.dto.response.CompetitionResponse;
import java.util.List;
import lombok.Builder;

@Builder
public record CompetitionTreeResponse(
    CompetitionResponse competition,
    List<StageTreeResponse> stages) {
}