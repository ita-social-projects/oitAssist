package com.itasocialacademy.oitassist.participation.dao.dto.response;

import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "DTO representing the each invitation's details in the list.")
public record InvitationListItemResponse(
    @Schema(description = "Unique identifier of the request", example = "1") Long invitationId,
    @Schema(description = "Request creation date", example = "2026-06-07T09:50:30Z") Instant issuedAt,
    @Schema(description = "Current request status", example = "PENDING") RequestStatus status,
    UserSummary user) {
}
