package com.itasocialacademy.oitassist.participation.dao.dto.response;

import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "DTO representing a single invitation in the student's enrollment history.")
public record InvitationHistoryItemResponse(
    @Schema(description = "Unique identifier of the invitation", example = "1") Long invitationId,
    @Schema(description = "Invitation creation date", example = "2026-06-07T09:50:30Z") Instant issuedAt,
    @Schema(description = "Current invitation status", example = "PENDING") RequestStatus status,
    CompetitionSummary competition,
    StageSummary stage) {
}