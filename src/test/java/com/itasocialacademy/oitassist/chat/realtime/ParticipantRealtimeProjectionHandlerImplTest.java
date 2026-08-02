package com.itasocialacademy.oitassist.chat.realtime;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType.COMMENT;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType.OFFICIAL_ANSWER;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.CLOSED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.ANSWERED;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.IN_REVIEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.NEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PUBLIC;
import static com.itasocialacademy.oitassist.chat.utils.event.RealtimeEventType.ACCESS_REVOKED;
import static com.itasocialacademy.oitassist.chat.utils.event.RealtimeEventType.MESSAGE_CREATED;
import static com.itasocialacademy.oitassist.chat.utils.event.RealtimeEventType.QUESTION_REMOVED;
import static com.itasocialacademy.oitassist.chat.utils.event.RealtimeEventType.QUESTION_UPSERTED;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionState;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility;
import com.itasocialacademy.oitassist.chat.event.CommentCreatedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.OfficialAnswerPublishedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionClaimedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionCreatedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionStateChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionStatusChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionVisibilityChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.utils.event.AccessRevokedPayload;
import com.itasocialacademy.oitassist.chat.utils.event.MessageCreatedPayload;
import com.itasocialacademy.oitassist.chat.utils.event.QuestionRemovalPayload;
import com.itasocialacademy.oitassist.chat.utils.event.QuestionUpsertPayload;
import com.itasocialacademy.oitassist.chat.utils.event.RealtimeEventType;
import com.itasocialacademy.oitassist.chat.utils.event.RealtimeForumEvent;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

@ExtendWith(MockitoExtension.class)
class ParticipantRealtimeProjectionHandlerImplTest {

    private static final Long TASK_ASSIGNMENT_ID = 10L;
    private static final Long QUESTION_ID = 20L;
    private static final Long AUTHOR_ID = 30L;
    private static final Long REVIEWER_ID = 40L;
    private static final Long MESSAGE_ID = 50L;

    private static final String FORUM_DESTINATION =
        "/topic/task-assignments/10/questions";

    private static final String THREAD_DESTINATION =
        "/topic/questions/20";

    private static final String PERSONAL_QUEUE =
        "/queue/questions";

    private static final Instant CREATED_AT =
        Instant.parse(
            "2026-08-03T10:00:00Z");

    private static final Instant UPDATED_AT =
        Instant.parse(
            "2026-08-03T10:15:00Z");

    private static final Instant OCCURRED_AT =
        Instant.parse(
            "2026-08-03T10:16:00Z");

    @Mock
    private SimpMessageSendingOperations messagingOperations;

    @InjectMocks
    private ParticipantRealtimeProjectionHandlerImpl handler;

