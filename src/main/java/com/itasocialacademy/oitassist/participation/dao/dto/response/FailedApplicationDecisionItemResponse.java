package com.itasocialacademy.oitassist.participation.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record FailedApplicationDecisionItemResponse(
    @Schema(description = "The ID of the application that was not processed", example = "1") Long applicationId,
    @Schema(description = "Application processing failure reason", example = "Application not found") String reason) {
}
