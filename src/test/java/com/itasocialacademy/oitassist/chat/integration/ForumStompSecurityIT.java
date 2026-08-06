package com.itasocialacademy.oitassist.chat.integration;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PUBLIC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.StompSession;

class ForumStompSecurityIT
    extends AbstractForumRealtimeIT {

    @Test
    void validToken_shouldConnect() throws Exception {
        StompSession session =
            connect(
                bearer(userToken()));

        assertTrue(
            session.isConnected());
    }

    @Test
    void missingOrInvalidToken_shouldFail() {
        assertConnectRejected(
            ALLOWED_ORIGIN,
            null);

        assertConnectRejected(
            ALLOWED_ORIGIN,
            "Bearer ");

        assertConnectRejected(
            ALLOWED_ORIGIN,
            "Bearer malformed-token");

        assertConnectRejected(
            ALLOWED_ORIGIN,
            "Token " + userToken());

        assertConnectRejected(
            ALLOWED_ORIGIN,
            bearer(
                wrongTypeToken(
                    USER_EMAIL)));

        assertConnectRejected(
            ALLOWED_ORIGIN,
            bearer(
                accessToken(
                    "missing-user@example.com")));

        String validToken =
            userToken();

        String tamperedToken =
            validToken.substring(
                0,
                validToken.length() - 1)
                + (validToken.endsWith("a")
                    ? "b"
                    : "a");

        assertConnectRejected(
            ALLOWED_ORIGIN,
            bearer(tamperedToken));
    }

    @Test
    void unsupportedOrigin_shouldFailHandshake() {
        assertConnectRejected(
            UNSUPPORTED_ORIGIN,
            bearer(userToken()));
    }

    @Test
    void userCannotSubscribeAdminInbox()
        throws Exception {

        RecordingSessionHandler handler =
            new RecordingSessionHandler();

        StompSession session =
            connect(
                ALLOWED_ORIGIN,
                bearer(userToken()),
                handler);

        assertSubscriptionRejected(
            session,
            handler,
            ADMIN_INBOX_DESTINATION);
    }

    @Test
    void participantCannotSubscribeInaccessibleTaskAssignmentForum()
        throws Exception {

        RecordingSessionHandler handler =
            new RecordingSessionHandler();

        StompSession session =
            connect(
                ALLOWED_ORIGIN,
                bearer(userToken()),
                handler);

        assertSubscriptionRejected(
            session,
            handler,
            forumDestination(
                INACCESSIBLE_TASK_ASSIGNMENT_ID));
    }

    @Test
    void privateQuestionSharedTopicSubscription_shouldFail()
        throws Exception {

        var question =
            saveQuestion(PRIVATE);

        RecordingSessionHandler handler =
            new RecordingSessionHandler();

        StompSession session =
            connect(
                ALLOWED_ORIGIN,
                bearer(otherUserToken()),
                handler);

        assertSubscriptionRejected(
            session,
            handler,
            questionDestination(
                question.getId()));
    }

    @Test
    void publicQuestionSharedTopicSubscription_shouldSucceed()
        throws Exception {

        var question =
            saveQuestion(PUBLIC);

        StompSession session =
            connect(
                bearer(otherUserToken()));

        SubscriptionProbe probe =
            subscribeShared(
                session,
                OTHER_USER_ID,
                questionDestination(
                    question.getId()));

        messagingTemplate.convertAndSend(
            questionDestination(
                question.getId()),
            "public-question-marker");

        assertEquals(
            "public-question-marker",
            probe.poll(
                Duration.ofSeconds(5)));
    }

    @Test
    void personalQuestionQueue_shouldBeIsolated()
        throws Exception {

        StompSession firstUser =
            connect(
                bearer(userToken()));

        StompSession secondUser =
            connect(
                bearer(otherUserToken()));

        SubscriptionProbe firstProbe =
            subscribePersonal(
                firstUser,
                USER_ID,
                PERSONAL_QUESTIONS_DESTINATION,
                PERSONAL_QUESTIONS_SUFFIX);

        SubscriptionProbe secondProbe =
            subscribePersonal(
                secondUser,
                OTHER_USER_ID,
                PERSONAL_QUESTIONS_DESTINATION,
                PERSONAL_QUESTIONS_SUFFIX);

        messagingTemplate.convertAndSendToUser(
            USER_ID.toString(),
            PERSONAL_QUESTIONS_SUFFIX,
            "owner-only-message");

        assertEquals(
            "owner-only-message",
            firstProbe.poll(
                Duration.ofSeconds(5)));

        assertNoMessage(
            secondProbe,
            Duration.ofMillis(500));
    }

    @Test
    void personalReviewQueue_shouldBeIsolated()
        throws Exception {

        StompSession firstAdmin =
            connect(
                bearer(adminToken()));

        StompSession secondAdmin =
            connect(
                bearer(otherAdminToken()));

        SubscriptionProbe firstProbe =
            subscribePersonal(
                firstAdmin,
                ADMIN_ID,
                PERSONAL_REVIEWS_DESTINATION,
                PERSONAL_REVIEWS_SUFFIX);

        SubscriptionProbe secondProbe =
            subscribePersonal(
                secondAdmin,
                OTHER_ADMIN_ID,
                PERSONAL_REVIEWS_DESTINATION,
                PERSONAL_REVIEWS_SUFFIX);

        messagingTemplate.convertAndSendToUser(
            ADMIN_ID.toString(),
            PERSONAL_REVIEWS_SUFFIX,
            "assigned-reviewer-only");

        assertEquals(
            "assigned-reviewer-only",
            firstProbe.poll(
                Duration.ofSeconds(5)));

        assertNoMessage(
            secondProbe,
            Duration.ofMillis(500));
    }
}