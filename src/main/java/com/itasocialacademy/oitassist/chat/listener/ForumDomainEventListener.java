package com.itasocialacademy.oitassist.chat.listener;

import com.itasocialacademy.oitassist.chat.event.ForumDomainEvent;
import com.itasocialacademy.oitassist.chat.realtime.handlers.AdministratorRealtimeProjectionHandler;
import com.itasocialacademy.oitassist.chat.realtime.handlers.ForumRealtimeProjectionHandler;
import com.itasocialacademy.oitassist.chat.realtime.handlers.OrganizationRealtimeProjectionHandler;
import com.itasocialacademy.oitassist.chat.realtime.handlers.ParticipantRealtimeProjectionHandler;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ForumDomainEventListener {
    private static final String PARTICIPANT_PROJECTION =
        "participant";

    private static final String ADMINISTRATOR_PROJECTION =
        "administrator";

    private static final String ORGANIZATION_PROJECTION =
        "organization";

    private final ObjectProvider<ParticipantRealtimeProjectionHandler> participantHandlers;

    private final ObjectProvider<AdministratorRealtimeProjectionHandler> administratorHandlers;

    private final ObjectProvider<OrganizationRealtimeProjectionHandler> organizationHandlers;

    @TransactionalEventListener(
        phase = TransactionPhase.AFTER_COMMIT)
    public void onForumDomainEvent(
        ForumDomainEvent event) {
        dispatch(
            event,
            participantHandlers.orderedStream(),
            PARTICIPANT_PROJECTION);

        dispatch(
            event,
            administratorHandlers.orderedStream(),
            ADMINISTRATOR_PROJECTION);

        dispatch(
            event,
            organizationHandlers.orderedStream(),
            ORGANIZATION_PROJECTION);
    }

    private void dispatch(
        ForumDomainEvent event,
        Stream<? extends ForumRealtimeProjectionHandler> handlers,
        String projection) {
        handlers.forEach(handler -> {
            try {
                handler.handle(event);
            } catch (RuntimeException exception) {
                /*
                 * The database transaction has already committed. Realtime delivery failures
                 * must not propagate back into the completed REST operation.
                 *
                 * Do not log exception messages because a handler exception could accidentally
                 * contain protected question or message data.
                 */
                log.error(
                    "Realtime projection delivery failed: "
                        + "eventType={}, taskAssignmentId={}, "
                        + "questionId={}, projection={}, handler={}, "
                        + "exceptionType={}",
                    event.getClass().getSimpleName(),
                    event.taskAssignmentId(),
                    event.questionId(),
                    projection,
                    handler.getClass().getSimpleName(),
                    exception.getClass().getName());
            }
        });
    }
}