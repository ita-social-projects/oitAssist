package com.itasocialacademy.oitassist.chat.realtime;

import com.itasocialacademy.oitassist.chat.event.domain.ForumDomainEvent;
import com.itasocialacademy.oitassist.chat.event.realtime.RealtimeEventType;
import com.itasocialacademy.oitassist.chat.event.realtime.RealtimeForumEvent;
import com.itasocialacademy.oitassist.chat.event.realtime.RealtimePayload;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ForumRealtimePublisher {
    private static final String PERSONAL_QUESTIONS_QUEUE = "/queue/questions";
    private static final String PERSONAL_REVIEWS_QUEUE = "/queue/reviews";
    private static final String TASK_ASSIGNMENT_FORUM_DESTINATION = "/topic/task-assignments/%d/questions";
    private static final String QUESTION_THREAD_DESTINATION = "/topic/questions/%d";
    private static final String ADMINISTRATOR_INBOX_DESTINATION = "/topic/admin/questions/inbox";
    private static final String ADMINISTRATOR_ALL_QUESTIONS_DESTINATION = "/topic/admin/questions/all";

    private final SimpMessageSendingOperations messagingOperations;

    public void toTaskAssignmentForum(ForumDomainEvent event, RealtimeEventType type, RealtimePayload payload) {
        messagingOperations.convertAndSend(
            TASK_ASSIGNMENT_FORUM_DESTINATION.formatted(event.taskAssignmentId()),
            createRealtimeEvent(event, type, payload));
    }

    public void toQuestionThread(ForumDomainEvent event, RealtimeEventType type, RealtimePayload payload) {
        messagingOperations.convertAndSend(
            QUESTION_THREAD_DESTINATION.formatted(event.questionId()),
            createRealtimeEvent(event, type, payload));
    }

    public void toAdministratorInbox(ForumDomainEvent event, RealtimeEventType type, RealtimePayload payload) {
        messagingOperations.convertAndSend(
            ADMINISTRATOR_INBOX_DESTINATION,
            createRealtimeEvent(event, type, payload));
    }

    public void toAdministratorAllQuestions(ForumDomainEvent event, RealtimeEventType type, RealtimePayload payload) {
        messagingOperations.convertAndSend(
            ADMINISTRATOR_ALL_QUESTIONS_DESTINATION,
            createRealtimeEvent(event, type, payload));
    }

    public void toPersonalQuestions(
        Long userId,
        ForumDomainEvent event,
        RealtimeEventType type,
        RealtimePayload payload) {
        sendToUser(userId, PERSONAL_QUESTIONS_QUEUE, event, type, payload);
    }

    public void toPersonalReviews(
        Long userId, ForumDomainEvent event, RealtimeEventType type, RealtimePayload payload) {
        sendToUser(userId, PERSONAL_REVIEWS_QUEUE, event, type, payload);
    }

    private void sendToUser(
        Long userId,
        String destination,
        ForumDomainEvent event,
        RealtimeEventType type,
        RealtimePayload payload) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Realtime recipient id must be positive");
        }

        messagingOperations.convertAndSendToUser(
            userId.toString(),
            destination,
            createRealtimeEvent(event, type, payload));
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
}