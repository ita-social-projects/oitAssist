package com.itasocialacademy.oitassist.chat.dao.dto.response;

import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Builder;

@Builder
@Schema(description = "Foundational question thread response")
public record QuestionThreadResponseDTO(
    Long id,
    Long taskId,
    Long authorId,
    Long assignedReviewerId,
    String title,
    String content,
    QuestionStatus status,
    QuestionVisibility visibility,
    QuestionStatus statusBeforeClose,
    Long version,
    Instant createdAt,
    Instant updatedAt) {
}