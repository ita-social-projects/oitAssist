package com.itasocialacademy.oitassist.chat.event.domain;

import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionState;
import java.time.Instant;
import java.util.Objects;

public record QuestionStateChangedDomainEvent(
    QuestionThreadResponseDTO question,
    QuestionState previousState,
    QuestionState currentState,
    Instant occurredAt)
    implements ForumDomainEvent {
    public QuestionStateChangedDomainEvent {
        Objects.requireNonNull(
            question,
            "State question snapshot must not be null");

        Objects.requireNonNull(
            previousState,
            "Previous question state must not be null");

        Objects.requireNonNull(
            currentState,
            "Current question state must not be null");

        Objects.requireNonNull(
            occurredAt,
            "State change time must not be null");

        if (question.state() != currentState) {
            throw new IllegalArgumentException(
                "Current state does not match question snapshot");
        }
    }
}