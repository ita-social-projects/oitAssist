package com.itasocialacademy.oitassist.participation.dao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO for rejecting the Application.")
public record RejectApplicationRequest(
    @Schema(description = "Application rejection reason (optional)",
        example = "User has invalid profile information") String rejectionReason) {
}
