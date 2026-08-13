package com.itasocialacademy.oitassist.chat.realtime.event;

/**
 * Contains identifiers required to remove a question from a participant
 * projection.
 *
 * <p>
 * This payload intentionally contains no question content or author data.
 * </p>
 */
public record QuestionRemovalPayload(
    Long taskAssignmentId,
    Long questionId)
    implements RealtimePayload {
    public QuestionRemovalPayload {
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