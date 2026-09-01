package com.itasocialacademy.oitassist.chat.event.domain;

import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import java.time.Instant;

/**
 * Internal immutable event describing a successfully performed forum mutation.
 */
public sealed interface ForumDomainEvent
    permits QuestionCreatedDomainEvent,
    CommentCreatedDomainEvent,
    QuestionClaimedDomainEvent,
    OfficialAnswerPublishedDomainEvent,
    QuestionVisibilityChangedDomainEvent,
    QuestionStatusChangedDomainEvent,
    QuestionStateChangedDomainEvent {
    QuestionThreadResponseDTO question();

    Instant occurredAt();

    default Long taskAssignmentId() {
        return question().taskAssignmentId();
    }

    default Long questionId() {
        return question().id();
    }
}