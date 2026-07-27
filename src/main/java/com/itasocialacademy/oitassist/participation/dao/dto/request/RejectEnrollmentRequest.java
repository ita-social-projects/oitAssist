package com.itasocialacademy.oitassist.participation.dao.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO for rejecting the Enrollment requests.")
public record RejectEnrollmentRequest(
    @Schema(description = "Request rejection reason (optional)") String rejectionReason) {
}
