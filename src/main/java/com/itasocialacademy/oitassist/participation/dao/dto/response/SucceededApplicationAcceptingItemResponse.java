package com.itasocialacademy.oitassist.participation.dao.dto.response;

import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record SucceededApplicationAcceptingItemResponse(
    @Schema(description = "The ID of the updated application", example = "1") Long applicationId,
    @Schema(description = "The ID of the new participant", example = "1") Long participantId,
    @Schema(description = "Updated request status", example = "ACCEPTED") RequestStatus status) {
}
