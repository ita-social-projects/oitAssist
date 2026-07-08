package com.itasocialacademy.oitassist.competition.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record StageTreeResponse(
    StageResponse stage,
    List<TourResponse> tours) {
}