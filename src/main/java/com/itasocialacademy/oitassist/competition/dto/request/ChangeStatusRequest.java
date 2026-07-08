package com.itasocialacademy.oitassist.competition.dto.request;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "DTO for changing the lifecycle status of a Competition")
public record ChangeStatusRequest(
    @Schema(
        description = "The target status to transition the competition into (e.g., PUBLISHED, FINISHED, ARCHIVED)",
        example = "PUBLISHED",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(
            message = "New status cannot be null") CompetitionStatus newStatus) {
}