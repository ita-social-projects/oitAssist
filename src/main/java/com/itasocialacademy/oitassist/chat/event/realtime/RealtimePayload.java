package com.itasocialacademy.oitassist.chat.event.realtime;

/**
 * Marker interface for payloads delivered through realtime forum events.
 *
 * <p>
 * Implementations are immutable external DTOs. JPA entities and internal
 * domain-event objects must not implement this interface.
 * </p>
 */
public sealed interface RealtimePayload
    permits QuestionUpsertPayload,
    QuestionRemovalPayload,
    MessageCreatedPayload,
    InboxUpsertPayload,
    InboxRemovalPayload,
    ReviewUpdatePayload,
    AccessRevokedPayload {
}