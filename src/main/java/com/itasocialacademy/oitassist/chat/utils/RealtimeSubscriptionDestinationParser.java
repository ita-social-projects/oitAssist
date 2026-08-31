package com.itasocialacademy.oitassist.chat.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import static com.itasocialacademy.oitassist.chat.utils.RealtimeSubscriptionDestination.Type.*;

@Component
public class RealtimeSubscriptionDestinationParser {
    private static final String ADMINISTRATOR_INBOX_DESTINATION =
        "/topic/admin/questions/inbox";

    private static final String PARTICIPANT_QUESTIONS_DESTINATION =
        "/user/queue/questions";

    private static final String ADMINISTRATOR_REVIEWS_DESTINATION =
        "/user/queue/reviews";

    private static final Pattern TASK_ASSIGNMENT_FORUM_PATTERN =
        Pattern.compile(
            "^/topic/task-assignments/([1-9][0-9]*)/questions$");

    private static final Pattern PUBLIC_QUESTION_PATTERN =
        Pattern.compile(
            "^/topic/questions/([1-9][0-9]*)$");

    private static final String SUBSCRIPTION_NOT_ALLOWED =
        "STOMP subscription is not allowed";

    public RealtimeSubscriptionDestination parse(
        String destination) {
        if (destination == null || destination.isBlank()) {
            throw subscriptionNotAllowed();
        }

        if (ADMINISTRATOR_INBOX_DESTINATION.equals(destination)) {
            return new RealtimeSubscriptionDestination(
                ADMINISTRATOR_INBOX,
                null);
        }

        if (PARTICIPANT_QUESTIONS_DESTINATION.equals(destination)) {
            return new RealtimeSubscriptionDestination(
                PARTICIPANT_QUESTIONS,
                null);
        }

        if (ADMINISTRATOR_REVIEWS_DESTINATION.equals(destination)) {
            return new RealtimeSubscriptionDestination(
                ADMINISTRATOR_REVIEWS,
                null);
        }

        Matcher taskAssignmentMatcher =
            TASK_ASSIGNMENT_FORUM_PATTERN.matcher(destination);

        if (taskAssignmentMatcher.matches()) {
            return new RealtimeSubscriptionDestination(
                TASK_ASSIGNMENT_FORUM,
                parsePositiveId(taskAssignmentMatcher.group(1)));
        }

        Matcher questionMatcher =
            PUBLIC_QUESTION_PATTERN.matcher(destination);

        if (questionMatcher.matches()) {
            return new RealtimeSubscriptionDestination(
                PUBLIC_QUESTION_THREAD,
                parsePositiveId(questionMatcher.group(1)));
        }

        throw subscriptionNotAllowed();
    }

    private Long parsePositiveId(String value) {
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
        return new AccessDeniedException(
            SUBSCRIPTION_NOT_ALLOWED);
    }
}