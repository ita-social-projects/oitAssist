package com.itasocialacademy.oitassist.chat.realtime;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionMessageType.COMMENT;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.IN_REVIEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.NEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PUBLIC;
import static com.itasocialacademy.oitassist.chat.event.realtime.RealtimeEventType.MESSAGE_CREATED;
import static com.itasocialacademy.oitassist.chat.event.realtime.RealtimeEventType.QUESTION_UPSERTED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility;
import com.itasocialacademy.oitassist.chat.event.domain.CommentCreatedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.domain.ForumDomainEvent;
import com.itasocialacademy.oitassist.chat.event.domain.QuestionClaimedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.domain.QuestionCreatedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.domain.QuestionVisibilityChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.realtime.MessageCreatedPayload;
import com.itasocialacademy.oitassist.chat.event.realtime.QuestionUpsertPayload;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParticipantRealtimeHandlerTest {

    private static final Long QUESTION_ID = 10L;
    private static final Long TASK_ASSIGNMENT_ID = 20L;
    private static final Long AUTHOR_ID = 100L;
    private static final Long RESPONDER_ID = 200L;
    private static final Long ADMINISTRATOR_ID = 300L;

    private static final Instant OCCURRED_AT = Instant.parse("2026-10-01T10:00:00Z");

    @Mock
    private ForumRealtimePublisher publisher;

    @Mock
    private OrganizationRealtimeRecipientResolver organizationRecipientResolver;

    @InjectMocks
    private ParticipantRealtimeHandler participantRealtimeHandler;

    @Test
    void handle_commentOnPrivateQuestionAssignedToResponder_shouldSendMessageOnlyToAuthorAndAssignedResponder() {
        QuestionThreadResponseDTO question = question(RESPONDER_ID, IN_REVIEW, PRIVATE);
        CommentCreatedDomainEvent event = new CommentCreatedDomainEvent(question, comment(), OCCURRED_AT);

        when(organizationRecipientResolver.isOrganizationResponder(TASK_ASSIGNMENT_ID, RESPONDER_ID))
            .thenReturn(true);

        participantRealtimeHandler.handle(event);

        verify(publisher).toPersonalQuestions(
            eq(AUTHOR_ID), eq(event), eq(MESSAGE_CREATED), any(MessageCreatedPayload.class));
        verify(publisher).toPersonalQuestions(
            eq(RESPONDER_ID), eq(event), eq(MESSAGE_CREATED), any(MessageCreatedPayload.class));
        verify(organizationRecipientResolver, never()).resolveInboxRecipients(anyLong());
    }

    @Test
    void handle_commentOnUnclaimedPrivateQuestion_shouldNotSendMessageToResponders() {
        QuestionThreadResponseDTO question = question(null, NEW, PRIVATE);
        CommentCreatedDomainEvent event = new CommentCreatedDomainEvent(question, comment(), OCCURRED_AT);

        participantRealtimeHandler.handle(event);

        verify(publisher).toAdministratorAllQuestions(
            eq(event), eq(MESSAGE_CREATED), any(MessageCreatedPayload.class));
        verify(publisher).toPersonalQuestions(
            eq(AUTHOR_ID), eq(event), eq(MESSAGE_CREATED), any(MessageCreatedPayload.class));
        verifyNoPersonalQuestionProjectionExceptAuthor(event);
        verify(organizationRecipientResolver, never()).resolveInboxRecipients(anyLong());
    }

    @Test
    void handle_privateQuestionCreated_shouldNotSendSnapshotToResponders() {
        QuestionCreatedDomainEvent event =
            new QuestionCreatedDomainEvent(question(null, NEW, PRIVATE), OCCURRED_AT);

        participantRealtimeHandler.handle(event);

        verify(publisher).toPersonalQuestions(
            eq(AUTHOR_ID), eq(event), eq(QUESTION_UPSERTED), any(QuestionUpsertPayload.class));
        verifyNoPersonalQuestionProjectionExceptAuthor(event);
        verify(organizationRecipientResolver, never()).resolveInboxRecipients(anyLong());
    }

    @Test
    void handle_privateQuestionAssignedToAdministrator_shouldNotSendSnapshotToResponders() {
        QuestionClaimedDomainEvent event = new QuestionClaimedDomainEvent(
            question(ADMINISTRATOR_ID, IN_REVIEW, PRIVATE),
            null,
            ADMINISTRATOR_ID,
            OCCURRED_AT);

        when(organizationRecipientResolver.isOrganizationResponder(TASK_ASSIGNMENT_ID, ADMINISTRATOR_ID))
            .thenReturn(false);

        participantRealtimeHandler.handle(event);

        verify(publisher).toAdministratorAllQuestions(
            eq(event), eq(QUESTION_UPSERTED), any(QuestionUpsertPayload.class));
        verifyNoPersonalQuestionProjectionExceptAuthor(event);
    }

    @Test
    void handle_privateQuestionClaimedByResponder_shouldSendSnapshotToClaimant() {
        QuestionClaimedDomainEvent event = new QuestionClaimedDomainEvent(
            question(RESPONDER_ID, IN_REVIEW, PRIVATE),
            null,
            RESPONDER_ID,
            OCCURRED_AT);

        when(organizationRecipientResolver.isOrganizationResponder(TASK_ASSIGNMENT_ID, RESPONDER_ID))
            .thenReturn(true);

        participantRealtimeHandler.handle(event);

        verify(publisher).toPersonalQuestions(
            eq(RESPONDER_ID), eq(event), eq(QUESTION_UPSERTED), any(QuestionUpsertPayload.class));
        verify(organizationRecipientResolver, never()).resolveInboxRecipients(anyLong());
    }

    @Test
    void handle_questionMadePrivate_shouldSendSnapshotOnlyToAuthorAndAssignedResponder() {
        QuestionVisibilityChangedDomainEvent event = new QuestionVisibilityChangedDomainEvent(
            question(RESPONDER_ID, IN_REVIEW, PRIVATE),
            PUBLIC,
            PRIVATE,
            OCCURRED_AT);

        when(organizationRecipientResolver.isOrganizationResponder(TASK_ASSIGNMENT_ID, RESPONDER_ID))
            .thenReturn(true);

        participantRealtimeHandler.handle(event);

        verify(publisher).toPersonalQuestions(
            eq(AUTHOR_ID), eq(event), eq(QUESTION_UPSERTED), any(QuestionUpsertPayload.class));
        verify(publisher).toPersonalQuestions(
            eq(RESPONDER_ID), eq(event), eq(QUESTION_UPSERTED), any(QuestionUpsertPayload.class));
        verify(organizationRecipientResolver, never()).resolveInboxRecipients(anyLong());
    }

    private void verifyNoPersonalQuestionProjectionExceptAuthor(ForumDomainEvent event) {
        verify(publisher, never()).toPersonalQuestions(eq(RESPONDER_ID), eq(event), any(), any());
        verify(publisher, never()).toPersonalQuestions(eq(ADMINISTRATOR_ID), eq(event), any(), any());
    }

    private QuestionThreadResponseDTO question(
        Long assignedReviewerId,
        QuestionStatus status,
        QuestionVisibility visibility) {
        return new QuestionThreadResponseDTO(
            QUESTION_ID,
            TASK_ASSIGNMENT_ID,
            AUTHOR_ID,
            assignedReviewerId,
            "Clarification about memory limit",
            "Does the memory limit include input buffers?",
            status,
            visibility,
            OPEN,
            1L,
            OCCURRED_AT,
            OCCURRED_AT);
    }

    private QuestionMessageResponseDTO comment() {
        return new QuestionMessageResponseDTO(
            30L,
            QUESTION_ID,
            AUTHOR_ID,
            COMMENT,
            "Could you also clarify the time limit?",
            OCCURRED_AT);
    }
}
