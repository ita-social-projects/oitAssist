package com.itasocialacademy.oitassist.chat.realtime.handlers;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.NEW;
import static com.itasocialacademy.oitassist.chat.realtime.event.RealtimeEventType.INBOX_REMOVED;
import static com.itasocialacademy.oitassist.chat.realtime.event.RealtimeEventType.INBOX_UPSERTED;
import static com.itasocialacademy.oitassist.chat.realtime.event.RealtimeEventType.MESSAGE_CREATED;
import static com.itasocialacademy.oitassist.chat.realtime.event.RealtimeEventType.REVIEW_UPDATED;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionReviewInboxItemResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.event.CommentCreatedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.ForumDomainEvent;
import com.itasocialacademy.oitassist.chat.event.OfficialAnswerPublishedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionClaimedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionCreatedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionStateChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionStatusChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionVisibilityChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.realtime.event.InboxRemovalPayload;
import com.itasocialacademy.oitassist.chat.realtime.event.InboxUpsertPayload;
import com.itasocialacademy.oitassist.chat.realtime.event.MessageCreatedPayload;
import com.itasocialacademy.oitassist.chat.realtime.event.RealtimeEventType;
import com.itasocialacademy.oitassist.chat.realtime.event.RealtimeForumEvent;
import com.itasocialacademy.oitassist.chat.realtime.event.RealtimePayload;
import com.itasocialacademy.oitassist.chat.realtime.event.ReviewUpdatePayload;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrganizationRealtimeProjectionHandlerImpl
    implements OrganizationRealtimeProjectionHandler {
    /*
     * Clients subscribe to /user/queue/reviews.
     *
     * convertAndSendToUser receives the destination without the configured /user
     * prefix.
     */
    private static final String PERSONAL_REVIEWS_QUEUE =
        "/queue/reviews";

    private final SimpMessageSendingOperations messagingOperations;

    private final OrganizationRealtimeRecipientResolver recipientResolver;

    @Override
    public void handle(
        ForumDomainEvent event) {
        switch (event) {
            case QuestionCreatedDomainEvent questionCreated ->
                synchronizeInbox(questionCreated);

            case CommentCreatedDomainEvent commentCreated ->
                projectCommentCreated(commentCreated);

            case QuestionClaimedDomainEvent questionClaimed ->
                projectQuestionClaimed(questionClaimed);

            case OfficialAnswerPublishedDomainEvent answerPublished ->
                projectOfficialAnswerPublished(answerPublished);

            case QuestionVisibilityChangedDomainEvent visibilityChanged ->
                projectVisibilityChanged(visibilityChanged);

            case QuestionStatusChangedDomainEvent statusChanged ->
                projectMembershipAndAssignedReview(statusChanged);

            case QuestionStateChangedDomainEvent stateChanged ->
                projectMembershipAndAssignedReview(stateChanged);
        }
    }

    private void projectCommentCreated(
        CommentCreatedDomainEvent event) {
        /*
         * Comments do not change inbox membership.
         *
         * Message content is delivered only when the assigned reviewer still has
         * responder eligibility for the exact TaskAssignment.
         */
        sendMessageToAssignedResponder(
            event,
            event.message());
    }

    private void projectQuestionClaimed(
        QuestionClaimedDomainEvent event) {
        /*
         * Claiming removes the question from every current responder's inbox, including
         * the successful claimant.
         */
        synchronizeInbox(event);

        /*
         * Only an ORG responder claimant receives REVIEW_UPDATED.
         *
         * An administrator claim has no matching responder assignment and therefore
         * produces no ORG assigned-review event.
         */
        sendReviewUpdateToAssignedResponder(
            event);
    }

    private void projectOfficialAnswerPublished(
        OfficialAnswerPublishedDomainEvent event) {
        /*
         * The final ANSWERED status removes the question from every responder inbox.
         */
        synchronizeInbox(event);

        Long responderId =
            resolveAssignedResponder(
                event);

        if (responderId == null) {
            return;
        }

        sendMessageToResponder(
            event,
            responderId,
            event.message());

        sendReviewUpdateToResponder(
            event,
            responderId);
    }

    private void projectVisibilityChanged(
        QuestionVisibilityChangedDomainEvent event) {
        /*
         * Visibility does not affect ORG inbox eligibility. An eligible item receives a
         * new summary to synchronize visibility/version.
         */
        if (isInboxEligible(
            event.question())) {
            sendInboxUpsert(
                event);

            return;
        }

        sendReviewUpdateToAssignedResponder(
            event);
    }

    private void projectMembershipAndAssignedReview(
        ForumDomainEvent event) {
        synchronizeInbox(event);

        sendReviewUpdateToAssignedResponder(
            event);
    }

    private void synchronizeInbox(
        ForumDomainEvent event) {
        if (isInboxEligible(
            event.question())) {
            sendInboxUpsert(event);
        } else {
            sendInboxRemoval(event);
        }
    }

    private boolean isInboxEligible(
        QuestionThreadResponseDTO question) {
        return question.state() == OPEN
            && question.status() == NEW
            && question.assignedReviewerId() == null;
    }

    private void sendInboxUpsert(
        ForumDomainEvent event) {
        QuestionReviewInboxItemResponseDTO summary =
            toReviewSummary(
                event.question());

        List<Long> responderIds =
            recipientResolver.resolveInboxRecipients(
                event.taskAssignmentId());

        responderIds.forEach(responderId -> messagingOperations.convertAndSendToUser(
            principalName(responderId),
            PERSONAL_REVIEWS_QUEUE,
            createRealtimeEvent(
                event,
                INBOX_UPSERTED,
                new InboxUpsertPayload(
                    summary))));
    }

    private void sendInboxRemoval(
        ForumDomainEvent event) {
        List<Long> responderIds =
            recipientResolver.resolveInboxRecipients(
                event.taskAssignmentId());

        responderIds.forEach(responderId -> messagingOperations.convertAndSendToUser(
            principalName(responderId),
            PERSONAL_REVIEWS_QUEUE,
            createRealtimeEvent(
                event,
                INBOX_REMOVED,
                new InboxRemovalPayload(
                    event.taskAssignmentId(),
                    event.questionId()))));
    }

    private void sendReviewUpdateToAssignedResponder(
        ForumDomainEvent event) {
        Long responderId =
            resolveAssignedResponder(
                event);

        if (responderId == null) {
            return;
        }

        sendReviewUpdateToResponder(
            event,
            responderId);
    }

    private void sendReviewUpdateToResponder(
        ForumDomainEvent event,
        Long responderId) {
        messagingOperations.convertAndSendToUser(
            principalName(responderId),
            PERSONAL_REVIEWS_QUEUE,
            createRealtimeEvent(
                event,
                REVIEW_UPDATED,
                new ReviewUpdatePayload(
                    toReviewSummary(
                        event.question()))));
    }

    private void sendMessageToAssignedResponder(
        ForumDomainEvent event,
        QuestionMessageResponseDTO message) {
        Long responderId =
            resolveAssignedResponder(
                event);

        if (responderId == null) {
            return;
        }

        sendMessageToResponder(
            event,
            responderId,
            message);
    }

    private void sendMessageToResponder(
        ForumDomainEvent event,
        Long responderId,
        QuestionMessageResponseDTO message) {
        messagingOperations.convertAndSendToUser(
            principalName(responderId),
            PERSONAL_REVIEWS_QUEUE,
            createRealtimeEvent(
                event,
                MESSAGE_CREATED,
                new MessageCreatedPayload(
                    message)));
    }

    private Long resolveAssignedResponder(
        ForumDomainEvent event) {
        Long reviewerId =
            event.question()
                .assignedReviewerId();

        if (!recipientResolver
            .isOrganizationResponder(
                event.taskAssignmentId(),
                reviewerId)) {
            return null;
        }

        return reviewerId;
    }

    private QuestionReviewInboxItemResponseDTO toReviewSummary(
        QuestionThreadResponseDTO question) {
        /*
         * Full question content is intentionally not copied into inbox or
         * assigned-review summary projections.
         */
        return new QuestionReviewInboxItemResponseDTO(
            question.id(),
            question.taskAssignmentId(),
            question.authorId(),
            question.assignedReviewerId(),
            question.title(),
            question.status(),
            question.state(),
            question.visibility(),
            question.version(),
            question.createdAt(),
            question.updatedAt());
    }

    private RealtimeForumEvent createRealtimeEvent(
        ForumDomainEvent domainEvent,
        RealtimeEventType type,
        RealtimePayload payload) {
        return new RealtimeForumEvent(
            UUID.randomUUID(),
            type,
            domainEvent.occurredAt(),
            domainEvent.taskAssignmentId(),
            domainEvent.questionId(),
            payload);
    }

    private String principalName(
        Long responderId) {
        if (responderId == null
            || responderId <= 0) {
            throw new IllegalArgumentException(
                "Responder id must be positive");
        }

        return responderId.toString();
    }
}