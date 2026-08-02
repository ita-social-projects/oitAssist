package com.itasocialacademy.oitassist.chat.utils;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PUBLIC;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthorizationChannelInterceptor
    implements ChannelInterceptor {
    private static final String AUTHENTICATION_REQUIRED =
        "STOMP authentication is required";

    private static final String SUBSCRIPTION_NOT_ALLOWED =
        "STOMP subscription is not allowed";

    private final RealtimeSubscriptionDestinationParser destinationParser;

    private final QuestionAccessPolicy questionAccessPolicy;
    private final QuestionThreadRepository questionThreadRepository;

    @Override
    public Message<?> preSend(
        Message<?> message,
        MessageChannel channel) {
        StompHeaderAccessor accessor =
            MessageHeaderAccessor.getAccessor(
                message,
                StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (command == StompCommand.SEND) {
            throw subscriptionNotAllowed();
        }

        if (command == StompCommand.SUBSCRIBE) {
            authorizeSubscription(accessor);
        }

        return message;
    }

    private void authorizeSubscription(
        StompHeaderAccessor accessor) {
        requireAuthentication(accessor.getUser());

        RealtimeSubscriptionDestination destination =
            destinationParser.parse(
                accessor.getDestination());

        try {
            switch (destination.type()) {
                case TASK_ASSIGNMENT_FORUM ->
                    authorizeTaskAssignmentForum(
                        destination.resourceId());

                case PUBLIC_QUESTION_THREAD ->
                    authorizePublicQuestion(
                        destination.resourceId());

                case ADMINISTRATOR_INBOX,
                    ADMINISTRATOR_REVIEWS ->
                    requireAdministrator();

                case PARTICIPANT_QUESTIONS -> {
                    // Every authenticated user may subscribe to their own
                    // Spring-resolved personal destination.
                }
                default -> {
                    throw subscriptionNotAllowed();
                }
            }
        } catch (RuntimeException exception) {
            /*
             * Deliberately discard the underlying exception and message.
             *
             * The client must not be able to distinguish: - a missing question; - a private
             * question; - an inaccessible TaskAssignment; - a missing participation; - an
             * invalid hierarchy.
             */
            throw subscriptionNotAllowed();
        }
    }

    private void authorizeTaskAssignmentForum(
        Long taskAssignmentId) {
        questionAccessPolicy
            .requireTaskAssignmentForumAccess(
                taskAssignmentId);
    }

    private void authorizePublicQuestion(
        Long questionId) {
        QuestionThread question = questionThreadRepository
            .findById(questionId)
            .orElseThrow(
                StompAuthorizationChannelInterceptor::subscriptionNotAllowed);

        if (question.getVisibility() != PUBLIC) {
            throw subscriptionNotAllowed();
        }

        questionAccessPolicy
            .requireQuestionViewAccess(question);
    }

    private void requireAdministrator() {
        if (!questionAccessPolicy.isAdministrator()) {
            throw subscriptionNotAllowed();
        }
    }

    private void requireAuthentication(
        Principal principal) {
        if (!(principal instanceof Authentication authentication)
            || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException(
                AUTHENTICATION_REQUIRED);
        }
    }

    private static AccessDeniedException subscriptionNotAllowed() {
        return new AccessDeniedException(
            SUBSCRIPTION_NOT_ALLOWED);
    }
}