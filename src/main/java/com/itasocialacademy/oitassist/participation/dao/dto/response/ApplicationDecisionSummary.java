package com.itasocialacademy.oitassist.participation.dao.dto.response;

import java.time.Instant;

public record ApplicationDecisionSummary(
    Long competitionId,
    Long stageId,
    Long processedBy,
    Instant processedAt) {
}
