package com.itasocialacademy.oitassist.participation.dao.dto.response;

import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "DTO representing the each invitation's details in the list.")
public record InvitationListItemResponse(
    Long invitationId,
    Instant issuedAt,
    RequestStatus status,
    UserSummary user) {
}
