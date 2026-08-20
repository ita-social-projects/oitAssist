package com.itasocialacademy.oitassist.participation.dao.dto.response;

import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "DTO representing the each application request's details in the list.")
public record ApplicationListItemResponse(
    Long applicationId,
    Instant issuedAt,
    RequestStatus status,
    UserSummary summary) {
}
