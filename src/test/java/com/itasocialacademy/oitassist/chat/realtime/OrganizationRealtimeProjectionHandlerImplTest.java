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
import com.itasocialacademy.oitassist.chat.realtime.handlers.OrganizationRealtimeProjectionHandlerImpl;
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
class OrganizationRealtimeProjectionHandlerImplTest {

    private static final Long TASK_ASSIGNMENT_ID = 10L;
    private static final Long QUESTION_ID = 20L;
    private static final Long AUTHOR_ID = 30L;

    private static final Long RESPONDER_ID = 40L;
    private static final Long OTHER_RESPONDER_ID = 41L;
    private static final Long UNRELATED_ORG_ID = 42L;
    private static final Long ADMINISTRATOR_ID = 90L;

    private static final Long MESSAGE_ID = 50L;

    private static final String PERSONAL_REVIEWS_QUEUE =
        "/queue/reviews";

    private static final Instant CREATED_AT =
        Instant.parse(
            "2026-08-05T10:00:00Z");

    private static final Instant UPDATED_AT =
        Instant.parse(
            "2026-08-05T10:15:00Z");

    private static final Instant OCCURRED_AT =
        Instant.parse(
            "2026-08-05T10:16:00Z");

    @Mock
    private SimpMessageSendingOperations messagingOperations;

    @Mock
    private OrganizationRealtimeRecipientResolver recipientResolver;

    @InjectMocks
    private OrganizationRealtimeProjectionHandlerImpl handler;

