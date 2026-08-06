package com.itasocialacademy.oitassist.chat.integration;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PUBLIC;
import static com.itasocialacademy.oitassist.chat.utils.event.RealtimeEventType.ACCESS_REVOKED;
import static com.itasocialacademy.oitassist.chat.utils.event.RealtimeEventType.INBOX_REMOVED;
import static com.itasocialacademy.oitassist.chat.utils.event.RealtimeEventType.INBOX_UPSERTED;
import static com.itasocialacademy.oitassist.chat.utils.event.RealtimeEventType.MESSAGE_CREATED;
import static com.itasocialacademy.oitassist.chat.utils.event.RealtimeEventType.QUESTION_REMOVED;
import static com.itasocialacademy.oitassist.chat.utils.event.RealtimeEventType.QUESTION_UPSERTED;
import static com.itasocialacademy.oitassist.chat.utils.event.RealtimeEventType.REVIEW_UPDATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

class ForumRealtimeRestIT
    extends AbstractForumRealtimeIT {

    @Test
    void restMutation_shouldProduceStompFrameAfterCommit()
        throws Exception {

        StompSession ownerSession =
            connect(
                bearer(userToken()));

        StompSession adminSession =
            connect(
                bearer(adminToken()));

        SubscriptionProbe ownerProbe =
            subscribePersonal(
                ownerSession,
                USER_ID,
                PERSONAL_QUESTIONS_DESTINATION,
                PERSONAL_QUESTIONS_SUFFIX);

        SubscriptionProbe inboxProbe =
            subscribeShared(
                adminSession,
                ADMIN_ID,
                ADMIN_INBOX_DESTINATION);

        MvcResult result =
            mockMvc.perform(
                post(
                    "/api/v1/task-assignments/{taskAssignmentId}/questions",
                    ACCESSIBLE_TASK_ASSIGNMENT_ID)
                    .header(
                        AUTHORIZATION,
                        bearer(userToken()))
                    .contentType(
                        MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of(
                                "title",
                                "Realtime question",
                                "content",
                                "Created through REST"))))
                .andExpect(
                    status().isCreated())
                .andReturn();

        JsonNode response =
            objectMapper.readTree(
                result.getResponse()
                    .getContentAsString());

        Long questionId =
            response.path("id").asLong();

        JsonNode ownerEvent =
            awaitEvent(
                ownerProbe,
                QUESTION_UPSERTED.name(),
                questionId);

        JsonNode inboxEvent =
            awaitEvent(
                inboxProbe,
                INBOX_UPSERTED.name(),
                questionId);

        assertEquals(
            "Realtime question",
            ownerEvent.path("payload")
                .path("question")
                .path("title")
                .asText());

        assertEquals(
            questionId.longValue(),
            inboxEvent.path("payload")
                .path("question")
                .path("id")
                .asLong());

        /*
         * The REST response and STOMP event are observed only after the service
         * transaction has committed. The row must already be visible through a separate
         * repository transaction.
         */
        assertTrue(
            questionThreadRepository
                .findById(questionId)
                .isPresent());
    }

    @Test
    void rolledBackRestMutation_shouldNotProduceFrame()
        throws Exception {

        StompSession ownerSession =
            connect(
                bearer(userToken()));

        StompSession adminSession =
            connect(
                bearer(adminToken()));

        SubscriptionProbe ownerProbe =
            subscribePersonal(
                ownerSession,
                USER_ID,
                PERSONAL_QUESTIONS_DESTINATION,
                PERSONAL_QUESTIONS_SUFFIX);

        SubscriptionProbe inboxProbe =
            subscribeShared(
                adminSession,
                ADMIN_ID,
                ADMIN_INBOX_DESTINATION);

        long questionsBefore =
            questionThreadRepository.count();

        mockMvc.perform(
            post("/test/realtime/rollback")
                .header(
                    AUTHORIZATION,
                    bearer(userToken())))
            .andExpect(
                status().isInternalServerError());

        assertEquals(
            questionsBefore,
            questionThreadRepository.count());

        assertNoMessage(
            ownerProbe,
            Duration.ofMillis(750));

        assertNoMessage(
            inboxProbe,
            Duration.ofMillis(750));
    }

    @Test
    void publicToPrivate_shouldSendAccessRevoked()
        throws Exception {

        var question =
            saveQuestion(PUBLIC);

        StompSession observerSession =
            connect(
                bearer(otherUserToken()));

        StompSession ownerSession =
            connect(
                bearer(userToken()));

        SubscriptionProbe forumProbe =
            subscribeShared(
                observerSession,
                OTHER_USER_ID,
                forumDestination(
                    ACCESSIBLE_TASK_ASSIGNMENT_ID));

        SubscriptionProbe threadProbe =
            subscribeShared(
                observerSession,
                OTHER_USER_ID,
                questionDestination(
                    question.getId()));

        SubscriptionProbe ownerProbe =
            subscribePersonal(
                ownerSession,
                USER_ID,
                PERSONAL_QUESTIONS_DESTINATION,
                PERSONAL_QUESTIONS_SUFFIX);

        mockMvc.perform(
            patch(
                "/api/v1/admin/questions/{questionId}/visibility",
                question.getId())
                .header(
                    AUTHORIZATION,
                    bearer(adminToken()))
                .contentType(
                    MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "visibility",
                            "PRIVATE",
                            "version",
                            question.getVersion()))))
            .andExpect(
                status().isOk());

        JsonNode forumRemoval =
            awaitEvent(
                forumProbe,
                QUESTION_REMOVED.name(),
                question.getId());

        JsonNode accessRevoked =
            awaitEvent(
                threadProbe,
                ACCESS_REVOKED.name(),
                question.getId());

        JsonNode ownerUpsert =
            awaitEvent(
                ownerProbe,
                QUESTION_UPSERTED.name(),
                question.getId());

        assertEquals(
            question.getId().longValue(),
            forumRemoval.path("payload")
                .path("questionId")
                .asLong());

        assertEquals(
            question.getId().longValue(),
            accessRevoked.path("payload")
                .path("questionId")
                .asLong());

        assertFalse(
            accessRevoked.path("payload")
                .has("content"));

        assertFalse(
            accessRevoked.path("payload")
                .has("question"));

        assertEquals(
            "PRIVATE",
            ownerUpsert.path("payload")
                .path("question")
                .path("visibility")
                .asText());

        assertEquals(
            PRIVATE,
            questionThreadRepository
                .findById(question.getId())
                .orElseThrow()
                .getVisibility());
    }

    @Test
    void completeForumRealtimeScenario_shouldSynchronizeAllProjections()
        throws Exception {

        /*
         * Persistent sessions:
         *
         * owner: /user/queue/questions
         *
         * observer: /topic/task-assignments/{id}/questions later /topic/questions/{id}
         *
         * administrator: /topic/admin/questions/inbox /user/queue/reviews
         */
        StompSession ownerSession =
            connect(
                bearer(userToken()));

        StompSession observerSession =
            connect(
                bearer(otherUserToken()));

        StompSession administratorSession =
            connect(
                bearer(adminToken()));

        SubscriptionProbe ownerProbe =
            subscribePersonal(
                ownerSession,
                USER_ID,
                PERSONAL_QUESTIONS_DESTINATION,
                PERSONAL_QUESTIONS_SUFFIX);

        SubscriptionProbe forumProbe =
            subscribeShared(
                observerSession,
                OTHER_USER_ID,
                forumDestination(
                    ACCESSIBLE_TASK_ASSIGNMENT_ID));

        SubscriptionProbe inboxProbe =
            subscribeShared(
                administratorSession,
                ADMIN_ID,
                ADMIN_INBOX_DESTINATION);

        SubscriptionProbe reviewProbe =
            subscribePersonal(
                administratorSession,
                ADMIN_ID,
                PERSONAL_REVIEWS_DESTINATION,
                PERSONAL_REVIEWS_SUFFIX);

        /*
         * 1. Participant creates a PRIVATE question through production REST.
         */
        JsonNode createdQuestion =
            createQuestionThroughRest(
                "Complete E2E question",
                "Initial private content");

        Long questionId =
            createdQuestion.path("id")
                .asLong();

        long version =
            createdQuestion.path("version")
                .asLong();

        JsonNode ownerCreated =
            awaitEvent(
                ownerProbe,
                QUESTION_UPSERTED.name(),
                questionId);

        JsonNode inboxCreated =
            awaitEvent(
                inboxProbe,
                INBOX_UPSERTED.name(),
                questionId);

        assertEquals(
            "PRIVATE",
            ownerCreated.path("payload")
                .path("question")
                .path("visibility")
                .asText());

        assertEquals(
            "NEW",
            inboxCreated.path("payload")
                .path("question")
                .path("status")
                .asText());

        /*
         * PRIVATE question must not appear in the shared forum.
         */
        assertNoMessage(
            forumProbe,
            Duration.ofMillis(400));

        /*
         * A separate participant cannot subscribe to its shared thread.
         */
        RecordingSessionHandler privateSubscriptionHandler =
            new RecordingSessionHandler();

        StompSession privateSubscriptionSession =
            connect(
                ALLOWED_ORIGIN,
                bearer(otherUserToken()),
                privateSubscriptionHandler);

        assertSubscriptionRejected(
            privateSubscriptionSession,
            privateSubscriptionHandler,
            questionDestination(questionId));

        /*
         * 2. Administrator publishes the question.
         */
        JsonNode publicQuestion =
            updateVisibilityThroughRest(
                questionId,
                "PUBLIC",
                version);

        version =
            publicQuestion.path("version")
                .asLong();

        JsonNode forumPublished =
            awaitEvent(
                forumProbe,
                QUESTION_UPSERTED.name(),
                questionId);

        JsonNode ownerPublished =
            awaitEvent(
                ownerProbe,
                QUESTION_UPSERTED.name(),
                questionId);

        JsonNode inboxPublished =
            awaitEvent(
                inboxProbe,
                INBOX_UPSERTED.name(),
                questionId);

        assertEquals(
            "PUBLIC",
            forumPublished.path("payload")
                .path("question")
                .path("visibility")
                .asText());

        assertEquals(
            "PUBLIC",
            ownerPublished.path("payload")
                .path("question")
                .path("visibility")
                .asText());

        assertEquals(
            "PUBLIC",
            inboxPublished.path("payload")
                .path("question")
                .path("visibility")
                .asText());

        /*
         * The same observer can now subscribe to the public thread.
         */
        SubscriptionProbe threadProbe =
            subscribeShared(
                observerSession,
                OTHER_USER_ID,
                questionDestination(questionId));

        /*
         * 3. Administrator claims the question.
         */
        JsonNode claimedQuestion =
            claimThroughRest(
                questionId,
                version);

        version =
            claimedQuestion.path("version")
                .asLong();

        JsonNode inboxRemoved =
            awaitEvent(
                inboxProbe,
                INBOX_REMOVED.name(),
                questionId);

        JsonNode reviewerClaimed =
            awaitEvent(
                reviewProbe,
                REVIEW_UPDATED.name(),
                questionId);

        JsonNode forumClaimed =
            awaitEvent(
                forumProbe,
                QUESTION_UPSERTED.name(),
                questionId);

        JsonNode threadClaimed =
            awaitEvent(
                threadProbe,
                QUESTION_UPSERTED.name(),
                questionId);

        JsonNode ownerClaimed =
            awaitEvent(
                ownerProbe,
                QUESTION_UPSERTED.name(),
                questionId);

        assertEquals(
            ADMIN_ID.longValue(),
            reviewerClaimed.path("payload")
                .path("question")
                .path("assignedReviewerId")
                .asLong());

        assertEquals(
            "IN_REVIEW",
            forumClaimed.path("payload")
                .path("question")
                .path("status")
                .asText());

        assertEquals(
            "IN_REVIEW",
            threadClaimed.path("payload")
                .path("question")
                .path("status")
                .asText());

        assertEquals(
            "IN_REVIEW",
            ownerClaimed.path("payload")
                .path("question")
                .path("status")
                .asText());

        assertEquals(
            questionId.longValue(),
            inboxRemoved.path("payload")
                .path("questionId")
                .asLong());

        /*
         * 4. Owner adds a comment through REST.
         *
         * No refetch is needed: - thread subscribers append the message; - owner
         * appends it from the personal queue; - assigned reviewer appends it from the
         * review queue.
         */
        JsonNode createdMessage =
            addCommentThroughRest(
                questionId,
                "Realtime participant comment");

        Long messageId =
            createdMessage.path("id")
                .asLong();

        JsonNode threadMessage =
            awaitEvent(
                threadProbe,
                MESSAGE_CREATED.name(),
                questionId);

        JsonNode ownerMessage =
            awaitEvent(
                ownerProbe,
                MESSAGE_CREATED.name(),
                questionId);

        JsonNode reviewerMessage =
            awaitEvent(
                reviewProbe,
                MESSAGE_CREATED.name(),
                questionId);

        assertEquals(
            messageId.longValue(),
            threadMessage.path("payload")
                .path("message")
                .path("id")
                .asLong());

        assertEquals(
            messageId.longValue(),
            ownerMessage.path("payload")
                .path("message")
                .path("id")
                .asLong());

        assertEquals(
            messageId.longValue(),
            reviewerMessage.path("payload")
                .path("message")
                .path("id")
                .asLong());

        /*
         * Comments currently do not modify QuestionThread.version, so the version
         * returned by claim remains current for the visibility request.
         */
        JsonNode privateQuestion =
            updateVisibilityThroughRest(
                questionId,
                "PRIVATE",
                version);

        /*
         * 5. PUBLIC -> PRIVATE:
         *
         * - remove from shared forum list; - revoke the already-open shared thread; -
         * keep author projection; - synchronize assigned reviewer projection.
         */
        JsonNode forumRemoval =
            awaitEvent(
                forumProbe,
                QUESTION_REMOVED.name(),
                questionId);

        JsonNode threadRevocation =
            awaitEvent(
                threadProbe,
                ACCESS_REVOKED.name(),
                questionId);

        JsonNode ownerPrivate =
            awaitEvent(
                ownerProbe,
                QUESTION_UPSERTED.name(),
                questionId);

        JsonNode reviewerPrivate =
            awaitEvent(
                reviewProbe,
                REVIEW_UPDATED.name(),
                questionId);

        assertEquals(
            questionId.longValue(),
            forumRemoval.path("payload")
                .path("questionId")
                .asLong());

        assertEquals(
            questionId.longValue(),
            threadRevocation.path("payload")
                .path("questionId")
                .asLong());

        assertFalse(
            threadRevocation.path("payload")
                .has("content"));

        assertEquals(
            "PRIVATE",
            ownerPrivate.path("payload")
                .path("question")
                .path("visibility")
                .asText());

        assertEquals(
            "PRIVATE",
            reviewerPrivate.path("payload")
                .path("question")
                .path("visibility")
                .asText());

        assertEquals(
            "PRIVATE",
            privateQuestion.path("visibility")
                .asText());

        var persistedQuestion =
            questionThreadRepository
                .findById(questionId)
                .orElseThrow();

        assertEquals(
            PRIVATE,
            persistedQuestion.getVisibility());

        assertEquals(
            ADMIN_ID,
            persistedQuestion
                .getAssignedReviewerId());

        assertTrue(
            questionMessageRepository
                .findById(messageId)
                .isPresent());
    }

    private JsonNode createQuestionThroughRest(
        String title,
        String content)
        throws Exception {

        MvcResult result =
            mockMvc.perform(
                post(
                    "/api/v1/task-assignments/{taskAssignmentId}/questions",
                    ACCESSIBLE_TASK_ASSIGNMENT_ID)
                    .header(
                        AUTHORIZATION,
                        bearer(userToken()))
                    .contentType(
                        MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of(
                                "title",
                                title,
                                "content",
                                content))))
                .andExpect(
                    status().isCreated())
                .andReturn();

        return objectMapper.readTree(
            result.getResponse()
                .getContentAsString());
    }

    private JsonNode updateVisibilityThroughRest(
        Long questionId,
        String visibility,
        long version)
        throws Exception {

        MvcResult result =
            mockMvc.perform(
                patch(
                    "/api/v1/admin/questions/{questionId}/visibility",
                    questionId)
                    .header(
                        AUTHORIZATION,
                        bearer(adminToken()))
                    .contentType(
                        MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of(
                                "visibility",
                                visibility,
                                "version",
                                version))))
                .andExpect(
                    status().isOk())
                .andReturn();

        return objectMapper.readTree(
            result.getResponse()
                .getContentAsString());
    }

    private JsonNode claimThroughRest(
        Long questionId,
        long version)
        throws Exception {

        MvcResult result =
            mockMvc.perform(
                post(
                    "/api/v1/admin/questions/{questionId}/claim",
                    questionId)
                    .header(
                        AUTHORIZATION,
                        bearer(adminToken()))
                    .contentType(
                        MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of(
                                "version",
                                version))))
                .andExpect(
                    status().isOk())
                .andReturn();

        return objectMapper.readTree(
            result.getResponse()
                .getContentAsString());
    }

    private JsonNode addCommentThroughRest(
        Long questionId,
        String content)
        throws Exception {

        MvcResult result =
            mockMvc.perform(
                post(
                    "/api/v1/questions/{questionId}/comments",
                    questionId)
                    .header(
                        AUTHORIZATION,
                        bearer(userToken()))
                    .contentType(
                        MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            Map.of(
                                "content",
                                content))))
                .andExpect(
                    status().isCreated())
                .andReturn();

        return objectMapper.readTree(
            result.getResponse()
                .getContentAsString());
    }
}