package com.itasocialacademy.oitassist.participation.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record SucceededInvitationResponse(
    @Schema(description = "The ID of the created record in the database", example = "1") Long id,
    @Schema(description = "Student ID that was failed to be invited", example = "2") Long studentId) {
}
