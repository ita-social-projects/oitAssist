package com.itasocialacademy.oitassist.evaluation.api.dto;

import java.util.List;

public record ParticipantResult(
    String participantName,
    Integer totalScore,
    List<StageResult> stages) {
}
