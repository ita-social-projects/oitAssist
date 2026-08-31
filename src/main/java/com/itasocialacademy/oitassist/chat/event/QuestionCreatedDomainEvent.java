package com.itasocialacademy.oitassist.chat.event;

import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import java.time.Instant;
import java.util.Objects;

public record QuestionCreatedDomainEvent(
    QuestionThreadResponseDTO question,
    Instant occurredAt)
    implements ForumDomainEvent {
    public QuestionCreatedDomainEvent {
        Objects.requireNonNull(
            question,
            "Created question snapshot must not be null");

        Objects.requireNonNull(
            occurredAt,
            "Question creation time must not be null");
    }
}