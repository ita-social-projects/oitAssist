package com.itasocialacademy.oitassist.chat.realtime;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.NEW;
import static com.itasocialacademy.oitassist.chat.utils.event.RealtimeEventType.INBOX_REMOVED;
import static com.itasocialacademy.oitassist.chat.utils.event.RealtimeEventType.INBOX_UPSERTED;
import static com.itasocialacademy.oitassist.chat.utils.event.RealtimeEventType.MESSAGE_CREATED;
import static com.itasocialacademy.oitassist.chat.utils.event.RealtimeEventType.REVIEW_UPDATED;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionReviewInboxItemResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.event.CommentCreatedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.ForumDomainEvent;
import com.itasocialacademy.oitassist.chat.event.OfficialAnswerPublishedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionClaimedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionCreatedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionStateChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionStatusChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionVisibilityChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.utils.event.InboxRemovalPayload;
import com.itasocialacademy.oitassist.chat.utils.event.InboxUpsertPayload;
import com.itasocialacademy.oitassist.chat.utils.event.MessageCreatedPayload;
import com.itasocialacademy.oitassist.chat.utils.event.RealtimeEventType;
import com.itasocialacademy.oitassist.chat.utils.event.RealtimeForumEvent;
import com.itasocialacademy.oitassist.chat.utils.event.RealtimePayload;
import com.itasocialacademy.oitassist.chat.utils.event.ReviewUpdatePayload;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdministratorRealtimeProjectionHandlerImpl
    implements AdministratorRealtimeProjectionHandler {
    private static final String ADMINISTRATOR_INBOX_DESTINATION =
        "/topic/admin/questions/inbox";

    /*
     * Clients subscribe to /user/queue/reviews.
     *
     * convertAndSendToUser receives the destination without the configured /user
     * prefix.
     */
    private static final String PERSONAL_REVIEWS_QUEUE =
        "/queue/reviews";

    private final SimpMessageSendingOperations messagingOperations;

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
         * A comment does not affect shared inbox eligibility.
         *
         * Message content is delivered only to the currently assigned administrator.
         * Unrelated administrators receive nothing through their personal queues.
         */
        sendMessageToAssignedReviewer(
            event,
            event.message());
    }

    private void projectQuestionClaimed(
        QuestionClaimedDomainEvent event) {
        /*
         * A claimed question is no longer eligible because it has an assigned reviewer
         * and its status is normally IN_REVIEW.
         */
        synchronizeInbox(event);

        sendReviewUpdateToReviewer(
            event,
            event.currentReviewerId());
    }

    private void projectOfficialAnswerPublished(
        OfficialAnswerPublishedDomainEvent event) {
        /*
         * Publishing an official answer changes the status to ANSWERED, so the question
         * must be removed from the unclaimed inbox.
         */
        synchronizeInbox(event);

        Long reviewerId =
            event.question().assignedReviewerId();

        if (reviewerId == null) {
            return;
        }

        /*
         * The assigned administrator receives both:
         *
         * - MESSAGE_CREATED to append the official answer; - REVIEW_UPDATED to
         * synchronize status, version and metadata.
         */
        sendMessageToReviewer(
            event,
            reviewerId,
            event.message());

        sendReviewUpdateToReviewer(
            event,
            reviewerId);
    }

    private void projectVisibilityChanged(
        QuestionVisibilityChangedDomainEvent event) {
        /*
         * Visibility does not affect inbox eligibility.
         *
         * Eligible items receive a fresh INBOX_UPSERTED projection so the visibility
         * field in the administrator list remains synchronized.
         */
        if (isInboxEligible(event.question())) {
            sendInboxUpsert(event);
            return;
        }

        /*
         * An assigned question is not part of the shared inbox, but its assigned
         * administrator still needs the current visibility and version.
         */
        sendReviewUpdateToAssignedReviewer(
            event);
    }

    private void projectMembershipAndAssignedReview(
        ForumDomainEvent event) {
        /*
         * Status and lifecycle state can add or remove a question from the shared
         * unclaimed inbox.
         */
        synchronizeInbox(event);

        /*
         * When the question is assigned, synchronize only that administrator's personal
         * review projection.
         */
        sendReviewUpdateToAssignedReviewer(
            event);
    }

    private void synchronizeInbox(
        ForumDomainEvent event) {
        if (isInboxEligible(event.question())) {
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
        messagingOperations.convertAndSend(
            ADMINISTRATOR_INBOX_DESTINATION,
            createRealtimeEvent(
                event,
                INBOX_UPSERTED,
                new InboxUpsertPayload(
                    toAdministratorSummary(
                        event.question()))));
    }

    private void sendInboxRemoval(
        ForumDomainEvent event) {
        /*
         * Shared removals intentionally contain identifiers only.
         */
        messagingOperations.convertAndSend(
            ADMINISTRATOR_INBOX_DESTINATION,
            createRealtimeEvent(
                event,
                INBOX_REMOVED,
                new InboxRemovalPayload(
                    event.taskAssignmentId(),
                    event.questionId())));
    }

    private void sendReviewUpdateToAssignedReviewer(
        ForumDomainEvent event) {
        Long reviewerId =
            event.question().assignedReviewerId();

        if (reviewerId == null) {
            return;
        }

        sendReviewUpdateToReviewer(
            event,
            reviewerId);
    }

    private void sendReviewUpdateToReviewer(
        ForumDomainEvent event,
        Long reviewerId) {
        messagingOperations.convertAndSendToUser(
            reviewerPrincipalName(reviewerId),
            PERSONAL_REVIEWS_QUEUE,
            createRealtimeEvent(
                event,
                REVIEW_UPDATED,
                new ReviewUpdatePayload(
                    toAdministratorSummary(
                        event.question()))));
    }

    private void sendMessageToAssignedReviewer(
        ForumDomainEvent event,
        QuestionMessageResponseDTO message) {
        Long reviewerId =
            event.question().assignedReviewerId();

        if (reviewerId == null) {
            return;
        }

        sendMessageToReviewer(
            event,
            reviewerId,
            message);
    }

    private void sendMessageToReviewer(
        ForumDomainEvent event,
        Long reviewerId,
        QuestionMessageResponseDTO message) {
        messagingOperations.convertAndSendToUser(
            reviewerPrincipalName(reviewerId),
            PERSONAL_REVIEWS_QUEUE,
            createRealtimeEvent(
                event,
                MESSAGE_CREATED,
                new MessageCreatedPayload(
                    message)));
    }

    private QuestionReviewInboxItemResponseDTO toAdministratorSummary(
        QuestionThreadResponseDTO question) {
        /*
         * The shared inbox and personal review queues reuse the existing administrator
         * summary contract.
         *
         * Full question content is deliberately not copied.
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
        /*
         * Each destination projection gets its own delivery ID. occurredAt remains the
         * time of the committed domain event.
         */
        return new RealtimeForumEvent(
            UUID.randomUUID(),
            type,
            domainEvent.occurredAt(),
            domainEvent.taskAssignmentId(),
            domainEvent.questionId(),
            payload);
    }

    private String reviewerPrincipalName(
        Long reviewerId) {
        if (reviewerId == null || reviewerId <= 0) {
            throw new IllegalArgumentException(
                "Assigned reviewer id must be positive");
        }

        return reviewerId.toString();
    }
}