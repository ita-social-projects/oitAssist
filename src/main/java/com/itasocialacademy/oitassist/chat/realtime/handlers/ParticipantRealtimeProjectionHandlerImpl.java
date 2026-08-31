package com.itasocialacademy.oitassist.chat.realtime.handlers;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PUBLIC;
import static com.itasocialacademy.oitassist.chat.realtime.event.RealtimeEventType.ACCESS_REVOKED;
import static com.itasocialacademy.oitassist.chat.realtime.event.RealtimeEventType.MESSAGE_CREATED;
import static com.itasocialacademy.oitassist.chat.realtime.event.RealtimeEventType.QUESTION_REMOVED;
import static com.itasocialacademy.oitassist.chat.realtime.event.RealtimeEventType.QUESTION_UPSERTED;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.event.CommentCreatedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.ForumDomainEvent;
import com.itasocialacademy.oitassist.chat.event.OfficialAnswerPublishedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionClaimedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionCreatedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionStateChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionStatusChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionVisibilityChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.realtime.event.AccessRevokedPayload;
import com.itasocialacademy.oitassist.chat.realtime.event.MessageCreatedPayload;
import com.itasocialacademy.oitassist.chat.realtime.event.QuestionRemovalPayload;
import com.itasocialacademy.oitassist.chat.realtime.event.QuestionUpsertPayload;
import com.itasocialacademy.oitassist.chat.realtime.event.RealtimeEventType;
import com.itasocialacademy.oitassist.chat.realtime.event.RealtimeForumEvent;
import com.itasocialacademy.oitassist.chat.realtime.event.RealtimePayload;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParticipantRealtimeProjectionHandlerImpl
    implements ParticipantRealtimeProjectionHandler {
    /*
     * Clients subscribe to /user/queue/questions.
     *
     * convertAndSendToUser receives only the destination suffix because Spring
     * resolves the configured /user prefix itself.
     */
    private static final String PERSONAL_QUESTIONS_QUEUE =
        "/queue/questions";

    private static final String TASK_ASSIGNMENT_FORUM_DESTINATION =
        "/topic/task-assignments/%d/questions";

    private static final String QUESTION_THREAD_DESTINATION =
        "/topic/questions/%d";

    private static final String ADMINISTRATOR_ALL_QUESTIONS_DESTINATION =
        "/topic/admin/questions/all";

    private final SimpMessageSendingOperations messagingOperations;

    private final OrganizationRealtimeRecipientResolver organizationRecipientResolver;

    @Override
    public void handle(
        ForumDomainEvent event) {
        switch (event) {
            case QuestionCreatedDomainEvent questionCreated ->
                projectQuestionCreated(
                    questionCreated);

            case CommentCreatedDomainEvent commentCreated ->
                projectCommentCreated(
                    commentCreated);

            case QuestionClaimedDomainEvent questionClaimed ->
                projectQuestionClaimed(
                    questionClaimed);

            case OfficialAnswerPublishedDomainEvent answerPublished ->
                projectOfficialAnswerPublished(
                    answerPublished);

            case QuestionVisibilityChangedDomainEvent visibilityChanged ->
                projectVisibilityChanged(
                    visibilityChanged);

            case QuestionStatusChangedDomainEvent statusChanged ->
                projectQuestionSnapshot(
                    statusChanged,
                    false);

            case QuestionStateChangedDomainEvent stateChanged ->
                projectQuestionSnapshot(
                    stateChanged,
                    false);
        }
    }

    private void projectQuestionCreated(
        QuestionCreatedDomainEvent event) {
        /*
         * Questions are currently created as PRIVATE, which produces only the personal
         * author projection.
         *
         * The PUBLIC branch keeps this handler safe if public creation is introduced
         * later.
         */
        projectQuestionSnapshot(
            event,
            true);
    }

    private void projectCommentCreated(
        CommentCreatedDomainEvent event) {
        if (event.question().visibility() == PUBLIC) {
            sendMessageToThread(
                event,
                event.message());
        }

        /*
         * The author receives the event even if they created the comment themselves.
         * Routing does not depend on the initiating browser tab.
         */
        sendMessageToAuthor(
            event,
            event.message());

        sendMessageToPrivilegedReaders(event, event.message());
    }

    private void projectOfficialAnswerPublished(
        OfficialAnswerPublishedDomainEvent event) {
        if (event.question().visibility() == PUBLIC) {
            /*
             * The thread needs both projections:
             *
             * - MESSAGE_CREATED to append the answer; - QUESTION_UPSERTED to update status,
             * version and other metadata.
             */
            sendMessageToThread(
                event,
                event.message());

            sendQuestionUpsertToThread(
                event);

            sendQuestionUpsertToForum(
                event);
        }

        /*
         * Private questions must receive both the answer and the updated question
         * snapshot only through the author's personal destination.
         *
         * Public-question authors receive the same personal notification.
         */
        sendMessageToAuthor(
            event,
            event.message());

        sendMessageToPrivilegedReaders(
            event,
            event.message());

        sendQuestionUpsertToAuthor(
            event);

        sendQuestionUpsertToPrivilegedReaders(event);
    }

    private void projectVisibilityChanged(
        QuestionVisibilityChangedDomainEvent event) {
        if (event.previousVisibility() == PRIVATE
            && event.currentVisibility() == PUBLIC) {
            sendQuestionUpsertToForum(
                event);

            sendQuestionUpsertToThread(
                event);

            sendQuestionUpsertToAuthor(
                event);

            return;
        }

        if (event.previousVisibility() == PUBLIC
            && event.currentVisibility() == PRIVATE) {
            sendQuestionRemovalToForum(
                event);

            sendAccessRevokedToThread(
                event);

            sendQuestionUpsertToAuthor(
                event);

            sendQuestionUpsertToPrivilegedReaders(
                event);

            return;
        }

        /*
         * Existing moderation currently permits assigning the same visibility value
         * again. Such an update may still increment the question version, so the
         * current projection must remain synchronized.
         */
        projectQuestionSnapshot(
            event,
            true);
    }

    private void projectQuestionClaimed(
        QuestionClaimedDomainEvent event) {
        projectQuestionSnapshot(
            event,
            true);
    }

    /**
     * Projects the current question snapshot according to its visibility.
     *
     * <p>
     * Public status and state changes update the shared thread and forum
     * projections. Private changes are sent only to the author.
     * </p>
     *
     * @param event                  internal committed event
     * @param notifyAuthorWhenPublic whether a public question also produces a
     *                               personal author projection
     */
    private void projectQuestionSnapshot(
        ForumDomainEvent event,
        boolean notifyAuthorWhenPublic) {
        if (event.question().visibility() == PUBLIC) {
            sendQuestionUpsertToThread(
                event);

            sendQuestionUpsertToForum(
                event);

            if (notifyAuthorWhenPublic) {
                sendQuestionUpsertToAuthor(
                    event);
            }

            return;
        }

        sendQuestionUpsertToAuthor(
            event);

        sendQuestionUpsertToPrivilegedReaders(
            event);
    }

    private void sendQuestionUpsertToForum(
        ForumDomainEvent event) {
        messagingOperations.convertAndSend(
            taskAssignmentForumDestination(
                event.taskAssignmentId()),
            createRealtimeEvent(
                event,
                QUESTION_UPSERTED,
                new QuestionUpsertPayload(
                    event.question())));
    }

    private void sendQuestionUpsertToThread(
        ForumDomainEvent event) {
        messagingOperations.convertAndSend(
            questionThreadDestination(
                event.questionId()),
            createRealtimeEvent(
                event,
                QUESTION_UPSERTED,
                new QuestionUpsertPayload(
                    event.question())));
    }

    private void sendQuestionUpsertToAuthor(
        ForumDomainEvent event) {
        messagingOperations.convertAndSendToUser(
            authorPrincipalName(event),
            PERSONAL_QUESTIONS_QUEUE,
            createRealtimeEvent(
                event,
                QUESTION_UPSERTED,
                new QuestionUpsertPayload(
                    event.question())));
    }

    private void sendMessageToThread(
        ForumDomainEvent event,
        QuestionMessageResponseDTO message) {
        messagingOperations.convertAndSend(
            questionThreadDestination(
                event.questionId()),
            createRealtimeEvent(
                event,
                MESSAGE_CREATED,
                new MessageCreatedPayload(
                    message)));
    }

    private void sendMessageToAuthor(
        ForumDomainEvent event,
        QuestionMessageResponseDTO message) {
        messagingOperations.convertAndSendToUser(
            authorPrincipalName(event),
            PERSONAL_QUESTIONS_QUEUE,
            createRealtimeEvent(
                event,
                MESSAGE_CREATED,
                new MessageCreatedPayload(
                    message)));
    }

    private void sendQuestionUpsertToPrivilegedReaders(
        ForumDomainEvent event) {
        messagingOperations.convertAndSend(
            ADMINISTRATOR_ALL_QUESTIONS_DESTINATION,
            createRealtimeEvent(
                event,
                QUESTION_UPSERTED,
                new QuestionUpsertPayload(
                    event.question())));

        organizationRecipientResolver
            .resolveInboxRecipients(
                event.taskAssignmentId())
            .stream()
            .filter(responderId -> !Objects.equals(
                responderId,
                event.question().authorId()))
            .forEach(responderId -> messagingOperations.convertAndSendToUser(
                responderId.toString(),
                PERSONAL_QUESTIONS_QUEUE,
                createRealtimeEvent(
                    event,
                    QUESTION_UPSERTED,
                    new QuestionUpsertPayload(
                        event.question()))));
    }

    private void sendMessageToPrivilegedReaders(
        ForumDomainEvent event,
        QuestionMessageResponseDTO message) {
        messagingOperations.convertAndSend(
            ADMINISTRATOR_ALL_QUESTIONS_DESTINATION,
            createRealtimeEvent(
                event,
                MESSAGE_CREATED,
                new MessageCreatedPayload(message)));

        organizationRecipientResolver
            .resolveInboxRecipients(
                event.taskAssignmentId())
            .forEach(responderId -> messagingOperations.convertAndSendToUser(
                responderId.toString(),
                PERSONAL_QUESTIONS_QUEUE,
                createRealtimeEvent(
                    event,
                    MESSAGE_CREATED,
                    new MessageCreatedPayload(message))));
    }

    private void sendQuestionRemovalToForum(
        ForumDomainEvent event) {
        messagingOperations.convertAndSend(
            taskAssignmentForumDestination(
                event.taskAssignmentId()),
            createRealtimeEvent(
                event,
                QUESTION_REMOVED,
                new QuestionRemovalPayload(
                    event.taskAssignmentId(),
                    event.questionId())));
    }

    private void sendAccessRevokedToThread(
        ForumDomainEvent event) {
        messagingOperations.convertAndSend(
            questionThreadDestination(
                event.questionId()),
            createRealtimeEvent(
                event,
                ACCESS_REVOKED,
                new AccessRevokedPayload(
                    event.taskAssignmentId(),
                    event.questionId())));
    }

    private RealtimeForumEvent createRealtimeEvent(
        ForumDomainEvent domainEvent,
        RealtimeEventType type,
        RealtimePayload payload) {
        /*
         * Each destination receives its own delivery event ID. This prevents a client
         * subscribed to several destinations from incorrectly discarding a distinct
         * projection as a duplicate.
         *
         * occurredAt is inherited from the committed domain event rather than generated
         * at WebSocket-delivery time.
         */
        return new RealtimeForumEvent(
            UUID.randomUUID(),
            type,
            domainEvent.occurredAt(),
            domainEvent.taskAssignmentId(),
            domainEvent.questionId(),
            payload);
    }

    private String authorPrincipalName(
        ForumDomainEvent event) {
        Long authorId =
            event.question().authorId();

        if (authorId == null || authorId <= 0) {
            throw new IllegalArgumentException(
                "Question author id must be positive");
        }

        return authorId.toString();
    }

    private String taskAssignmentForumDestination(
        Long taskAssignmentId) {
        return TASK_ASSIGNMENT_FORUM_DESTINATION
            .formatted(taskAssignmentId);
    }

    private String questionThreadDestination(
        Long questionId) {
        return QUESTION_THREAD_DESTINATION
            .formatted(questionId);
    }
}