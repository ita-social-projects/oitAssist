package com.itasocialacademy.oitassist.chat.event.domain;

import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import java.time.Instant;
import java.util.Objects;

public record CommentCreatedDomainEvent(
    QuestionThreadResponseDTO question,
    QuestionMessageResponseDTO message,
    Instant occurredAt)
    implements ForumDomainEvent {
    public CommentCreatedDomainEvent {
        Objects.requireNonNull(
            question,
            "Comment question snapshot must not be null");

        Objects.requireNonNull(
            message,
            "Created comment snapshot must not be null");

        Objects.requireNonNull(
            occurredAt,
            "Comment creation time must not be null");

        if (!Objects.equals(
            question.id(),
            message.questionThreadId())) {
            throw new IllegalArgumentException(
                "Comment question id does not match question snapshot");
        }
    }
}