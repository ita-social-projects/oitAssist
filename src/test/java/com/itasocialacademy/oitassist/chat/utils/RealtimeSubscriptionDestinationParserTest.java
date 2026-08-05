package com.itasocialacademy.oitassist.chat.utils;

import static com.itasocialacademy.oitassist.chat.utils.RealtimeSubscriptionDestination.Type.ADMINISTRATOR_INBOX;
import static com.itasocialacademy.oitassist.chat.utils.RealtimeSubscriptionDestination.Type.PARTICIPANT_QUESTIONS;
import static com.itasocialacademy.oitassist.chat.utils.RealtimeSubscriptionDestination.Type.PERSONAL_REVIEWS;
import static com.itasocialacademy.oitassist.chat.utils.RealtimeSubscriptionDestination.Type.PUBLIC_QUESTION_THREAD;
import static com.itasocialacademy.oitassist.chat.utils.RealtimeSubscriptionDestination.Type.TASK_ASSIGNMENT_FORUM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;

class RealtimeSubscriptionDestinationParserTest {

    private final RealtimeSubscriptionDestinationParser parser =
        new RealtimeSubscriptionDestinationParser();

    @Test
    void parse_taskAssignmentForum_shouldReturnResourceDestination() {

        RealtimeSubscriptionDestination result =
            parser.parse(
                "/topic/task-assignments/12/questions");

        assertEquals(
            TASK_ASSIGNMENT_FORUM,
            result.type());

        assertEquals(
            12L,
            result.resourceId());
    }

    @Test
    void parse_publicQuestion_shouldReturnResourceDestination() {

        RealtimeSubscriptionDestination result =
            parser.parse(
                "/topic/questions/84");

        assertEquals(
            PUBLIC_QUESTION_THREAD,
            result.type());

        assertEquals(
            84L,
            result.resourceId());
    }

    @Test
    void parse_adminInbox_shouldReturnFixedDestination() {

        RealtimeSubscriptionDestination result =
            parser.parse(
                "/topic/admin/questions/inbox");

        assertEquals(
            ADMINISTRATOR_INBOX,
            result.type());

        assertNull(
            result.resourceId());
    }

    @Test
    void parse_participantQueue_shouldReturnFixedDestination() {

        RealtimeSubscriptionDestination result =
            parser.parse(
                "/user/queue/questions");

        assertEquals(
            PARTICIPANT_QUESTIONS,
            result.type());

        assertNull(
            result.resourceId());
    }

    @Test
    void parse_personalReviewQueue_shouldReturnFixedDestination() {

        RealtimeSubscriptionDestination result =
            parser.parse(
                "/user/queue/reviews");

        assertEquals(
            PERSONAL_REVIEWS,
            result.type());

        assertNull(
            result.resourceId());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        " ",
        "/topic/questions/0",
        "/topic/questions/-1",
        "/topic/questions/abc",
        "/topic/questions/9223372036854775808",
        "/topic/questions/1/messages",
        "/topic/task-assignments/0/questions",
        "/topic/task-assignments/-1/questions",
        "/topic/task-assignments/abc/questions",
        "/topic/task-assignments/1",
        "/user/42/queue/questions",
        "/user/42/queue/reviews",
        "/user/queue/reviews/42",
        "/queue/reviews",
        "/queue/reviews-user42",
        "/queue/questions",
        "/app/questions"
    })
    void parse_unsupportedDestination_shouldReject(
        String destination) {

        assertThrows(
            AccessDeniedException.class,
            () -> parser.parse(
                destination));
    }
}