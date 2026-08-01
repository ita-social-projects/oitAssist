package com.itasocialacademy.oitassist.participation.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record FailedInvitationResponse(
    @Schema(description = "Student ID that was failed to be invited", example = "2") Long studentId,
    @Schema(description = "Invitation failure reason", example = "Student not found") String reason) {
}
