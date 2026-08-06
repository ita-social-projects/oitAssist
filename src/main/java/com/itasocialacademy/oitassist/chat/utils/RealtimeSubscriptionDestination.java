package com.itasocialacademy.oitassist.chat.utils;

import java.util.Objects;

public record RealtimeSubscriptionDestination(Type type, Long resourceId) {
    public RealtimeSubscriptionDestination {
        Objects.requireNonNull(
            type,
            "Realtime subscription type must not be null");

        if (type.resourceScoped()
            && (resourceId == null || resourceId <= 0)) {
            throw new IllegalArgumentException(
                "Resource-scoped destination requires a positive identifier");
        }

        if (!type.resourceScoped()
            && resourceId != null) {
            throw new IllegalArgumentException(
                "Fixed destination must not contain a resource identifier");
        }
    }

    public enum Type {
        TASK_ASSIGNMENT_FORUM(true),
        PUBLIC_QUESTION_THREAD(true),
        ADMINISTRATOR_INBOX(false),
        PARTICIPANT_QUESTIONS(false),
        PERSONAL_REVIEWS(false),
        ADMINISTRATOR_ALL_QUESTIONS(false);

        private final boolean resourceScoped;

        Type(boolean resourceScoped) {
            this.resourceScoped = resourceScoped;
        }

        public boolean resourceScoped() {
            return resourceScoped;
        }
    }
}
