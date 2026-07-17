package com.itasocialacademy.oitassist.chat.dao.dto.response;

import com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Builder;

@Builder
@Schema(description = "Foundational question message response")
public record QuestionMessageResponseDTO(
    Long id,
    Long questionThreadId,
    Long authorId,
    QuestionMessageType type,
    String content,
    Instant createdAt) {
}