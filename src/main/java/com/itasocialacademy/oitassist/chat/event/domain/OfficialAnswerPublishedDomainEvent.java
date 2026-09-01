package com.itasocialacademy.oitassist.chat.event.domain;

import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import java.time.Instant;
import java.util.Objects;

public record OfficialAnswerPublishedDomainEvent(
    QuestionThreadResponseDTO question,
    QuestionMessageResponseDTO message,
    QuestionStatus previousStatus,
    QuestionStatus currentStatus,
    Instant occurredAt)
    implements ForumDomainEvent {
    public OfficialAnswerPublishedDomainEvent {
        Objects.requireNonNull(
            question,
            "Answered question snapshot must not be null");

        Objects.requireNonNull(
            message,
            "Official answer snapshot must not be null");

        Objects.requireNonNull(
            previousStatus,
            "Previous question status must not be null");

        Objects.requireNonNull(
            currentStatus,
            "Current question status must not be null");

        Objects.requireNonNull(
            occurredAt,
            "Official answer publication time must not be null");

        if (!Objects.equals(
            question.id(),
            message.questionThreadId())) {
            throw new IllegalArgumentException(
                "Official answer question id does not match question snapshot");
        }

        if (question.status() != currentStatus) {
            throw new IllegalArgumentException(
                "Current status does not match question snapshot");
        }
    }
}