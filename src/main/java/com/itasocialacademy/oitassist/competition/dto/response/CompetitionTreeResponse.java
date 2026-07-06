package com.itasocialacademy.oitassist.competition.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record CompetitionTreeResponse(
    CompetitionResponse competition,
    List<StageTreeResponse> stages) {
}