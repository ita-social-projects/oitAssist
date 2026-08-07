package com.itasocialacademy.oitassist.competition.api.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record CompetitionTreeDetail(
    CompetitionDetail competition,
    List<StageTreeDetail> stages) {
}
