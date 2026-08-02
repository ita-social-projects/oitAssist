package com.itasocialacademy.oitassist.chat.utils.event;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType.COMMENT;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.NEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PUBLIC;
import static com.itasocialacademy.oitassist.chat.utils.event.RealtimeEventType.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.itasocialacademy.oitassist.chat.dao.dto.response.AdminQuestionInboxItemResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.ObjectMapper;

class RealtimeForumEventTest {

    private static final Long TASK_ASSIGNMENT_ID = 10L;
    private static final Long QUESTION_ID = 20L;
    private static final Long MESSAGE_ID = 30L;
    private static final Long AUTHOR_ID = 40L;
    private static final Long REVIEWER_ID = 50L;
    private static final Long VERSION = 3L;

    private static final Instant OCCURRED_AT =
        Instant.parse("2026-08-02T16:00:00Z");

    private final ObjectMapper objectMapper =
        new ObjectMapper();

    @ParameterizedTest
    @MethodSource("serializableEvents")
    void serialization_shouldSerializeEveryPayloadCategory(
        RealtimeForumEvent event,
        List<String> expectedFragments)
        throws Exception {

        String json =
            objectMapper.writeValueAsString(event);

        assertTrue(
            json.contains(
                "\"eventId\":\""
                    + event.eventId()
                    + "\""));

        assertTrue(
            json.contains(
                "\"type\":\""
                    + event.type()
                    + "\""));

        assertTrue(
            json.contains("\"occurredAt\""));

        assertTrue(
            json.contains(
                "\"taskAssignmentId\":"
                    + TASK_ASSIGNMENT_ID));

        assertTrue(
            json.contains(
                "\"questionId\":"
                    + QUESTION_ID));

        assertTrue(
            json.contains("\"payload\""));

        expectedFragments.forEach(fragment -> assertTrue(
            json.contains(fragment),
            () -> "Expected JSON fragment: "
                + fragment
                + "\nActual JSON: "
                + json));
    }

    @Test
    void removalPayloads_shouldNotSerializeProtectedContent()
        throws Exception {

        List<RealtimePayload> payloads = List.of(
            new QuestionRemovalPayload(
                TASK_ASSIGNMENT_ID,
                QUESTION_ID),
            new InboxRemovalPayload(
                TASK_ASSIGNMENT_ID,
                QUESTION_ID),
            new AccessRevokedPayload(
                TASK_ASSIGNMENT_ID,
                QUESTION_ID));

        for (RealtimePayload payload : payloads) {
            String json =
                objectMapper.writeValueAsString(payload);

            assertFalse(json.contains("title"));
            assertFalse(json.contains("content"));
            assertFalse(json.contains("authorId"));
            assertFalse(json.contains("message"));
        }
    }

    @Test
    void create_shouldGenerateUniqueEventIdentifiers() {
        RealtimePayload payload =
            new QuestionRemovalPayload(
                TASK_ASSIGNMENT_ID,
                QUESTION_ID);

        RealtimeForumEvent first =
            RealtimeForumEvent.create(
                QUESTION_REMOVED,
                TASK_ASSIGNMENT_ID,
                QUESTION_ID,
                payload);

        RealtimeForumEvent second =
            RealtimeForumEvent.create(
                QUESTION_REMOVED,
                TASK_ASSIGNMENT_ID,
                QUESTION_ID,
                payload);

        assertNotNull(first.eventId());
        assertNotNull(first.occurredAt());
        assertNotEquals(
            first.eventId(),
            second.eventId());
    }

    @Test
    void constructor_incompatiblePayload_shouldReject() {
        RealtimePayload payload =
            new QuestionRemovalPayload(
                TASK_ASSIGNMENT_ID,
                QUESTION_ID);

        assertThrows(
            IllegalArgumentException.class,
            () -> new RealtimeForumEvent(
                UUID.randomUUID(),
                MESSAGE_CREATED,
                OCCURRED_AT,
                TASK_ASSIGNMENT_ID,
                QUESTION_ID,
                payload));
    }

    @Test
    void constructor_payloadScopeDiffersFromEnvelope_shouldReject() {
        RealtimePayload payload =
            new QuestionRemovalPayload(
                999L,
                QUESTION_ID);

        assertThrows(
            IllegalArgumentException.class,
            () -> new RealtimeForumEvent(
                UUID.randomUUID(),
                QUESTION_REMOVED,
                OCCURRED_AT,
                TASK_ASSIGNMENT_ID,
                QUESTION_ID,
                payload));
    }

