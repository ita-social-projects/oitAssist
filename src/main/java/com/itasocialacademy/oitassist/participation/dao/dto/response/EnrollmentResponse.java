package com.itasocialacademy.oitassist.participation.dao.dto.response;

import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class EnrollmentResponse {
    @Schema(description = "Unique identifier of the application", example = "1")
    private Long id;
    @Schema(description = "Unique identifier of the competition", example = "1")
    private Long competitionId;
    @Schema(description = "Unique identifier of the stage", example = "1")
    private Long stageId;
    @Schema(description = "ID of the user who issued the application", example = "5")
    private Long issuedBy;
    @Schema(description = "Application creation date", example = "2026-06-07T09:50:30Z")
    private Instant issuedAt;
    @Schema(description = "Current application status", example = "ACCEPTED")
    private RequestStatus status;
}
