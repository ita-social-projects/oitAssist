package com.itasocialacademy.oitassist.export.dao.dto;

import java.util.List;

public record ParticipantResult(
    String participantName,
    int totalScore,
    List<StageResult> stages) {
}
