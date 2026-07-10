package com.itasocialacademy.oitassist.participation.dao.dto.response;

import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Builder;

@Builder
@Schema(description = "DTO representing an Application creation response")
public record CreateApplicationResponse(
    @Schema(description = "Unique identifier of the application", example = "1") Long id,
    @Schema(description = "Unique identifier of the competition", example = "1") Long competitionId,
    @Schema(description = "Unique identifier of the stage", example = "1") Long stageId,
    @Schema(description = "ID of the user who issued the application", example = "5") Long issuedBy,
    @Schema(description = "Application creation date", example = "2026-06-07T09:50:30Z") Instant issuedAt,
    @Schema(description = "Current application status", example = "ACCEPTED") RequestStatus status) {
}
