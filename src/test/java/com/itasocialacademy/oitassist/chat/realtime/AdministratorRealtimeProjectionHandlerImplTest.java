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
import static com.itasocialacademy.oitassist.chat.realtime.event.RealtimeEventType.INBOX_REMOVED;
import static com.itasocialacademy.oitassist.chat.realtime.event.RealtimeEventType.INBOX_UPSERTED;
import static com.itasocialacademy.oitassist.chat.realtime.event.RealtimeEventType.MESSAGE_CREATED;
import static com.itasocialacademy.oitassist.chat.realtime.event.RealtimeEventType.REVIEW_UPDATED;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionReviewInboxItemResponseDTO;
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
import com.itasocialacademy.oitassist.chat.realtime.handlers.AdministratorRealtimeProjectionHandlerImpl;
import com.itasocialacademy.oitassist.chat.realtime.handlers.OrganizationRealtimeRecipientResolver;
import com.itasocialacademy.oitassist.chat.realtime.event.InboxRemovalPayload;
import com.itasocialacademy.oitassist.chat.realtime.event.InboxUpsertPayload;
import com.itasocialacademy.oitassist.chat.realtime.event.MessageCreatedPayload;
import com.itasocialacademy.oitassist.chat.realtime.event.RealtimeEventType;
import com.itasocialacademy.oitassist.chat.realtime.event.RealtimeForumEvent;
import com.itasocialacademy.oitassist.chat.realtime.event.ReviewUpdatePayload;
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
class AdministratorRealtimeProjectionHandlerImplTest {

    private static final Long TASK_ASSIGNMENT_ID = 10L;
    private static final Long QUESTION_ID = 20L;
    private static final Long AUTHOR_ID = 30L;
    private static final Long REVIEWER_ID = 40L;
    private static final Long OTHER_REVIEWER_ID = 41L;
    private static final Long MESSAGE_ID = 50L;

    private static final String INBOX_DESTINATION =
        "/topic/admin/questions/inbox";

    private static final String PERSONAL_REVIEWS_QUEUE =
        "/queue/reviews";

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

    @Mock
    private OrganizationRealtimeRecipientResolver organizationRecipientResolver;

    @InjectMocks
    private AdministratorRealtimeProjectionHandlerImpl handler;

