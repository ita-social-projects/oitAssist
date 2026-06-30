package com.itasocialacademy.oitassist.export.dao.dto;

import java.util.List;

public record StageResult(
    String stageTitle,
    int stageScore,
    List<TourResult> tours) {
}