    @Test
    void privateQuestionCreation_shouldUpsertEveryEligibleResponder() {

        QuestionThreadResponseDTO question =
            question(
                PRIVATE,
                NEW,
                OPEN,
                null,
                0L);

        when(recipientResolver
            .resolveInboxRecipients(
                TASK_ASSIGNMENT_ID))
            .thenReturn(
                List.of(
                    RESPONDER_ID,
                    OTHER_RESPONDER_ID));

        handler.handle(
            new QuestionCreatedDomainEvent(
                question,
                OCCURRED_AT));

        List<RealtimeForumEvent> firstResponderEvents =
            captureResponderEvents(
                RESPONDER_ID,
                1);

        List<RealtimeForumEvent> secondResponderEvents =
            captureResponderEvents(
                OTHER_RESPONDER_ID,
                1);

        assertInboxUpsert(
            firstResponderEvents.getFirst(),
            question);

        assertInboxUpsert(
            secondResponderEvents.getFirst(),
            question);

        verify(
            messagingOperations,
            never())
            .convertAndSendToUser(
                eq(UNRELATED_ORG_ID.toString()),
                eq(PERSONAL_REVIEWS_QUEUE),
                any());

        assertUniqueEventIds(
            firstResponderEvents,
            secondResponderEvents);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void publicQuestionCreation_shouldAlsoUpsertEligibleResponder() {

        QuestionThreadResponseDTO question =
            question(
                PUBLIC,
                NEW,
                OPEN,
                null,
                0L);

        when(recipientResolver
            .resolveInboxRecipients(
                TASK_ASSIGNMENT_ID))
            .thenReturn(
                List.of(
                    RESPONDER_ID));

        handler.handle(
            new QuestionCreatedDomainEvent(
                question,
                OCCURRED_AT));

        List<RealtimeForumEvent> events =
            captureResponderEvents(
                RESPONDER_ID,
                1);

        assertInboxUpsert(
            events.getFirst(),
            question);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void questionCreation_withoutEligibleResponders_shouldSendNothing() {

        QuestionThreadResponseDTO question =
            question(
                PRIVATE,
                NEW,
                OPEN,
                null,
                0L);

        when(recipientResolver
            .resolveInboxRecipients(
                TASK_ASSIGNMENT_ID))
            .thenReturn(
                List.of());

        handler.handle(
            new QuestionCreatedDomainEvent(
                question,
                OCCURRED_AT));

        verifyNoInteractions(
            messagingOperations);
    }

    @Test
    void organizationClaim_shouldRemoveFromAllInboxesAndUpdateOnlyClaimant() {

        QuestionThreadResponseDTO question =
            question(
                PRIVATE,
                IN_REVIEW,
                OPEN,
                RESPONDER_ID,
                1L);

        when(recipientResolver
            .resolveInboxRecipients(
                TASK_ASSIGNMENT_ID))
            .thenReturn(
                List.of(
                    RESPONDER_ID,
                    OTHER_RESPONDER_ID));

        when(recipientResolver
            .isOrganizationResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(true);

        handler.handle(
            new QuestionClaimedDomainEvent(
                question,
                null,
                RESPONDER_ID,
                OCCURRED_AT));

        List<RealtimeForumEvent> claimantEvents =
            captureResponderEvents(
                RESPONDER_ID,
                2);

        List<RealtimeForumEvent> otherResponderEvents =
            captureResponderEvents(
                OTHER_RESPONDER_ID,
                1);

        assertEventTypes(
            claimantEvents,
            INBOX_REMOVED,
            REVIEW_UPDATED);

        assertInboxRemoval(
            claimantEvents.get(0));

        assertReviewUpdated(
            claimantEvents.get(1),
            question);

        assertInboxRemoval(
            otherResponderEvents.getFirst());

        verify(
            messagingOperations,
            never())
            .convertAndSendToUser(
                eq(UNRELATED_ORG_ID.toString()),
                eq(PERSONAL_REVIEWS_QUEUE),
                any());

        assertUniqueEventIds(
            claimantEvents,
            otherResponderEvents);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void administratorClaim_shouldRemoveFromOrgInboxesWithoutReviewUpdate() {

        QuestionThreadResponseDTO question =
            question(
                PRIVATE,
                IN_REVIEW,
                OPEN,
                ADMINISTRATOR_ID,
                1L);

        when(recipientResolver
            .resolveInboxRecipients(
                TASK_ASSIGNMENT_ID))
            .thenReturn(
                List.of(
                    RESPONDER_ID,
                    OTHER_RESPONDER_ID));

        when(recipientResolver
            .isOrganizationResponder(
                TASK_ASSIGNMENT_ID,
                ADMINISTRATOR_ID))
            .thenReturn(false);

        handler.handle(
            new QuestionClaimedDomainEvent(
                question,
                null,
                ADMINISTRATOR_ID,
                OCCURRED_AT));

        List<RealtimeForumEvent> firstResponderEvents =
            captureResponderEvents(
                RESPONDER_ID,
                1);

        List<RealtimeForumEvent> secondResponderEvents =
            captureResponderEvents(
                OTHER_RESPONDER_ID,
                1);

        assertInboxRemoval(
            firstResponderEvents.getFirst());

        assertInboxRemoval(
            secondResponderEvents.getFirst());

        verify(
            messagingOperations,
            never())
            .convertAndSendToUser(
                eq(ADMINISTRATOR_ID.toString()),
                eq(PERSONAL_REVIEWS_QUEUE),
                any());

        assertUniqueEventIds(
            firstResponderEvents,
            secondResponderEvents);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void assignedComment_shouldNotifyAssignedOrgResponderOnly() {

        QuestionThreadResponseDTO question =
            question(
                PRIVATE,
                IN_REVIEW,
                OPEN,
                RESPONDER_ID,
                2L);

        QuestionMessageResponseDTO message =
            message(COMMENT);

        when(recipientResolver
            .isOrganizationResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(true);

        handler.handle(
            new CommentCreatedDomainEvent(
                question,
                message,
                OCCURRED_AT));

        List<RealtimeForumEvent> events =
            captureResponderEvents(
                RESPONDER_ID,
                1);

        assertMessageCreated(
            events.getFirst(),
            message);

        verify(
            messagingOperations,
            never())
            .convertAndSendToUser(
                eq(OTHER_RESPONDER_ID.toString()),
                eq(PERSONAL_REVIEWS_QUEUE),
                any());

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void administratorAssignedComment_shouldProduceNoOrgProjection() {

        QuestionThreadResponseDTO question =
            question(
                PRIVATE,
                IN_REVIEW,
                OPEN,
                ADMINISTRATOR_ID,
                2L);

        when(recipientResolver
            .isOrganizationResponder(
                TASK_ASSIGNMENT_ID,
                ADMINISTRATOR_ID))
            .thenReturn(false);

        handler.handle(
            new CommentCreatedDomainEvent(
                question,
                message(COMMENT),
                OCCURRED_AT));

        verifyNoInteractions(
            messagingOperations);
    }

    @Test
    void unassignedComment_shouldProduceNoOrgProjection() {

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
    void officialAnswer_shouldRemoveInboxAndUpdateAssignedOrgResponder() {

        QuestionThreadResponseDTO question =
            question(
                PRIVATE,
                ANSWERED,
                OPEN,
                RESPONDER_ID,
                3L);

        QuestionMessageResponseDTO message =
            message(OFFICIAL_ANSWER);

        when(recipientResolver
            .resolveInboxRecipients(
                TASK_ASSIGNMENT_ID))
            .thenReturn(
                List.of(
                    RESPONDER_ID,
                    OTHER_RESPONDER_ID));

        when(recipientResolver
            .isOrganizationResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(true);

        handler.handle(
            new OfficialAnswerPublishedDomainEvent(
                question,
                message,
                IN_REVIEW,
                ANSWERED,
                OCCURRED_AT));

        List<RealtimeForumEvent> reviewerEvents =
            captureResponderEvents(
                RESPONDER_ID,
                3);

        List<RealtimeForumEvent> otherResponderEvents =
            captureResponderEvents(
                OTHER_RESPONDER_ID,
                1);

        assertEventTypes(
            reviewerEvents,
            INBOX_REMOVED,
            MESSAGE_CREATED,
            REVIEW_UPDATED);

        assertInboxRemoval(
            reviewerEvents.get(0));

        assertMessageCreated(
            reviewerEvents.get(1),
            message);

        assertReviewUpdated(
            reviewerEvents.get(2),
            question);

        assertInboxRemoval(
            otherResponderEvents.getFirst());

        assertUniqueEventIds(
            reviewerEvents,
            otherResponderEvents);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void officialAnswer_withoutOrgReviewer_shouldOnlyRemoveInbox() {

        QuestionThreadResponseDTO question =
            question(
                PRIVATE,
                ANSWERED,
                OPEN,
                ADMINISTRATOR_ID,
                3L);

        when(recipientResolver
            .resolveInboxRecipients(
                TASK_ASSIGNMENT_ID))
            .thenReturn(
                List.of(
                    RESPONDER_ID));

        when(recipientResolver
            .isOrganizationResponder(
                TASK_ASSIGNMENT_ID,
                ADMINISTRATOR_ID))
            .thenReturn(false);

        handler.handle(
            new OfficialAnswerPublishedDomainEvent(
                question,
                message(OFFICIAL_ANSWER),
                IN_REVIEW,
                ANSWERED,
                OCCURRED_AT));

        List<RealtimeForumEvent> events =
            captureResponderEvents(
                RESPONDER_ID,
                1);

        assertInboxRemoval(
            events.getFirst());

        verify(
            messagingOperations,
            never())
            .convertAndSendToUser(
                eq(ADMINISTRATOR_ID.toString()),
                eq(PERSONAL_REVIEWS_QUEUE),
                any());

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void eligibleVisibilityChange_shouldUpsertCurrentSummary() {

        QuestionThreadResponseDTO question =
            question(
                PUBLIC,
                NEW,
                OPEN,
                null,
                4L);

        when(recipientResolver
            .resolveInboxRecipients(
                TASK_ASSIGNMENT_ID))
            .thenReturn(
                List.of(
                    RESPONDER_ID,
                    OTHER_RESPONDER_ID));

        handler.handle(
            new QuestionVisibilityChangedDomainEvent(
                question,
                PRIVATE,
                PUBLIC,
                OCCURRED_AT));

        List<RealtimeForumEvent> firstResponderEvents =
            captureResponderEvents(
                RESPONDER_ID,
                1);

        List<RealtimeForumEvent> secondResponderEvents =
            captureResponderEvents(
                OTHER_RESPONDER_ID,
                1);

        assertInboxUpsert(
            firstResponderEvents.getFirst(),
            question);

        assertInboxUpsert(
            secondResponderEvents.getFirst(),
            question);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void assignedVisibilityChange_shouldUpdateAssignedOrgResponderOnly() {

        QuestionThreadResponseDTO question =
            question(
                PUBLIC,
                IN_REVIEW,
                OPEN,
                RESPONDER_ID,
                4L);

        when(recipientResolver
            .isOrganizationResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(true);

        handler.handle(
            new QuestionVisibilityChangedDomainEvent(
                question,
                PRIVATE,
                PUBLIC,
                OCCURRED_AT));

        List<RealtimeForumEvent> events =
            captureResponderEvents(
                RESPONDER_ID,
                1);

        assertReviewUpdated(
            events.getFirst(),
            question);

        verify(
            recipientResolver,
            never())
            .resolveInboxRecipients(
                TASK_ASSIGNMENT_ID);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void closingOpenQuestion_shouldRemoveFromEveryResponderInbox() {

        QuestionThreadResponseDTO question =
            question(
                PRIVATE,
                NEW,
                CLOSED,
                null,
                5L);

        when(recipientResolver
            .resolveInboxRecipients(
                TASK_ASSIGNMENT_ID))
            .thenReturn(
                List.of(
                    RESPONDER_ID,
                    OTHER_RESPONDER_ID));

        handler.handle(
            new QuestionStateChangedDomainEvent(
                question,
                OPEN,
                CLOSED,
                OCCURRED_AT));

        List<RealtimeForumEvent> firstResponderEvents =
            captureResponderEvents(
                RESPONDER_ID,
                1);

        List<RealtimeForumEvent> secondResponderEvents =
            captureResponderEvents(
                OTHER_RESPONDER_ID,
                1);

        assertInboxRemoval(
            firstResponderEvents.getFirst());

        assertInboxRemoval(
            secondResponderEvents.getFirst());

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void reopeningNewUnassignedQuestion_shouldUpsertEveryResponderInbox() {

        QuestionThreadResponseDTO question =
            question(
                PUBLIC,
                NEW,
                OPEN,
                null,
                6L);

        when(recipientResolver
            .resolveInboxRecipients(
                TASK_ASSIGNMENT_ID))
            .thenReturn(
                List.of(
                    RESPONDER_ID,
                    OTHER_RESPONDER_ID));

        handler.handle(
            new QuestionStateChangedDomainEvent(
                question,
                CLOSED,
                OPEN,
                OCCURRED_AT));

        List<RealtimeForumEvent> firstResponderEvents =
            captureResponderEvents(
                RESPONDER_ID,
                1);

        List<RealtimeForumEvent> secondResponderEvents =
            captureResponderEvents(
                OTHER_RESPONDER_ID,
                1);

        assertInboxUpsert(
            firstResponderEvents.getFirst(),
            question);

        assertInboxUpsert(
            secondResponderEvents.getFirst(),
            question);

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void statusChangedAwayFromNew_shouldRemoveFromEveryResponderInbox() {

        QuestionThreadResponseDTO question =
            question(
                PRIVATE,
                IN_REVIEW,
                OPEN,
                null,
                7L);

        when(recipientResolver
            .resolveInboxRecipients(
                TASK_ASSIGNMENT_ID))
            .thenReturn(
                List.of(
                    RESPONDER_ID,
                    OTHER_RESPONDER_ID));

        handler.handle(
            new QuestionStatusChangedDomainEvent(
                question,
                NEW,
                IN_REVIEW,
                OCCURRED_AT));

        List<RealtimeForumEvent> firstResponderEvents =
            captureResponderEvents(
                RESPONDER_ID,
                1);

        List<RealtimeForumEvent> secondResponderEvents =
            captureResponderEvents(
                OTHER_RESPONDER_ID,
                1);

        assertInboxRemoval(
            firstResponderEvents.getFirst());

        assertInboxRemoval(
            secondResponderEvents.getFirst());

        verifyNoMoreInteractions(
            messagingOperations);
    }

    @Test
    void assignedStatusChange_shouldRemoveInboxAndUpdateAssignedResponder() {

        QuestionThreadResponseDTO question =
            question(
                PRIVATE,
                ANSWERED,
                OPEN,
                RESPONDER_ID,
                8L);

        when(recipientResolver
            .resolveInboxRecipients(
                TASK_ASSIGNMENT_ID))
            .thenReturn(
                List.of(
                    RESPONDER_ID,
                    OTHER_RESPONDER_ID));

        when(recipientResolver
            .isOrganizationResponder(
                TASK_ASSIGNMENT_ID,
                RESPONDER_ID))
            .thenReturn(true);

        handler.handle(
            new QuestionStatusChangedDomainEvent(
                question,
                IN_REVIEW,
                ANSWERED,
                OCCURRED_AT));

        List<RealtimeForumEvent> reviewerEvents =
            captureResponderEvents(
                RESPONDER_ID,
                2);

        List<RealtimeForumEvent> otherResponderEvents =
            captureResponderEvents(
                OTHER_RESPONDER_ID,
                1);

        assertEventTypes(
            reviewerEvents,
            INBOX_REMOVED,
            REVIEW_UPDATED);

        assertInboxRemoval(
            reviewerEvents.get(0));

        assertReviewUpdated(
            reviewerEvents.get(1),
            question);

        assertInboxRemoval(
            otherResponderEvents.getFirst());

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
                9L);

        when(recipientResolver
            .resolveInboxRecipients(
                TASK_ASSIGNMENT_ID))
            .thenReturn(
                List.of(
                    RESPONDER_ID));

        handler.handle(
            new QuestionStateChangedDomainEvent(
                question,
                OPEN,
                CLOSED,
                OCCURRED_AT));

        RealtimeForumEvent event =
            captureResponderEvents(
                RESPONDER_ID,
                1)
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

    private List<RealtimeForumEvent> captureResponderEvents(
        Long responderId,
        int invocationCount) {

        ArgumentCaptor<Object> payloadCaptor =
            ArgumentCaptor.forClass(
                Object.class);

        verify(
            messagingOperations,
            times(invocationCount))
            .convertAndSendToUser(
                eq(responderId.toString()),
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

        assertReviewSummary(
            payload.question(),
            question);
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

        assertReviewSummary(
            payload.question(),
            question);
    }

    private void assertReviewSummary(
        QuestionReviewInboxItemResponseDTO summary,
        QuestionThreadResponseDTO question) {

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
                : RESPONDER_ID,
            type,
            type == COMMENT
                ? "Participant comment"
                : "Official answer",
            OCCURRED_AT);
    }
}