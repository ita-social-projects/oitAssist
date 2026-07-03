package com.itasocialacademy.oitassist.competition.api.dto;

import com.itasocialacademy.oitassist.competition.dao.dto.response.StageResponse;
import com.itasocialacademy.oitassist.competition.dao.dto.response.TourResponse;
import java.util.List;
import lombok.Builder;

@Builder
public record StageTreeResponse(
    StageResponse stage,
    List<TourResponse> tours) {
}