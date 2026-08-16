package com.itasocialacademy.oitassist.participation.dao.dto.response;

import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import java.time.Instant;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class ProcessEnrollmentResponse {
    @Schema(description = "Unique identifier of the enrollment request", example = "1")
    Long id;
    @Schema(description = "Unique identifier of the competition", example = "1")
    Long competitionId;
    @Schema(description = "Unique identifier of the stage", example = "1")
    Long stageId;
    @Schema(description = "ID of the user who issued the request", example = "5")
    Long issuedBy;
    @Schema(description = "Request creation date", example = "2026-06-07T09:50:30Z")
    Instant issuedAt;
    @Schema(description = "Request updating date", example = "2026-06-07T10:12:00Z")
    Instant processedAt;
    @Schema(description = "Updated request status", example = "ACCEPTED")
    RequestStatus status;
    @Schema(description = "Request rejection reason (optional)")
    String rejectionReason;
}
