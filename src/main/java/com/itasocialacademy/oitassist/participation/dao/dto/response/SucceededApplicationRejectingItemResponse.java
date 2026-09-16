package com.itasocialacademy.oitassist.participation.dao.dto.response;

import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record SucceededApplicationRejectingItemResponse(
    @Schema(description = "The ID of the updated application", example = "1") Long applicationId,
    @Schema(description = "The ID of the applicant that failed to apply", example = "1") Long studentId,
    @Schema(description = "Updated request status", example = "REJECTED") RequestStatus status) {
}
