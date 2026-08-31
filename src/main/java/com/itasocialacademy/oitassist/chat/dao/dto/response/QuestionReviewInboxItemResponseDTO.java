package com.itasocialacademy.oitassist.chat.dao.dto.response;

import com.itasocialacademy.oitassist.chat.dao.enums.QuestionState;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Builder;

@Builder
@Schema(description = "Administrator question inbox item")
public record QuestionReviewInboxItemResponseDTO(
    Long id,
    Long taskAssignmentId,
    Long authorId,
    Long assignedReviewerId,
    String title,
    QuestionStatus status,
    QuestionState state,
    QuestionVisibility visibility,
    Long version,
    Instant createdAt,
    Instant updatedAt) {
}