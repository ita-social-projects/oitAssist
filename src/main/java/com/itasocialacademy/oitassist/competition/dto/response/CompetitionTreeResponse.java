package com.itasocialacademy.oitassist.competition.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Schema(description = "DTO representing a Competition tree")
@Builder
public record CompetitionTreeResponse(
    CompetitionResponse competition,
    List<StageTreeResponse> stages) {
}