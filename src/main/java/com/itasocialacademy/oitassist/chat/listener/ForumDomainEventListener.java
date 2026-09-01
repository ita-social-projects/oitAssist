package com.itasocialacademy.oitassist.chat.listener;

import com.itasocialacademy.oitassist.chat.event.domain.ForumDomainEvent;
import com.itasocialacademy.oitassist.chat.realtime.ParticipantRealtimeHandler;
import com.itasocialacademy.oitassist.chat.realtime.ReviewRealtimeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ForumDomainEventListener {
    private static final String PARTICIPANT_PROJECTION = "participant";
    private static final String ADMINISTRATOR_PROJECTION = "administrator";
    private static final String ORGANIZATION_PROJECTION = "organization";

    private final ParticipantRealtimeHandler participantRealtimeHandler;
    private final ReviewRealtimeHandler reviewRealtimeHandler;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onForumDomainEvent(ForumDomainEvent event) {
        dispatch(
            event,
            PARTICIPANT_PROJECTION,
            participantRealtimeHandler.getClass().getSimpleName(),
            () -> participantRealtimeHandler.handle(event));
        dispatch(
            event,
            ADMINISTRATOR_PROJECTION,
            reviewRealtimeHandler.getClass().getSimpleName(),
            () -> reviewRealtimeHandler.projectAdministrator(event));
        dispatch(
            event,
            ORGANIZATION_PROJECTION,
            reviewRealtimeHandler.getClass().getSimpleName(),
            () -> reviewRealtimeHandler.projectOrganization(event));
    }

    private void dispatch(ForumDomainEvent event, String projection, String handlerName, Runnable projectionAction) {
        try {
            projectionAction.run();
        } catch (RuntimeException exception) {
            /*
             * The database transaction has already committed. Realtime delivery failures
             * must not propagate back into the completed REST operation.
             *
             * Do not log exception messages because a handler exception could accidentally
             * contain protected question or message data.
             */
            log.error(
                "Realtime projection delivery failed: eventType={}, taskAssignmentId={}, questionId={}, "
                    + "projection={}, handler={}, exceptionType={}",
                event.getClass().getSimpleName(),
                event.taskAssignmentId(),
                event.questionId(),
                projection,
                handlerName,
                exception.getClass().getName());
        }
    }
}