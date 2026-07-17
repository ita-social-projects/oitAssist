package com.itasocialacademy.oitassist.competition.dto.request;

import com.itasocialacademy.oitassist.competition.dao.enums.StageStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "DTO for changing the lifecycle status of a Stage")
public record ChangeStageStatusRequest(
    @Schema(description = "The new status of the stage", example = "IN_PROGRESS",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(
            message = "Stage status must not be null") StageStatus status) {
}
