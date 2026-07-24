package com.itasocialacademy.oitassist.chat.dao.dto.response;

import com.itasocialacademy.oitassist.chat.dao.enums.QuestionState;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Builder;

@Builder
@Schema(description = "Question thread summary displayed in the participant forum")
public record QuestionThreadSummaryResponseDTO(
    Long id,
    Long taskId,
    Long authorId,
    String title,
    QuestionStatus status,
    QuestionVisibility visibility,
    QuestionState state,
    Instant createdAt,
    Instant updatedAt) {
}