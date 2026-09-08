package com.itasocialacademy.oitassist.participation.dao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "DTO for rejecting a list of Application requests.")
public record RejectApplicationListRequest(
    @Schema(
        description = "List of applications' IDs",
        type = "array", example = "[1, 3, 4]",
        requiredMode = Schema.RequiredMode.REQUIRED) @NotEmpty @Size(max = 200) List<@NotNull Long> applicationIds,
    @Schema(description = "Request rejection reason (optional)") String rejectionReason) {
}