    @Test
    void privateQuestionCreation_shouldUpsertSharedInbox() {

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

        List<RealtimeForumEvent> inboxEvents =
            captureInboxEvents(1);

        assertInboxUpsert(
            inboxEvents.getFirst(),
            question);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void publicQuestionCreation_shouldAlsoUpsertSharedInbox() {

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

        List<RealtimeForumEvent> inboxEvents =
            captureInboxEvents(1);

        assertInboxUpsert(
            inboxEvents.getFirst(),
            question);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void claim_shouldRemoveFromInboxAndUpdateOnlyClaimant() {

        QuestionThreadResponseDTO question =
            question(
                PRIVATE,
                IN_REVIEW,
                OPEN,
                REVIEWER_ID,
                1L);

        handler.handle(
            new QuestionClaimedDomainEvent(
                question,
                null,
                REVIEWER_ID,
                OCCURRED_AT));

        List<RealtimeForumEvent> inboxEvents =
            captureInboxEvents(1);

        List<RealtimeForumEvent> reviewerEvents =
            captureReviewerEvents(
                REVIEWER_ID,
                1);

        assertInboxRemoval(
            inboxEvents.getFirst());

        assertReviewUpdated(
            reviewerEvents.getFirst(),
            question);

        verify(
            messagingOperations,
            never())
            .convertAndSendToUser(
                eq(OTHER_REVIEWER_ID.toString()),
                eq(PERSONAL_REVIEWS_QUEUE),
                any());

        assertUniqueEventIds(
            inboxEvents,
            reviewerEvents);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void orgClaim_shouldRemoveSharedInboxWithoutDuplicatePersonalReview() {

        QuestionThreadResponseDTO question =
            question(
                PRIVATE,
                IN_REVIEW,
                OPEN,
                REVIEWER_ID,
                1L);

        when(organizationRecipientResolver
            .isOrganizationResponder(
                TASK_ASSIGNMENT_ID,
                REVIEWER_ID))
            .thenReturn(true);

        handler.handle(
            new QuestionClaimedDomainEvent(
                question,
                null,
                REVIEWER_ID,
                OCCURRED_AT));

        List<RealtimeForumEvent> inboxEvents =
            captureInboxEvents(1);

        assertInboxRemoval(
            inboxEvents.getFirst());

        verify(
            messagingOperations,
            never())
            .convertAndSendToUser(
                eq(REVIEWER_ID.toString()),
                eq(PERSONAL_REVIEWS_QUEUE),
                any());

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void assignedComment_shouldNotifyAssignedReviewerOnly() {

        QuestionThreadResponseDTO question =
            question(
                PRIVATE,
                IN_REVIEW,
                OPEN,
                REVIEWER_ID,
                2L);

        QuestionMessageResponseDTO message =
            message(COMMENT);

        handler.handle(
            new CommentCreatedDomainEvent(
                question,
                message,
                OCCURRED_AT));

        List<RealtimeForumEvent> reviewerEvents =
            captureReviewerEvents(
                REVIEWER_ID,
                1);

        assertMessageCreated(
            reviewerEvents.getFirst(),
            message);

        verify(
            messagingOperations,
            never())
            .convertAndSendToUser(
                eq(OTHER_REVIEWER_ID.toString()),
                eq(PERSONAL_REVIEWS_QUEUE),
                any());

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void orgAssignedComment_shouldNotProduceAdministratorPersonalProjection() {

        QuestionThreadResponseDTO question =
            question(
                PRIVATE,
                IN_REVIEW,
                OPEN,
                REVIEWER_ID,
                2L);

        when(organizationRecipientResolver
            .isOrganizationResponder(
                TASK_ASSIGNMENT_ID,
                REVIEWER_ID))
            .thenReturn(true);

        handler.handle(
            new CommentCreatedDomainEvent(
                question,
                message(COMMENT),
                OCCURRED_AT));

        verifyNoInteractions(
            messagingOperations);
    }

    @Test
    void unassignedComment_shouldProduceNoAdministratorProjection() {

        QuestionThreadResponseDTO question =
            question(
                PUBLIC,
                NEW,
                OPEN,
                null,
                2L);

        handler.handle(
            new CommentCreatedDomainEvent(
                question,
                message(COMMENT),
                OCCURRED_AT));

        verifyNoInteractions(
            messagingOperations);
    }

    @Test
    void assignedOfficialAnswer_shouldRemoveInboxAndUpdateReviewer() {

        QuestionThreadResponseDTO question =
            question(
                PRIVATE,
                ANSWERED,
                OPEN,
                REVIEWER_ID,
                3L);

        QuestionMessageResponseDTO message =
            message(OFFICIAL_ANSWER);

        handler.handle(
            new OfficialAnswerPublishedDomainEvent(
                question,
                message,
                IN_REVIEW,
                ANSWERED,
                OCCURRED_AT));

        List<RealtimeForumEvent> inboxEvents =
            captureInboxEvents(1);

        List<RealtimeForumEvent> reviewerEvents =
            captureReviewerEvents(
                REVIEWER_ID,
                2);

        assertInboxRemoval(
            inboxEvents.getFirst());

        assertEventTypes(
            reviewerEvents,
            MESSAGE_CREATED,
            REVIEW_UPDATED);

        assertMessageCreated(
            reviewerEvents.get(0),
            message);

        assertReviewUpdated(
            reviewerEvents.get(1),
            question);

        assertUniqueEventIds(
            inboxEvents,
            reviewerEvents);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void orgAssignedOfficialAnswer_shouldOnlyRemoveSharedInbox() {

        QuestionThreadResponseDTO question =
            question(
                PRIVATE,
                ANSWERED,
                OPEN,
                REVIEWER_ID,
                3L);

        when(organizationRecipientResolver
            .isOrganizationResponder(
                TASK_ASSIGNMENT_ID,
                REVIEWER_ID))
            .thenReturn(true);

        handler.handle(
            new OfficialAnswerPublishedDomainEvent(
                question,
                message(OFFICIAL_ANSWER),
                IN_REVIEW,
                ANSWERED,
                OCCURRED_AT));

        List<RealtimeForumEvent> inboxEvents =
            captureInboxEvents(1);

        assertInboxRemoval(
            inboxEvents.getFirst());

        verify(
            messagingOperations,
            never())
            .convertAndSendToUser(
                eq(REVIEWER_ID.toString()),
                eq(PERSONAL_REVIEWS_QUEUE),
                any());

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void unassignedOfficialAnswer_shouldRemoveInboxWithoutPersonalContent() {

        QuestionThreadResponseDTO question =
            question(
                PUBLIC,
                ANSWERED,
                OPEN,
                null,
                3L);

        handler.handle(
            new OfficialAnswerPublishedDomainEvent(
                question,
                message(OFFICIAL_ANSWER),
                NEW,
                ANSWERED,
                OCCURRED_AT));

        List<RealtimeForumEvent> inboxEvents =
            captureInboxEvents(1);

        assertInboxRemoval(
            inboxEvents.getFirst());

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void statusChangedToEligibleNew_shouldUpsertInbox() {

        QuestionThreadResponseDTO question =
            question(
                PRIVATE,
                NEW,
                OPEN,
                null,
                4L);

        handler.handle(
            new QuestionStatusChangedDomainEvent(
                question,
                IN_REVIEW,
                NEW,
                OCCURRED_AT));

        List<RealtimeForumEvent> inboxEvents =
            captureInboxEvents(1);

        assertInboxUpsert(
            inboxEvents.getFirst(),
            question);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void statusChangedAwayFromNew_shouldRemoveInbox() {

        QuestionThreadResponseDTO question =
            question(
                PUBLIC,
                IN_REVIEW,
                OPEN,
                null,
                4L);

        handler.handle(
            new QuestionStatusChangedDomainEvent(
                question,
                NEW,
                IN_REVIEW,
                OCCURRED_AT));

        List<RealtimeForumEvent> inboxEvents =
            captureInboxEvents(1);

        assertInboxRemoval(
            inboxEvents.getFirst());

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void assignedStatusChange_shouldRemoveInboxAndUpdateReviewer() {

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

        List<RealtimeForumEvent> inboxEvents =
            captureInboxEvents(1);

        List<RealtimeForumEvent> reviewerEvents =
            captureReviewerEvents(
                REVIEWER_ID,
                1);

        assertInboxRemoval(
            inboxEvents.getFirst());

        assertReviewUpdated(
            reviewerEvents.getFirst(),
            question);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void closingOpenQuestion_shouldRemoveFromInbox() {

        QuestionThreadResponseDTO question =
            question(
                PRIVATE,
                NEW,
                CLOSED,
                null,
                4L);

        handler.handle(
            new QuestionStateChangedDomainEvent(
                question,
                OPEN,
                CLOSED,
                OCCURRED_AT));

        List<RealtimeForumEvent> inboxEvents =
            captureInboxEvents(1);

        assertInboxRemoval(
            inboxEvents.getFirst());

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void reopeningEligibleQuestion_shouldReturnItToInbox() {

        QuestionThreadResponseDTO question =
            question(
                PUBLIC,
                NEW,
                OPEN,
                null,
                5L);

        handler.handle(
            new QuestionStateChangedDomainEvent(
                question,
                CLOSED,
                OPEN,
                OCCURRED_AT));

        List<RealtimeForumEvent> inboxEvents =
            captureInboxEvents(1);

        assertInboxUpsert(
            inboxEvents.getFirst(),
            question);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void reopeningAssignedQuestion_shouldNotReturnItToSharedInbox() {

        QuestionThreadResponseDTO question =
            question(
                PRIVATE,
                NEW,
                OPEN,
                REVIEWER_ID,
                5L);

        handler.handle(
            new QuestionStateChangedDomainEvent(
                question,
                CLOSED,
                OPEN,
                OCCURRED_AT));

        List<RealtimeForumEvent> inboxEvents =
            captureInboxEvents(1);

        List<RealtimeForumEvent> reviewerEvents =
            captureReviewerEvents(
                REVIEWER_ID,
                1);

        assertInboxRemoval(
            inboxEvents.getFirst());

        assertReviewUpdated(
            reviewerEvents.getFirst(),
            question);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void eligibleVisibilityChange_shouldSynchronizeInboxSummary() {

        QuestionThreadResponseDTO question =
            question(
                PUBLIC,
                NEW,
                OPEN,
                null,
                6L);

        handler.handle(
            new QuestionVisibilityChangedDomainEvent(
                question,
                PRIVATE,
                PUBLIC,
                OCCURRED_AT));

        List<RealtimeForumEvent> inboxEvents =
            captureInboxEvents(1);

        assertInboxUpsert(
            inboxEvents.getFirst(),
            question);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void assignedVisibilityChange_shouldUpdateAssignedReviewerOnly() {

        QuestionThreadResponseDTO question =
            question(
                PUBLIC,
                IN_REVIEW,
                OPEN,
                REVIEWER_ID,
                6L);

        handler.handle(
            new QuestionVisibilityChangedDomainEvent(
                question,
                PRIVATE,
                PUBLIC,
                OCCURRED_AT));

        List<RealtimeForumEvent> reviewerEvents =
            captureReviewerEvents(
                REVIEWER_ID,
                1);

        assertReviewUpdated(
            reviewerEvents.getFirst(),
            question);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void inboxRemovalPayload_shouldContainIdentifiersOnly() {

        QuestionThreadResponseDTO question =
            question(
                PRIVATE,
                ANSWERED,
                CLOSED,
                null,
                7L);

        handler.handle(
            new QuestionStateChangedDomainEvent(
                question,
                OPEN,
                CLOSED,
                OCCURRED_AT));

        RealtimeForumEvent event =
            captureInboxEvents(1)
                .getFirst();

        InboxRemovalPayload payload =
            assertInstanceOf(
                InboxRemovalPayload.class,
                event.payload());

        assertAll(
            () -> assertEquals(
                TASK_ASSIGNMENT_ID,
                payload.taskAssignmentId()),
            () -> assertEquals(
                QUESTION_ID,
                payload.questionId()));

        verifyNoMoreInteractions(
            messagingOperations);
    }

    private List<RealtimeForumEvent> captureInboxEvents(
        int invocationCount) {

        ArgumentCaptor<Object> payloadCaptor =
            ArgumentCaptor.forClass(
                Object.class);

        verify(
            messagingOperations,
            times(invocationCount))
            .convertAndSend(
                eq(INBOX_DESTINATION),
                payloadCaptor.capture());

        return toRealtimeEvents(
            payloadCaptor.getAllValues());
    }

    private List<RealtimeForumEvent> captureReviewerEvents(
        Long reviewerId,
        int invocationCount) {

        ArgumentCaptor<Object> payloadCaptor =
            ArgumentCaptor.forClass(
                Object.class);

        verify(
            messagingOperations,
            times(invocationCount))
            .convertAndSendToUser(
                eq(reviewerId.toString()),
                eq(PERSONAL_REVIEWS_QUEUE),
                payloadCaptor.capture());

        return toRealtimeEvents(
            payloadCaptor.getAllValues());
    }

    private List<RealtimeForumEvent> toRealtimeEvents(
        List<Object> values) {

        return values.stream()
            .map(value -> assertInstanceOf(
                RealtimeForumEvent.class,
                value))
            .toList();
    }

    private void assertInboxUpsert(
        RealtimeForumEvent event,
        QuestionThreadResponseDTO question) {

        assertEventType(
            event,
            INBOX_UPSERTED);

        InboxUpsertPayload payload =
            assertInstanceOf(
                InboxUpsertPayload.class,
                event.payload());

        QuestionReviewInboxItemResponseDTO summary =
            payload.question();

        assertAll(
            () -> assertEquals(
                question.id(),
                summary.id()),
            () -> assertEquals(
                question.taskAssignmentId(),
                summary.taskAssignmentId()),
            () -> assertEquals(
                question.authorId(),
                summary.authorId()),
            () -> assertEquals(
                question.assignedReviewerId(),
                summary.assignedReviewerId()),
            () -> assertEquals(
                question.title(),
                summary.title()),
            () -> assertEquals(
                question.status(),
                summary.status()),
            () -> assertEquals(
                question.state(),
                summary.state()),
            () -> assertEquals(
                question.visibility(),
                summary.visibility()),
            () -> assertEquals(
                question.version(),
                summary.version()),
            () -> assertEquals(
                question.createdAt(),
                summary.createdAt()),
            () -> assertEquals(
                question.updatedAt(),
                summary.updatedAt()));
    }

    private void assertInboxRemoval(
        RealtimeForumEvent event) {

        assertEventType(
            event,
            INBOX_REMOVED);

        InboxRemovalPayload payload =
            assertInstanceOf(
                InboxRemovalPayload.class,
                event.payload());

        assertAll(
            () -> assertEquals(
                TASK_ASSIGNMENT_ID,
                payload.taskAssignmentId()),
            () -> assertEquals(
                QUESTION_ID,
                payload.questionId()));
    }

    private void assertReviewUpdated(
        RealtimeForumEvent event,
        QuestionThreadResponseDTO question) {

        assertEventType(
            event,
            REVIEW_UPDATED);

        ReviewUpdatePayload payload =
            assertInstanceOf(
                ReviewUpdatePayload.class,
                event.payload());

        assertEquals(
            question.id(),
            payload.question().id());

        assertEquals(
            question.version(),
            payload.question().version());

        assertEquals(
            question.assignedReviewerId(),
            payload.question()
                .assignedReviewerId());
    }

    private void assertMessageCreated(
        RealtimeForumEvent event,
        QuestionMessageResponseDTO message) {

        assertEventType(
            event,
            MESSAGE_CREATED);

        MessageCreatedPayload payload =
            assertInstanceOf(
                MessageCreatedPayload.class,
                event.payload());

        assertSame(
            message,
            payload.message());
    }

    private void assertEventTypes(
        List<RealtimeForumEvent> events,
        RealtimeEventType... expectedTypes) {

        assertEquals(
            List.of(expectedTypes),
            events.stream()
                .map(
                    RealtimeForumEvent::type)
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
    private final void assertUniqueEventIds(
        List<RealtimeForumEvent>... eventGroups) {

        List<RealtimeForumEvent> events =
            java.util.Arrays.stream(
                eventGroups)
                .flatMap(
                    List::stream)
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