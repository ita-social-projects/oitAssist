package com.itasocialacademy.oitassist.chat.event.domain;

import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import java.time.Instant;
import java.util.Objects;

public record QuestionClaimedDomainEvent(
    QuestionThreadResponseDTO question,
    Long previousReviewerId,
    Long currentReviewerId,
    Instant occurredAt)
    implements ForumDomainEvent {
    public QuestionClaimedDomainEvent {
        Objects.requireNonNull(
            question,
            "Claimed question snapshot must not be null");

        Objects.requireNonNull(
            currentReviewerId,
            "Current reviewer id must not be null");

        Objects.requireNonNull(
            occurredAt,
            "Question claim time must not be null");

        if (currentReviewerId <= 0) {
            throw new IllegalArgumentException(
                "Current reviewer id must be positive");
        }

        if (!Objects.equals(
            question.assignedReviewerId(),
            currentReviewerId)) {
            throw new IllegalArgumentException(
                "Current reviewer id does not match question snapshot");
        }
    }
}