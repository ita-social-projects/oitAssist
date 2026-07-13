package com.itasocialacademy.oitassist.participation.dao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
@Schema(description = "DTO for creating a new Application. "
    + "Newly created applications will initially have the PENDING status.")
public record CreateApplicationRequest(
    @Schema(
        description = "Unique identifier of the competition",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Long competitionId,
    @Schema(
        description = "Unique identifier of the stage",
        example = "2",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Long stageId) {
}
