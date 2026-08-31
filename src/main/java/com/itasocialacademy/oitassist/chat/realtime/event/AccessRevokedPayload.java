package com.itasocialacademy.oitassist.chat.realtime.event;

/**
 * Instructs a client to discard cached question content after access has been
 * revoked.
 *
 * <p>
 * This payload intentionally contains identifiers only.
 * </p>
 */
public record AccessRevokedPayload(
    Long taskAssignmentId,
    Long questionId)
    implements RealtimePayload {
    public AccessRevokedPayload {
        requirePositive(
            taskAssignmentId,
            "Task assignment id");

        requirePositive(
            questionId,
            "Question id");
    }

    private static void requirePositive(
        Long identifier,
        String fieldName) {
        if (identifier == null || identifier <= 0) {
            throw new IllegalArgumentException(
                "%s must be a positive number"
                    .formatted(fieldName));
        }
    }
}