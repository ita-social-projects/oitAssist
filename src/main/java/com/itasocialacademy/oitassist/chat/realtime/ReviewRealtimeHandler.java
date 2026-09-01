package com.itasocialacademy.oitassist.chat.realtime;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.NEW;
import static com.itasocialacademy.oitassist.chat.event.realtime.RealtimeEventType.INBOX_REMOVED;
import static com.itasocialacademy.oitassist.chat.event.realtime.RealtimeEventType.INBOX_UPSERTED;
import static com.itasocialacademy.oitassist.chat.event.realtime.RealtimeEventType.MESSAGE_CREATED;
import static com.itasocialacademy.oitassist.chat.event.realtime.RealtimeEventType.REVIEW_UPDATED;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionMessageResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionReviewInboxItemResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.event.domain.CommentCreatedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.domain.ForumDomainEvent;
import com.itasocialacademy.oitassist.chat.event.domain.OfficialAnswerPublishedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.domain.QuestionClaimedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.domain.QuestionCreatedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.domain.QuestionStateChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.domain.QuestionStatusChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.domain.QuestionVisibilityChangedDomainEvent;
import com.itasocialacademy.oitassist.chat.event.realtime.InboxRemovalPayload;
import com.itasocialacademy.oitassist.chat.event.realtime.InboxUpsertPayload;
import com.itasocialacademy.oitassist.chat.event.realtime.MessageCreatedPayload;
import com.itasocialacademy.oitassist.chat.event.realtime.ReviewUpdatePayload;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewRealtimeHandler {
    private final ForumRealtimePublisher publisher;
    private final OrganizationRealtimeRecipientResolver organizationRecipientResolver;

    public void projectAdministrator(ForumDomainEvent event) {
        switch (event) {
            case QuestionCreatedDomainEvent questionCreated ->
                synchronizeAdministratorInbox(questionCreated);

            case CommentCreatedDomainEvent commentCreated ->
                sendAdministratorMessageToAssignedReviewer(
                    commentCreated,
                    commentCreated.message());

            case QuestionClaimedDomainEvent questionClaimed -> {
                synchronizeAdministratorInbox(questionClaimed);
                sendAdministratorReviewUpdateToReviewer(
                    questionClaimed,
                    questionClaimed.currentReviewerId());
            }

            case OfficialAnswerPublishedDomainEvent answerPublished ->
                projectAdministratorOfficialAnswerPublished(answerPublished);

            case QuestionVisibilityChangedDomainEvent visibilityChanged ->
                projectAdministratorVisibilityChanged(visibilityChanged);

            case QuestionStatusChangedDomainEvent statusChanged ->
                projectAdministratorMembershipAndAssignedReview(statusChanged);

            case QuestionStateChangedDomainEvent stateChanged ->
                projectAdministratorMembershipAndAssignedReview(stateChanged);
        }
    }

    public void projectOrganization(ForumDomainEvent event) {
        switch (event) {
            case QuestionCreatedDomainEvent questionCreated ->
                synchronizeOrganizationInbox(questionCreated);

            case CommentCreatedDomainEvent commentCreated ->
                sendOrganizationMessageToAssignedResponder(
                    commentCreated,
                    commentCreated.message());

            case QuestionClaimedDomainEvent questionClaimed -> {
                synchronizeOrganizationInbox(questionClaimed);
                sendOrganizationReviewUpdateToAssignedResponder(questionClaimed);
            }

            case OfficialAnswerPublishedDomainEvent answerPublished ->
                projectOrganizationOfficialAnswerPublished(answerPublished);

            case QuestionVisibilityChangedDomainEvent visibilityChanged ->
                projectOrganizationVisibilityChanged(visibilityChanged);

            case QuestionStatusChangedDomainEvent statusChanged ->
                projectOrganizationMembershipAndAssignedReview(statusChanged);

            case QuestionStateChangedDomainEvent stateChanged ->
                projectOrganizationMembershipAndAssignedReview(stateChanged);
        }
    }

    private void projectAdministratorOfficialAnswerPublished(
        OfficialAnswerPublishedDomainEvent event) {
        synchronizeAdministratorInbox(event);

        Long reviewerId = event.question().assignedReviewerId();

        if (reviewerId == null) {
            return;
        }

        sendAdministratorMessageToReviewer(
            event,
            reviewerId,
            event.message());
        sendAdministratorReviewUpdateToReviewer(event, reviewerId);
    }

    private void projectAdministratorVisibilityChanged(
        QuestionVisibilityChangedDomainEvent event) {
        if (isInboxEligible(event.question())) {
            sendAdministratorInboxUpsert(event);
            return;
        }

        sendAdministratorReviewUpdateToAssignedReviewer(event);
    }

    private void projectAdministratorMembershipAndAssignedReview(
        ForumDomainEvent event) {
        synchronizeAdministratorInbox(event);
        sendAdministratorReviewUpdateToAssignedReviewer(event);
    }

    private void synchronizeAdministratorInbox(ForumDomainEvent event) {
        if (isInboxEligible(event.question())) {
            sendAdministratorInboxUpsert(event);
        } else {
            publisher.toAdministratorInbox(
                event,
                INBOX_REMOVED,
                new InboxRemovalPayload(
                    event.taskAssignmentId(),
                    event.questionId()));
        }
    }

    private void sendAdministratorInboxUpsert(ForumDomainEvent event) {
        publisher.toAdministratorInbox(
            event,
            INBOX_UPSERTED,
            new InboxUpsertPayload(
                toReviewSummary(event.question())));
    }

    private void sendAdministratorReviewUpdateToAssignedReviewer(
        ForumDomainEvent event) {
        Long reviewerId = event.question().assignedReviewerId();

        if (reviewerId == null) {
            return;
        }

        sendAdministratorReviewUpdateToReviewer(event, reviewerId);
    }

    private void sendAdministratorReviewUpdateToReviewer(
        ForumDomainEvent event,
        Long reviewerId) {
        if (isOrganizationResponder(event, reviewerId)) {
            return;
        }

        publisher.toPersonalReviews(
            reviewerId,
            event,
            REVIEW_UPDATED,
            new ReviewUpdatePayload(
                toReviewSummary(event.question())));
    }

    private void sendAdministratorMessageToAssignedReviewer(
        ForumDomainEvent event,
        QuestionMessageResponseDTO message) {
        Long reviewerId = event.question().assignedReviewerId();

        if (reviewerId == null) {
            return;
        }

        sendAdministratorMessageToReviewer(
            event,
            reviewerId,
            message);
    }

    private void sendAdministratorMessageToReviewer(
        ForumDomainEvent event,
        Long reviewerId,
        QuestionMessageResponseDTO message) {
        if (isOrganizationResponder(event, reviewerId)) {
            return;
        }

        publisher.toPersonalReviews(
            reviewerId,
            event,
            MESSAGE_CREATED,
            new MessageCreatedPayload(message));
    }

    private boolean isOrganizationResponder(
        ForumDomainEvent event,
        Long reviewerId) {
        return reviewerId != null
            && organizationRecipientResolver.isOrganizationResponder(
                event.taskAssignmentId(),
                reviewerId);
    }

    private void projectOrganizationOfficialAnswerPublished(
        OfficialAnswerPublishedDomainEvent event) {
        synchronizeOrganizationInbox(event);

        Long responderId = resolveAssignedOrganizationResponder(event);

        if (responderId == null) {
            return;
        }

        sendOrganizationMessageToResponder(
            event,
            responderId,
            event.message());
        sendOrganizationReviewUpdateToResponder(event, responderId);
    }

    private void projectOrganizationVisibilityChanged(
        QuestionVisibilityChangedDomainEvent event) {
        if (isInboxEligible(event.question())) {
            sendOrganizationInboxUpsert(event);
            return;
        }

        sendOrganizationReviewUpdateToAssignedResponder(event);
    }

    private void projectOrganizationMembershipAndAssignedReview(
        ForumDomainEvent event) {
        synchronizeOrganizationInbox(event);
        sendOrganizationReviewUpdateToAssignedResponder(event);
    }

    private void synchronizeOrganizationInbox(ForumDomainEvent event) {
        if (isInboxEligible(event.question())) {
            sendOrganizationInboxUpsert(event);
        } else {
            sendOrganizationInboxRemoval(event);
        }
    }

    private void sendOrganizationInboxUpsert(ForumDomainEvent event) {
        QuestionReviewInboxItemResponseDTO summary =
            toReviewSummary(event.question());

        organizationRecipientResolver
            .resolveInboxRecipients(event.taskAssignmentId())
            .forEach(responderId -> publisher.toPersonalReviews(
                responderId,
                event,
                INBOX_UPSERTED,
                new InboxUpsertPayload(summary)));
    }

    private void sendOrganizationInboxRemoval(ForumDomainEvent event) {
        List<Long> responderIds =
            organizationRecipientResolver.resolveInboxRecipients(
                event.taskAssignmentId());

        responderIds.forEach(responderId -> publisher.toPersonalReviews(
            responderId,
            event,
            INBOX_REMOVED,
            new InboxRemovalPayload(
                event.taskAssignmentId(),
                event.questionId())));
    }

    private void sendOrganizationReviewUpdateToAssignedResponder(
        ForumDomainEvent event) {
        Long responderId = resolveAssignedOrganizationResponder(event);

        if (responderId == null) {
            return;
        }

        sendOrganizationReviewUpdateToResponder(event, responderId);
    }

    private void sendOrganizationReviewUpdateToResponder(
        ForumDomainEvent event,
        Long responderId) {
        publisher.toPersonalReviews(
            responderId,
            event,
            REVIEW_UPDATED,
            new ReviewUpdatePayload(
                toReviewSummary(event.question())));
    }

    private void sendOrganizationMessageToAssignedResponder(
        ForumDomainEvent event,
        QuestionMessageResponseDTO message) {
        Long responderId = resolveAssignedOrganizationResponder(event);

        if (responderId == null) {
            return;
        }
        sendOrganizationMessageToResponder(
            event,
            responderId,
            message);
    }

    private void sendOrganizationMessageToResponder(
        ForumDomainEvent event,
        Long responderId,
        QuestionMessageResponseDTO message) {
        publisher.toPersonalReviews(
            responderId,
            event,
            MESSAGE_CREATED,
            new MessageCreatedPayload(message));
    }

    private Long resolveAssignedOrganizationResponder(
        ForumDomainEvent event) {
        Long reviewerId = event.question().assignedReviewerId();

        if (!organizationRecipientResolver.isOrganizationResponder(
            event.taskAssignmentId(),
            reviewerId)) {
            return null;
        }

        return reviewerId;
    }

    private boolean isInboxEligible(
        QuestionThreadResponseDTO question) {
        return question.state() == OPEN
            && question.status() == NEW
            && question.assignedReviewerId() == null;
    }

    private QuestionReviewInboxItemResponseDTO toReviewSummary(
        QuestionThreadResponseDTO question) {
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
}