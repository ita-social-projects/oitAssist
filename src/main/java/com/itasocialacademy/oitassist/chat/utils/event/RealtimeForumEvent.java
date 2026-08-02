package com.itasocialacademy.oitassist.chat.utils.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import com.itasocialacademy.oitassist.chat.dao.dto.response.AdminQuestionInboxItemResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;

/**
 * Immutable external envelope delivered to STOMP subscribers.
 *
 * @param eventId          unique delivery-event identifier
 * @param type             frontend projection operation
 * @param occurredAt       UTC event creation time
 * @param taskAssignmentId related task-assignment identifier
 * @param questionId       related question identifier
 * @param payload          typed external payload
 */
public record RealtimeForumEvent(
    UUID eventId,
    RealtimeEventType type,
    Instant occurredAt,
    Long taskAssignmentId,
    Long questionId,
    RealtimePayload payload) {
    public RealtimeForumEvent {
        eventId = Objects.requireNonNull(
            eventId,
            "Realtime event id must not be null");

        type = Objects.requireNonNull(
            type,
            "Realtime event type must not be null");

        occurredAt = Objects.requireNonNull(
            occurredAt,
            "Realtime event occurrence time must not be null");

        requirePositiveId(
            taskAssignmentId,
            "Task assignment id");

        requirePositiveId(
            questionId,
            "Question id");

        type.validatePayload(payload);

        validatePayloadScope(
            taskAssignmentId,
            questionId,
            payload);
    }

    /**
     * Creates an event with a new delivery identifier and current UTC time.
     */
    public static RealtimeForumEvent create(
        RealtimeEventType type,
        Long taskAssignmentId,
        Long questionId,
        RealtimePayload payload) {
        return new RealtimeForumEvent(
            UUID.randomUUID(),
            type,
            Instant.now(),
            taskAssignmentId,
            questionId,
            payload);
    }

    private static void validatePayloadScope(
        Long taskAssignmentId,
        Long questionId,
        RealtimePayload payload) {
        switch (payload) {
            case QuestionUpsertPayload questionUpsert ->
                validateQuestionSnapshot(
                    taskAssignmentId,
                    questionId,
                    questionUpsert.question());

            case QuestionRemovalPayload questionRemoval -> {
                requireMatchingId(
                    taskAssignmentId,
                    questionRemoval.taskAssignmentId(),
                    "Task assignment id");
                requireMatchingId(
                    questionId,
                    questionRemoval.questionId(),
                    "Question id");
            }

            case MessageCreatedPayload messageCreated -> {
                requireMatchingId(
                    questionId,
                    messageCreated.message().questionThreadId(),
                    "Question id");
                requirePositiveId(
                    messageCreated.message().id(),
                    "Message id");

                Objects.requireNonNull(
                    messageCreated.message().createdAt(),
                    "Message creation time must not be null");
            }

            case InboxUpsertPayload inboxUpsert ->
                validateInboxSnapshot(
                    taskAssignmentId,
                    questionId,
                    inboxUpsert.question());

            case InboxRemovalPayload inboxRemoval -> {
                requireMatchingId(
                    taskAssignmentId,
                    inboxRemoval.taskAssignmentId(),
                    "Task assignment id");
                requireMatchingId(
                    questionId,
                    inboxRemoval.questionId(),
                    "Question id");
            }

            case ReviewUpdatePayload reviewUpdate ->
                validateInboxSnapshot(
                    taskAssignmentId,
                    questionId,
                    reviewUpdate.question());

            case AccessRevokedPayload accessRevoked -> {
                requireMatchingId(
                    taskAssignmentId,
                    accessRevoked.taskAssignmentId(),
                    "Task assignment id");
                requireMatchingId(
                    questionId,
                    accessRevoked.questionId(),
                    "Question id");
            }
        }
    }

    private static void validateQuestionSnapshot(
        Long taskAssignmentId,
        Long questionId,
        QuestionThreadResponseDTO question) {
        requireMatchingId(
            taskAssignmentId,
            question.taskAssignmentId(),
            "Task assignment id");

        requireMatchingId(
            questionId,
            question.id(),
            "Question id");

        requireNonNegativeVersion(
            question.version());
    }

    private static void validateInboxSnapshot(
        Long taskAssignmentId,
        Long questionId,
        AdminQuestionInboxItemResponseDTO question) {
        requireMatchingId(
            taskAssignmentId,
            question.taskAssignmentId(),
            "Task assignment id");

        requireMatchingId(
            questionId,
            question.id(),
            "Question id");

        requireNonNegativeVersion(
            question.version());
    }

    private static void requireMatchingId(
        Long expected,
        Long actual,
        String fieldName) {
        requirePositiveId(
            actual,
            fieldName);

        if (!expected.equals(actual)) {
            throw new IllegalArgumentException(
                "%s in payload does not match event envelope"
                    .formatted(fieldName));
        }
    }

    private static void requirePositiveId(
        Long identifier,
        String fieldName) {
        if (identifier == null || identifier <= 0) {
            throw new IllegalArgumentException(
                "%s must be a positive number"
                    .formatted(fieldName));
        }
    }

    private static void requireNonNegativeVersion(
        Long version) {
        if (version == null || version < 0) {
            throw new IllegalArgumentException(
                "Question version must not be negative");
        }
    }
}