    private static Stream<Arguments> serializableEvents() {

        QuestionThreadResponseDTO question =
            questionResponse();

        AdminQuestionInboxItemResponseDTO inboxQuestion =
            inboxResponse();

        QuestionMessageResponseDTO message =
            messageResponse();

        return Stream.of(
            Arguments.of(
                event(
                    QUESTION_UPSERTED,
                    new QuestionUpsertPayload(
                        question)),
                List.of(
                    "\"question\"",
                    "\"version\":3",
                    "\"content\":\"Question content\"")),

            Arguments.of(
                event(
                    QUESTION_REMOVED,
                    new QuestionRemovalPayload(
                        TASK_ASSIGNMENT_ID,
                        QUESTION_ID)),
                List.of(
                    "\"payload\":{\"taskAssignmentId\":10",
                    "\"questionId\":20")),

            Arguments.of(
                event(
                    MESSAGE_CREATED,
                    new MessageCreatedPayload(
                        message)),
                List.of(
                    "\"message\"",
                    "\"id\":30",
                    "\"content\":\"Message content\"")),

            Arguments.of(
                event(
                    INBOX_UPSERTED,
                    new InboxUpsertPayload(
                        inboxQuestion)),
                List.of(
                    "\"question\"",
                    "\"assignedReviewerId\":50",
                    "\"version\":3")),

            Arguments.of(
                event(
                    INBOX_REMOVED,
                    new InboxRemovalPayload(
                        TASK_ASSIGNMENT_ID,
                        QUESTION_ID)),
                List.of(
                    "\"payload\":{\"taskAssignmentId\":10",
                    "\"questionId\":20")),

            Arguments.of(
                event(
                    REVIEW_UPDATED,
                    new ReviewUpdatePayload(
                        inboxQuestion)),
                List.of(
                    "\"question\"",
                    "\"assignedReviewerId\":50",
                    "\"version\":3")),

            Arguments.of(
                event(
                    ACCESS_REVOKED,
                    new AccessRevokedPayload(
                        TASK_ASSIGNMENT_ID,
                        QUESTION_ID)),
                List.of(
                    "\"payload\":{\"taskAssignmentId\":10",
                    "\"questionId\":20")));
    }

    private static RealtimeForumEvent event(
        RealtimeEventType type,
        RealtimePayload payload) {

        return new RealtimeForumEvent(
            UUID.randomUUID(),
            type,
            OCCURRED_AT,
            TASK_ASSIGNMENT_ID,
            QUESTION_ID,
            payload);
    }

    private static QuestionThreadResponseDTO questionResponse() {

        return QuestionThreadResponseDTO.builder()
            .id(QUESTION_ID)
            .taskAssignmentId(TASK_ASSIGNMENT_ID)
            .authorId(AUTHOR_ID)
            .assignedReviewerId(REVIEWER_ID)
            .title("Question title")
            .content("Question content")
            .status(NEW)
            .visibility(PUBLIC)
            .state(OPEN)
            .version(VERSION)
            .createdAt(OCCURRED_AT)
            .updatedAt(OCCURRED_AT)
            .build();
    }

    private static AdminQuestionInboxItemResponseDTO inboxResponse() {

        return AdminQuestionInboxItemResponseDTO.builder()
            .id(QUESTION_ID)
            .taskAssignmentId(TASK_ASSIGNMENT_ID)
            .authorId(AUTHOR_ID)
            .assignedReviewerId(REVIEWER_ID)
            .title("Question title")
            .status(NEW)
            .state(OPEN)
            .visibility(PUBLIC)
            .version(VERSION)
            .createdAt(OCCURRED_AT)
            .updatedAt(OCCURRED_AT)
            .build();
    }

    private static QuestionMessageResponseDTO messageResponse() {

        return QuestionMessageResponseDTO.builder()
            .id(MESSAGE_ID)
            .questionThreadId(QUESTION_ID)
            .authorId(AUTHOR_ID)
            .type(COMMENT)
            .content("Message content")
            .createdAt(OCCURRED_AT)
            .build();
    }
}