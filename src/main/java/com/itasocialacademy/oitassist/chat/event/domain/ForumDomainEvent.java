package com.itasocialacademy.oitassist.chat.event.domain;

import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import java.time.Instant;

/**
 * Internal immutable event describing a successfully performed forum mutation.
 *
 * <p>
 * These events are published inside the business transaction and consumed only
 * after the transaction commits.
 * </p>
 *
 * <p>
 * Internal events must not be sent directly through STOMP. Participant and
 * administrator projection handlers convert them into external realtime events.
 * </p>
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