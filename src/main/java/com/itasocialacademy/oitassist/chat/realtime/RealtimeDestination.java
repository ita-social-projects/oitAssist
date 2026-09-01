package com.itasocialacademy.oitassist.chat.realtime;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.access.AccessDeniedException;

public record RealtimeDestination(Type type, Long resourceId) {
    private static final String ADMINISTRATOR_INBOX_DESTINATION =
        "/topic/admin/questions/inbox";
    private static final String ADMINISTRATOR_ALL_QUESTIONS_DESTINATION =
        "/topic/admin/questions/all";
    private static final String PARTICIPANT_QUESTIONS_DESTINATION =
        "/user/queue/questions";
    private static final String PERSONAL_REVIEWS_DESTINATION =
        "/user/queue/reviews";

    private static final Pattern TASK_ASSIGNMENT_FORUM_PATTERN =
        Pattern.compile("^/topic/task-assignments/([1-9][0-9]*)/questions$");
    private static final Pattern PUBLIC_QUESTION_PATTERN =
        Pattern.compile("^/topic/questions/([1-9][0-9]*)$");

    private static final String SUBSCRIPTION_NOT_ALLOWED =
        "STOMP subscription is not allowed";

    public RealtimeDestination {
        Objects.requireNonNull(
            type,
            "Realtime subscription type must not be null");

        if (type.resourceScoped()
            && (resourceId == null || resourceId <= 0)) {
            throw new IllegalArgumentException(
                "Resource-scoped destination requires a positive identifier");
        }

        if (!type.resourceScoped() && resourceId != null) {
            throw new IllegalArgumentException(
                "Fixed destination must not contain a resource identifier");
        }
    }

    public static RealtimeDestination parse(String destination) {
        if (destination == null || destination.isBlank()) {
            throw subscriptionNotAllowed();
        }

        if (ADMINISTRATOR_INBOX_DESTINATION.equals(destination)) {
            return fixed(Type.ADMINISTRATOR_INBOX);
        }

        if (ADMINISTRATOR_ALL_QUESTIONS_DESTINATION.equals(destination)) {
            return fixed(Type.ADMINISTRATOR_ALL_QUESTIONS);
        }

        if (PARTICIPANT_QUESTIONS_DESTINATION.equals(destination)) {
            return fixed(Type.PARTICIPANT_QUESTIONS);
        }

        if (PERSONAL_REVIEWS_DESTINATION.equals(destination)) {
            return fixed(Type.PERSONAL_REVIEWS);
        }

        Matcher taskAssignmentMatcher =
            TASK_ASSIGNMENT_FORUM_PATTERN.matcher(destination);

        if (taskAssignmentMatcher.matches()) {
            return scoped(
                Type.TASK_ASSIGNMENT_FORUM,
                taskAssignmentMatcher.group(1));
        }
        Matcher questionMatcher =
            PUBLIC_QUESTION_PATTERN.matcher(destination);

        if (questionMatcher.matches()) {
            return scoped(
                Type.PUBLIC_QUESTION_THREAD,
                questionMatcher.group(1));
        }

        throw subscriptionNotAllowed();
    }

    private static RealtimeDestination fixed(Type type) {
        return new RealtimeDestination(type, null);
    }

    private static RealtimeDestination scoped(
        Type type,
        String rawIdentifier) {
        return new RealtimeDestination(
            type,
            parsePositiveId(rawIdentifier));
    }

    private static Long parsePositiveId(String value) {
        try {
            long identifier = Long.parseLong(value);

            if (identifier <= 0) {
                throw subscriptionNotAllowed();
            }

            return identifier;
        } catch (NumberFormatException exception) {
            throw subscriptionNotAllowed();
        }
    }

    private static AccessDeniedException subscriptionNotAllowed() {
        return new AccessDeniedException(SUBSCRIPTION_NOT_ALLOWED);
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