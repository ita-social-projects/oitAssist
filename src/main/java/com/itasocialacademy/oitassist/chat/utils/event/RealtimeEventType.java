package com.itasocialacademy.oitassist.chat.utils.event;

import java.util.Objects;

/**
 * Describes the operation that a frontend projection must apply.
 *
 * <p>
 * Every event type accepts exactly one payload category:
 * </p>
 *
 * <ul>
 * <li>{@link #QUESTION_UPSERTED} — {@link QuestionUpsertPayload}</li>
 * <li>{@link #QUESTION_REMOVED} — {@link QuestionRemovalPayload}</li>
 * <li>{@link #MESSAGE_CREATED} — {@link MessageCreatedPayload}</li>
 * <li>{@link #INBOX_UPSERTED} — {@link InboxUpsertPayload}</li>
 * <li>{@link #INBOX_REMOVED} — {@link InboxRemovalPayload}</li>
 * <li>{@link #REVIEW_UPDATED} — {@link ReviewUpdatePayload}</li>
 * <li>{@link #ACCESS_REVOKED} — {@link AccessRevokedPayload}</li>
 * </ul>
 */
public enum RealtimeEventType {
    QUESTION_UPSERTED(QuestionUpsertPayload.class),
    QUESTION_REMOVED(QuestionRemovalPayload.class),
    MESSAGE_CREATED(MessageCreatedPayload.class),
    INBOX_UPSERTED(InboxUpsertPayload.class),
    INBOX_REMOVED(InboxRemovalPayload.class),
    REVIEW_UPDATED(ReviewUpdatePayload.class),
    ACCESS_REVOKED(AccessRevokedPayload.class);

    private final Class<? extends RealtimePayload> payloadType;

    RealtimeEventType(
        Class<? extends RealtimePayload> payloadType) {
        this.payloadType = payloadType;
    }

    public Class<? extends RealtimePayload> payloadType() {
        return payloadType;
    }

    void validatePayload(
        RealtimePayload payload) {
        Objects.requireNonNull(
            payload,
            "Realtime event payload must not be null");

        if (!payloadType.isInstance(payload)) {
            throw new IllegalArgumentException(
                "Realtime event type %s requires payload %s, but received %s"
                    .formatted(
                        name(),
                        payloadType.getSimpleName(),
                        payload.getClass().getSimpleName()));
        }
    }
}
