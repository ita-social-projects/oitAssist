package com.itasocialacademy.oitassist.chat.dao.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(
    description = "TaskAssignment-specific forum responder assignment")
public record TaskAssignmentForumResponderResponseDTO(
    @Schema(example = "51") Long id,

    @Schema(example = "120") Long taskAssignmentId,

    @Schema(example = "17") Long responderUserId,

    @Schema(example = "responder@example.com") String responderEmail,

    @Schema(example = "Olena") String responderFirstName,

    @Schema(example = "Koval") String responderLastName,

    @Schema(example = "3") Long assignedByUserId,

    @Schema(example = "2026-08-04T12:00:00Z") Instant assignedAt) {
}