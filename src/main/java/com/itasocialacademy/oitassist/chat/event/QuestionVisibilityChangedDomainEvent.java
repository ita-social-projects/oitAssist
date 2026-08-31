package com.itasocialacademy.oitassist.chat.event;

import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility;
import java.time.Instant;
import java.util.Objects;

public record QuestionVisibilityChangedDomainEvent(
    QuestionThreadResponseDTO question,
    QuestionVisibility previousVisibility,
    QuestionVisibility currentVisibility,
    Instant occurredAt)
    implements ForumDomainEvent {
    public QuestionVisibilityChangedDomainEvent {
        Objects.requireNonNull(
            question,
            "Visibility question snapshot must not be null");

        Objects.requireNonNull(
            previousVisibility,
            "Previous visibility must not be null");

        Objects.requireNonNull(
            currentVisibility,
            "Current visibility must not be null");

        Objects.requireNonNull(
            occurredAt,
            "Visibility change time must not be null");

        if (question.visibility() != currentVisibility) {
            throw new IllegalArgumentException(
                "Current visibility does not match question snapshot");
        }
    }
}