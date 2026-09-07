package com.itasocialacademy.oitassist.participation.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record FailedApplicationDecisionItemResponse(
    @Schema(description = "The ID of the updated application", example = "1") Long applicationId,
    @Schema(description = "Application processing failure reason", example = "Application not found") String reason) {
}
