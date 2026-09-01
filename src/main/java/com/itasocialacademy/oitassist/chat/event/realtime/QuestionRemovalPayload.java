package com.itasocialacademy.oitassist.chat.event.realtime;

public record QuestionRemovalPayload(Long taskAssignmentId, Long questionId) implements RealtimePayload {
    public QuestionRemovalPayload {
        requirePositive(taskAssignmentId, "Task assignment id");
        requirePositive(questionId, "Question id");
    }

    private static void requirePositive(Long identifier, String fieldName) {
        if (identifier == null || identifier <= 0) {
            throw new IllegalArgumentException("%s must be a positive number".formatted(fieldName));
        }
    }
}