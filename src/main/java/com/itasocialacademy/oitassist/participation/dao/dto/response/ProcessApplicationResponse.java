package com.itasocialacademy.oitassist.participation.dao.dto.response;

import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Builder;

@Builder
@Schema(description = "DTO representing an Application processing response")
public record ProcessApplicationResponse(
    @Schema(description = "Unique identifier of the application", example = "1") Long id,
    @Schema(description = "Unique identifier of the competition", example = "1") Long competitionId,
    @Schema(description = "Unique identifier of the stage", example = "1") Long stageId,
    @Schema(description = "ID of the user who issued the application", example = "5") Long issuedBy,
    @Schema(description = "Application creation date", example = "2026-06-07T09:50:30Z") Instant issuedAt,
    @Schema(description = "ID of the user who last updated the application", example = "5") Long processedBy,
    @Schema(description = "Application updating date", example = "2026-06-07T10:12:00Z") Instant processedAt,
    @Schema(description = "Current application status", example = "ACCEPTED") RequestStatus status,
    @Schema(description = "Application rejection reason (optional)",
        example = "User has invalid profile information") String rejectionReason) {
}