    @Test
    void privateQuestionCreation_shouldSendAuthorUpsertOnly() {
        QuestionThreadResponseDTO question =
            question(
                PRIVATE,
                NEW,
                OPEN,
                null,
                0L);

        handler.handle(
            new QuestionCreatedDomainEvent(
                question,
                OCCURRED_AT));

        List<RealtimeForumEvent> personalEvents =
            capturePersonalEvents(1);

        assertEventTypes(
            personalEvents,
            QUESTION_UPSERTED);

        assertQuestionUpsert(
            personalEvents.getFirst(),
            question);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void publicQuestionCreation_shouldSendSharedAndAuthorUpserts() {
        QuestionThreadResponseDTO question =
            question(
                PUBLIC,
                NEW,
                OPEN,
                null,
                0L);

        handler.handle(
            new QuestionCreatedDomainEvent(
                question,
                OCCURRED_AT));

        List<RealtimeForumEvent> threadEvents =
            captureSharedEvents(
                THREAD_DESTINATION,
                1);

        List<RealtimeForumEvent> forumEvents =
            captureSharedEvents(
                FORUM_DESTINATION,
                1);

        List<RealtimeForumEvent> personalEvents =
            capturePersonalEvents(1);

        assertQuestionUpsert(
            threadEvents.getFirst(),
            question);

        assertQuestionUpsert(
            forumEvents.getFirst(),
            question);

        assertQuestionUpsert(
            personalEvents.getFirst(),
            question);

        assertUniqueEventIds(
            threadEvents,
            forumEvents,
            personalEvents);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void publicComment_shouldSendAppendableMessageToThreadAndAuthor() {
        QuestionThreadResponseDTO question =
            question(
                PUBLIC,
                IN_REVIEW,
                OPEN,
                REVIEWER_ID,
                3L);

        QuestionMessageResponseDTO message =
            message(
                COMMENT);

        handler.handle(
            new CommentCreatedDomainEvent(
                question,
                message,
                OCCURRED_AT));

        List<RealtimeForumEvent> threadEvents =
            captureSharedEvents(
                THREAD_DESTINATION,
                1);

        List<RealtimeForumEvent> personalEvents =
            capturePersonalEvents(1);

        assertMessageCreated(
            threadEvents.getFirst(),
            message);

        assertMessageCreated(
            personalEvents.getFirst(),
            message);

        assertUniqueEventIds(
            threadEvents,
            personalEvents);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void privateComment_shouldSendMessageToAuthorOnly() {
        QuestionThreadResponseDTO question =
            question(
                PRIVATE,
                IN_REVIEW,
                OPEN,
                REVIEWER_ID,
                3L);

        QuestionMessageResponseDTO message =
            message(
                COMMENT);

        handler.handle(
            new CommentCreatedDomainEvent(
                question,
                message,
                OCCURRED_AT));

        List<RealtimeForumEvent> personalEvents =
            capturePersonalEvents(1);

        assertMessageCreated(
            personalEvents.getFirst(),
            message);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void publicOfficialAnswer_shouldUpdateThreadForumAndAuthor() {
        QuestionThreadResponseDTO question =
            question(
                PUBLIC,
                ANSWERED,
                OPEN,
                REVIEWER_ID,
                4L);

        QuestionMessageResponseDTO message =
            message(
                OFFICIAL_ANSWER);

        handler.handle(
            new OfficialAnswerPublishedDomainEvent(
                question,
                message,
                IN_REVIEW,
                ANSWERED,
                OCCURRED_AT));

        List<RealtimeForumEvent> threadEvents =
            captureSharedEvents(
                THREAD_DESTINATION,
                2);

        List<RealtimeForumEvent> forumEvents =
            captureSharedEvents(
                FORUM_DESTINATION,
                1);

        List<RealtimeForumEvent> personalEvents =
            capturePersonalEvents(2);

        assertEventTypes(
            threadEvents,
            MESSAGE_CREATED,
            QUESTION_UPSERTED);

        assertEventTypes(
            forumEvents,
            QUESTION_UPSERTED);

        assertEventTypes(
            personalEvents,
            MESSAGE_CREATED,
            QUESTION_UPSERTED);

        assertMessageCreated(
            threadEvents.get(0),
            message);

        assertQuestionUpsert(
            threadEvents.get(1),
            question);

        assertQuestionUpsert(
            forumEvents.getFirst(),
            question);

        assertMessageCreated(
            personalEvents.get(0),
            message);

        assertQuestionUpsert(
            personalEvents.get(1),
            question);

        assertUniqueEventIds(
            threadEvents,
            forumEvents,
            personalEvents);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void privateOfficialAnswer_shouldUseAuthorQueueOnly() {
        QuestionThreadResponseDTO question =
            question(
                PRIVATE,
                ANSWERED,
                OPEN,
                REVIEWER_ID,
                4L);

        QuestionMessageResponseDTO message =
            message(
                OFFICIAL_ANSWER);

        handler.handle(
            new OfficialAnswerPublishedDomainEvent(
                question,
                message,
                IN_REVIEW,
                ANSWERED,
                OCCURRED_AT));

        List<RealtimeForumEvent> personalEvents =
            capturePersonalEvents(2);

        assertEventTypes(
            personalEvents,
            MESSAGE_CREATED,
            QUESTION_UPSERTED);

        assertMessageCreated(
            personalEvents.get(0),
            message);

        assertQuestionUpsert(
            personalEvents.get(1),
            question);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void privateToPublic_shouldUpsertForumThreadAndAuthor() {
        QuestionThreadResponseDTO question =
            question(
                PUBLIC,
                IN_REVIEW,
                OPEN,
                REVIEWER_ID,
                4L);

        handler.handle(
            new QuestionVisibilityChangedDomainEvent(
                question,
                PRIVATE,
                PUBLIC,
                OCCURRED_AT));

        List<RealtimeForumEvent> forumEvents =
            captureSharedEvents(
                FORUM_DESTINATION,
                1);

        List<RealtimeForumEvent> threadEvents =
            captureSharedEvents(
                THREAD_DESTINATION,
                1);

        List<RealtimeForumEvent> personalEvents =
            capturePersonalEvents(1);

        assertQuestionUpsert(
            forumEvents.getFirst(),
            question);

        assertQuestionUpsert(
            threadEvents.getFirst(),
            question);

        assertQuestionUpsert(
            personalEvents.getFirst(),
            question);

        assertUniqueEventIds(
            forumEvents,
            threadEvents,
            personalEvents);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void publicToPrivate_shouldRemoveRevokeAndSendPrivateAuthorSnapshot() {
        QuestionThreadResponseDTO question =
            question(
                PRIVATE,
                IN_REVIEW,
                OPEN,
                REVIEWER_ID,
                5L);

        handler.handle(
            new QuestionVisibilityChangedDomainEvent(
                question,
                PUBLIC,
                PRIVATE,
                OCCURRED_AT));

        List<RealtimeForumEvent> forumEvents =
            captureSharedEvents(
                FORUM_DESTINATION,
                1);

        List<RealtimeForumEvent> threadEvents =
            captureSharedEvents(
                THREAD_DESTINATION,
                1);

        List<RealtimeForumEvent> personalEvents =
            capturePersonalEvents(1);

        RealtimeForumEvent removalEvent =
            forumEvents.getFirst();

        assertEventType(
            removalEvent,
            QUESTION_REMOVED);

        QuestionRemovalPayload removalPayload =
            assertInstanceOf(
                QuestionRemovalPayload.class,
                removalEvent.payload());

        assertAll(
            () -> assertEquals(
                TASK_ASSIGNMENT_ID,
                removalPayload.taskAssignmentId()),
            () -> assertEquals(
                QUESTION_ID,
                removalPayload.questionId()));

        RealtimeForumEvent revokedEvent =
            threadEvents.getFirst();

        assertEventType(
            revokedEvent,
            ACCESS_REVOKED);

        AccessRevokedPayload revokedPayload =
            assertInstanceOf(
                AccessRevokedPayload.class,
                revokedEvent.payload());

        assertAll(
            () -> assertEquals(
                TASK_ASSIGNMENT_ID,
                revokedPayload.taskAssignmentId()),
            () -> assertEquals(
                QUESTION_ID,
                revokedPayload.questionId()));

        assertQuestionUpsert(
            personalEvents.getFirst(),
            question);

        assertUniqueEventIds(
            forumEvents,
            threadEvents,
            personalEvents);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void publicStatusChange_shouldUpdateThreadAndForum() {
        QuestionThreadResponseDTO question =
            question(
                PUBLIC,
                ANSWERED,
                OPEN,
                REVIEWER_ID,
                5L);

        handler.handle(
            new QuestionStatusChangedDomainEvent(
                question,
                IN_REVIEW,
                ANSWERED,
                OCCURRED_AT));

        List<RealtimeForumEvent> threadEvents =
            captureSharedEvents(
                THREAD_DESTINATION,
                1);

        List<RealtimeForumEvent> forumEvents =
            captureSharedEvents(
                FORUM_DESTINATION,
                1);

        assertQuestionUpsert(
            threadEvents.getFirst(),
            question);

        assertQuestionUpsert(
            forumEvents.getFirst(),
            question);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void privateStatusChange_shouldUpdateAuthorOnly() {
        QuestionThreadResponseDTO question =
            question(
                PRIVATE,
                ANSWERED,
                OPEN,
                REVIEWER_ID,
                5L);

        handler.handle(
            new QuestionStatusChangedDomainEvent(
                question,
                IN_REVIEW,
                ANSWERED,
                OCCURRED_AT));

        List<RealtimeForumEvent> personalEvents =
            capturePersonalEvents(1);

        assertQuestionUpsert(
            personalEvents.getFirst(),
            question);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void closingPublicQuestion_shouldKeepItInThreadAndForum() {
        QuestionThreadResponseDTO question =
            question(
                PUBLIC,
                ANSWERED,
                CLOSED,
                REVIEWER_ID,
                6L);

        handler.handle(
            new QuestionStateChangedDomainEvent(
                question,
                OPEN,
                CLOSED,
                OCCURRED_AT));

        List<RealtimeForumEvent> threadEvents =
            captureSharedEvents(
                THREAD_DESTINATION,
                1);

        List<RealtimeForumEvent> forumEvents =
            captureSharedEvents(
                FORUM_DESTINATION,
                1);

        assertEventTypes(
            threadEvents,
            QUESTION_UPSERTED);

        assertEventTypes(
            forumEvents,
            QUESTION_UPSERTED);

        assertQuestionUpsert(
            threadEvents.getFirst(),
            question);

        assertQuestionUpsert(
            forumEvents.getFirst(),
            question);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void privateStateChange_shouldUpdateAuthorOnly() {
        QuestionThreadResponseDTO question =
            question(
                PRIVATE,
                IN_REVIEW,
                CLOSED,
                REVIEWER_ID,
                6L);

        handler.handle(
            new QuestionStateChangedDomainEvent(
                question,
                OPEN,
                CLOSED,
                OCCURRED_AT));

        List<RealtimeForumEvent> personalEvents =
            capturePersonalEvents(1);

        assertQuestionUpsert(
            personalEvents.getFirst(),
            question);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void publicClaim_shouldUpdateThreadForumAndAuthor() {
        QuestionThreadResponseDTO question =
            question(
                PUBLIC,
                IN_REVIEW,
                OPEN,
                REVIEWER_ID,
                4L);

        handler.handle(
            new QuestionClaimedDomainEvent(
                question,
                null,
                REVIEWER_ID,
                OCCURRED_AT));

        List<RealtimeForumEvent> threadEvents =
            captureSharedEvents(
                THREAD_DESTINATION,
                1);

        List<RealtimeForumEvent> forumEvents =
            captureSharedEvents(
                FORUM_DESTINATION,
                1);

        List<RealtimeForumEvent> personalEvents =
            capturePersonalEvents(1);

        assertQuestionUpsert(
            threadEvents.getFirst(),
            question);

        assertQuestionUpsert(
            forumEvents.getFirst(),
            question);

        assertQuestionUpsert(
            personalEvents.getFirst(),
            question);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void privateClaim_shouldUpdateAuthorOnly() {
        QuestionThreadResponseDTO question =
            question(
                PRIVATE,
                IN_REVIEW,
                OPEN,
                REVIEWER_ID,
                4L);

        handler.handle(
            new QuestionClaimedDomainEvent(
                question,
                null,
                REVIEWER_ID,
                OCCURRED_AT));

        List<RealtimeForumEvent> personalEvents =
            capturePersonalEvents(1);

        assertQuestionUpsert(
            personalEvents.getFirst(),
            question);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    private List<RealtimeForumEvent> captureSharedEvents(
        String destination,
        int invocationCount) {

        ArgumentCaptor<Object> payloadCaptor =
            ArgumentCaptor.forClass(
                Object.class);

        verify(
            messagingOperations,
            times(invocationCount))
            .convertAndSend(
                eq(destination),
                payloadCaptor.capture());

        return payloadCaptor
            .getAllValues()
            .stream()
            .map(value -> assertInstanceOf(
                RealtimeForumEvent.class,
                value))
            .toList();
    }

    private List<RealtimeForumEvent> capturePersonalEvents(
        int invocationCount) {

        ArgumentCaptor<Object> payloadCaptor =
            ArgumentCaptor.forClass(
                Object.class);

        verify(
            messagingOperations,
            times(invocationCount))
            .convertAndSendToUser(
                eq(AUTHOR_ID.toString()),
                eq(PERSONAL_QUEUE),
                payloadCaptor.capture());

        return payloadCaptor
            .getAllValues()
            .stream()
            .map(value -> assertInstanceOf(
                RealtimeForumEvent.class,
                value))
            .toList();
    }

    private void assertQuestionUpsert(
        RealtimeForumEvent event,
        QuestionThreadResponseDTO expectedQuestion) {

        assertEventType(
            event,
            QUESTION_UPSERTED);

        QuestionUpsertPayload payload =
            assertInstanceOf(
                QuestionUpsertPayload.class,
                event.payload());

        assertSame(
            expectedQuestion,
            payload.question());
    }

    private void assertMessageCreated(
        RealtimeForumEvent event,
        QuestionMessageResponseDTO expectedMessage) {

        assertEventType(
            event,
            MESSAGE_CREATED);

        MessageCreatedPayload payload =
            assertInstanceOf(
                MessageCreatedPayload.class,
                event.payload());

        assertSame(
            expectedMessage,
            payload.message());
    }

    private void assertEventTypes(
        List<RealtimeForumEvent> events,
        RealtimeEventType... expectedTypes) {

        assertEquals(
            List.of(expectedTypes),
            events.stream()
                .map(RealtimeForumEvent::type)
                .toList());
    }

    private void assertEventType(
        RealtimeForumEvent event,
        RealtimeEventType expectedType) {

        assertAll(
            () -> assertNotNull(
                event.eventId()),
            () -> assertEquals(
                expectedType,
                event.type()),
            () -> assertEquals(
                OCCURRED_AT,
                event.occurredAt()),
            () -> assertEquals(
                TASK_ASSIGNMENT_ID,
                event.taskAssignmentId()),
            () -> assertEquals(
                QUESTION_ID,
                event.questionId()));
    }

    @SafeVarargs
    private void assertUniqueEventIds(
        List<RealtimeForumEvent>... eventGroups) {

        List<RealtimeForumEvent> events =
            java.util.Arrays.stream(
                eventGroups)
                .flatMap(List::stream)
                .toList();

        Set<UUID> uniqueEventIds =
            new HashSet<>();

        events.forEach(event -> uniqueEventIds.add(
            event.eventId()));

        assertEquals(
            events.size(),
            uniqueEventIds.size());
    }

    private QuestionThreadResponseDTO question(
        QuestionVisibility visibility,
        QuestionStatus status,
        QuestionState state,
        Long reviewerId,
        Long version) {

        return new QuestionThreadResponseDTO(
            QUESTION_ID,
            TASK_ASSIGNMENT_ID,
            AUTHOR_ID,
            reviewerId,
            "Question title",
            "Protected question content",
            status,
            visibility,
            state,
            version,
            CREATED_AT,
            UPDATED_AT);
    }

    private QuestionMessageResponseDTO message(
        QuestionMessageType type) {

        return new QuestionMessageResponseDTO(
            MESSAGE_ID,
            QUESTION_ID,
            type == COMMENT
                ? AUTHOR_ID
                : REVIEWER_ID,
            type,
            type == COMMENT
                ? "Participant comment"
                : "Official answer",
            OCCURRED_AT);
    }
}