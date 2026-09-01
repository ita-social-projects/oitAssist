package com.itasocialacademy.oitassist.chat.event.domain;

import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import java.time.Instant;
import java.util.Objects;

public record QuestionStatusChangedDomainEvent(
    QuestionThreadResponseDTO question,
    QuestionStatus previousStatus,
    QuestionStatus currentStatus,
    Instant occurredAt) implements ForumDomainEvent {
    public QuestionStatusChangedDomainEvent {
        Objects.requireNonNull(question, "Status question snapshot must not be null");
        Objects.requireNonNull(previousStatus, "Previous question status must not be null");
        Objects.requireNonNull(currentStatus, "Current question status must not be null");
        Objects.requireNonNull(occurredAt, "Status change time must not be null");

        if (question.status() != currentStatus) {
            throw new IllegalArgumentException("Current status does not match question snapshot");
        }
    }
}