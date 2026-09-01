package com.itasocialacademy.oitassist.chat.realtime;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PUBLIC;
import static com.itasocialacademy.oitassist.chat.event.realtime.RealtimeEventType.ACCESS_REVOKED;
import static com.itasocialacademy.oitassist.chat.event.realtime.RealtimeEventType.MESSAGE_CREATED;
import static com.itasocialacademy.oitassist.chat.event.realtime.RealtimeEventType.QUESTION_REMOVED;
import static com.itasocialacademy.oitassist.chat.event.realtime.RealtimeEventType.QUESTION_UPSERTED;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.event.domain.CommentCreatedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.domain.ForumDomainEvent;
import com.itasocialacademy.oitassist.chat.event.domain.OfficialAnswerPublishedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.domain.QuestionClaimedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.domain.QuestionCreatedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.domain.QuestionStateChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.domain.QuestionStatusChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.domain.QuestionVisibilityChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.realtime.AccessRevokedPayload;
import com.itasocialacademy.oitassist.chat.event.realtime.MessageCreatedPayload;
import com.itasocialacademy.oitassist.chat.event.realtime.QuestionRemovalPayload;
import com.itasocialacademy.oitassist.chat.event.realtime.QuestionUpsertPayload;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParticipantRealtimeHandler {
    private final ForumRealtimePublisher publisher;
    private final OrganizationRealtimeRecipientResolver organizationRecipientResolver;

    public void handle(ForumDomainEvent event) {
        switch (event) {
            case QuestionCreatedDomainEvent questionCreated ->
                projectQuestionSnapshot(questionCreated, true);

            case CommentCreatedDomainEvent commentCreated ->
                projectCommentCreated(commentCreated);

            case QuestionClaimedDomainEvent questionClaimed ->
                projectQuestionSnapshot(questionClaimed, true);

            case OfficialAnswerPublishedDomainEvent answerPublished ->
                projectOfficialAnswerPublished(answerPublished);

            case QuestionVisibilityChangedDomainEvent visibilityChanged ->
                projectVisibilityChanged(visibilityChanged);

            case QuestionStatusChangedDomainEvent statusChanged ->
                projectQuestionSnapshot(statusChanged, false);

            case QuestionStateChangedDomainEvent stateChanged ->
                projectQuestionSnapshot(stateChanged, false);
        }
    }

    private void projectCommentCreated(CommentCreatedDomainEvent event) {
        if (event.question().visibility() == PUBLIC) {
            publisher.toQuestionThread(
                event,
                MESSAGE_CREATED,
                new MessageCreatedPayload(event.message()));
        }

        sendMessageToAuthor(event, event.message());
        sendMessageToPrivilegedReaders(event, event.message());
    }

    private void projectOfficialAnswerPublished(
        OfficialAnswerPublishedDomainEvent event) {
        if (event.question().visibility() == PUBLIC) {
            publisher.toQuestionThread(
                event,
                MESSAGE_CREATED,
                new MessageCreatedPayload(event.message()));

            sendQuestionUpsertToThread(event);
            sendQuestionUpsertToForum(event);
        }

        sendMessageToAuthor(event, event.message());
        sendMessageToPrivilegedReaders(event, event.message());
        sendQuestionUpsertToAuthor(event);
        sendQuestionUpsertToPrivilegedReaders(event);
    }

    private void projectVisibilityChanged(
        QuestionVisibilityChangedDomainEvent event) {
        if (event.previousVisibility() == PRIVATE
            && event.currentVisibility() == PUBLIC) {
            sendQuestionUpsertToForum(event);
            sendQuestionUpsertToThread(event);
            sendQuestionUpsertToAuthor(event);
            return;
        }

        if (event.previousVisibility() == PUBLIC
            && event.currentVisibility() == PRIVATE) {
            publisher.toTaskAssignmentForum(
                event,
                QUESTION_REMOVED,
                new QuestionRemovalPayload(
                    event.taskAssignmentId(),
                    event.questionId()));

            publisher.toQuestionThread(
                event,
                ACCESS_REVOKED,
                new AccessRevokedPayload(
                    event.taskAssignmentId(),
                    event.questionId()));

            sendQuestionUpsertToAuthor(event);
            sendQuestionUpsertToPrivilegedReaders(event);
            return;
        }

        projectQuestionSnapshot(event, true);
    }

    private void projectQuestionSnapshot(
        ForumDomainEvent event,
        boolean notifyAuthorWhenPublic) {
        if (event.question().visibility() == PUBLIC) {
            sendQuestionUpsertToThread(event);
            sendQuestionUpsertToForum(event);

            if (notifyAuthorWhenPublic) {
                sendQuestionUpsertToAuthor(event);
            }

            return;
        }

        sendQuestionUpsertToAuthor(event);
        sendQuestionUpsertToPrivilegedReaders(event);
    }

    private void sendQuestionUpsertToForum(ForumDomainEvent event) {
        publisher.toTaskAssignmentForum(
            event,
            QUESTION_UPSERTED,
            new QuestionUpsertPayload(event.question()));
    }

    private void sendQuestionUpsertToThread(ForumDomainEvent event) {
        publisher.toQuestionThread(
            event,
            QUESTION_UPSERTED,
            new QuestionUpsertPayload(event.question()));
    }

    private void sendQuestionUpsertToAuthor(ForumDomainEvent event) {
        publisher.toPersonalQuestions(
            event.question().authorId(),
            event,
            QUESTION_UPSERTED,
            new QuestionUpsertPayload(event.question()));
    }

    private void sendMessageToAuthor(
        ForumDomainEvent event,
        QuestionMessageResponseDTO message) {
        publisher.toPersonalQuestions(
            event.question().authorId(),
            event,
            MESSAGE_CREATED,
            new MessageCreatedPayload(message));
    }

    private void sendQuestionUpsertToPrivilegedReaders(
        ForumDomainEvent event) {
        publisher.toAdministratorAllQuestions(
            event,
            QUESTION_UPSERTED,
            new QuestionUpsertPayload(event.question()));

        organizationRecipientResolver
            .resolveInboxRecipients(event.taskAssignmentId())
            .stream()
            .filter(responderId -> !Objects.equals(
                responderId,
                event.question().authorId()))
            .forEach(responderId -> publisher.toPersonalQuestions(
                responderId,
                event,
                QUESTION_UPSERTED,
                new QuestionUpsertPayload(event.question())));
    }

    private void sendMessageToPrivilegedReaders(
        ForumDomainEvent event,
        QuestionMessageResponseDTO message) {
        publisher.toAdministratorAllQuestions(
            event,
            MESSAGE_CREATED,
            new MessageCreatedPayload(message));

        organizationRecipientResolver
            .resolveInboxRecipients(event.taskAssignmentId())
            .forEach(responderId -> publisher.toPersonalQuestions(
                responderId,
                event,
                MESSAGE_CREATED,
                new MessageCreatedPayload(message)));
    }
}