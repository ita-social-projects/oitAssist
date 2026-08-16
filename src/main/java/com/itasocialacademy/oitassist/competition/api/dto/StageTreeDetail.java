package com.itasocialacademy.oitassist.competition.api.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record StageTreeDetail(
    StageDetail stage,
    List<TourDetail> tours) {
}