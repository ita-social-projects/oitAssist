package com.itasocialacademy.oitassist.evaluation.api.dto;

import java.util.List;

public record StageResult(
    String stageTitle,
    Integer stageScore,
    List<TourResult> tours) {
}
