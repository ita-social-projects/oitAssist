package com.itasocialacademy.oitassist.participation.dao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "DTO for rejecting a list of Application requests.")
public record AcceptApplicationListRequest(
    @Schema(
        description = "List of applications' IDs",
        type = "array", example = "[1, 3, 4]",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotEmpty List<@NotNull Long> applicationIds) {
}
