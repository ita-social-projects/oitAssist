package com.itasocialacademy.oitassist.competition.dto.request;

import com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "DTO for changing the lifecycle status of a Tour")
public record ChangeTourStatusRequest(
    @Schema(description = "The new execution status of the tour", example = "IN_PROGRESS",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(
            message = "Execution status must not be null") ExecutionStatus status) {
}