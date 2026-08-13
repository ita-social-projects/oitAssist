package com.itasocialacademy.oitassist.chat.listener;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionState;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility;
import com.itasocialacademy.oitassist.chat.event.ForumDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionCreatedDomainEvent;
import com.itasocialacademy.oitassist.chat.realtime.handlers.AdministratorRealtimeProjectionHandler;
import com.itasocialacademy.oitassist.chat.realtime.handlers.OrganizationRealtimeProjectionHandler;
import com.itasocialacademy.oitassist.chat.realtime.handlers.ParticipantRealtimeProjectionHandler;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class ForumDomainEventListenerTest {

    @Test
    @SuppressWarnings("unchecked")
    void participantFailure_shouldNotPreventRemainingHandlers() {

        ObjectProvider<ParticipantRealtimeProjectionHandler> participantProvider =
            mock(ObjectProvider.class);

        ObjectProvider<AdministratorRealtimeProjectionHandler> administratorProvider =
            mock(ObjectProvider.class);

        ObjectProvider<OrganizationRealtimeProjectionHandler> organizationProvider =
            mock(ObjectProvider.class);

        ParticipantRealtimeProjectionHandler failingHandler =
            mock(
                ParticipantRealtimeProjectionHandler.class);

        ParticipantRealtimeProjectionHandler succeedingHandler =
            mock(
                ParticipantRealtimeProjectionHandler.class);

        AdministratorRealtimeProjectionHandler administratorHandler =
            mock(
                AdministratorRealtimeProjectionHandler.class);

        OrganizationRealtimeProjectionHandler organizationHandler =
            mock(
                OrganizationRealtimeProjectionHandler.class);

        ForumDomainEvent event =
            createEvent();

        when(participantProvider
            .orderedStream())
            .thenReturn(
                Stream.of(
                    failingHandler,
                    succeedingHandler));

        when(administratorProvider
            .orderedStream())
            .thenReturn(
                Stream.of(
                    administratorHandler));

        when(organizationProvider
            .orderedStream())
            .thenReturn(
                Stream.of(
                    organizationHandler));

        doThrow(
            new RuntimeException(
                "Simulated realtime failure"))
            .when(failingHandler)
            .handle(event);

        ForumDomainEventListener listener =
            new ForumDomainEventListener(
                participantProvider,
                administratorProvider,
                organizationProvider);

        assertDoesNotThrow(
            () -> listener
                .onForumDomainEvent(
                    event));

        verify(failingHandler)
            .handle(event);

        verify(succeedingHandler)
            .handle(event);

        verify(administratorHandler)
            .handle(event);

        verify(organizationHandler)
            .handle(event);
    }

    @Test
    @SuppressWarnings("unchecked")
    void organizationFailure_shouldNotPreventRemainingHandlers() {

        ObjectProvider<ParticipantRealtimeProjectionHandler> participantProvider =
            mock(ObjectProvider.class);

        ObjectProvider<AdministratorRealtimeProjectionHandler> administratorProvider =
            mock(ObjectProvider.class);

        ObjectProvider<OrganizationRealtimeProjectionHandler> organizationProvider =
            mock(ObjectProvider.class);

        ParticipantRealtimeProjectionHandler participantHandler =
            mock(
                ParticipantRealtimeProjectionHandler.class);

        AdministratorRealtimeProjectionHandler administratorHandler =
            mock(
                AdministratorRealtimeProjectionHandler.class);

        OrganizationRealtimeProjectionHandler failingHandler =
            mock(
                OrganizationRealtimeProjectionHandler.class);

        OrganizationRealtimeProjectionHandler succeedingHandler =
            mock(
                OrganizationRealtimeProjectionHandler.class);

        ForumDomainEvent event =
            createEvent();

        when(participantProvider
            .orderedStream())
            .thenReturn(
                Stream.of(
                    participantHandler));

        when(administratorProvider
            .orderedStream())
            .thenReturn(
                Stream.of(
                    administratorHandler));

        when(organizationProvider
            .orderedStream())
            .thenReturn(
                Stream.of(
                    failingHandler,
                    succeedingHandler));

        doThrow(
            new RuntimeException(
                "Simulated ORG realtime failure"))
            .when(failingHandler)
            .handle(event);

        ForumDomainEventListener listener =
            new ForumDomainEventListener(
                participantProvider,
                administratorProvider,
                organizationProvider);

        assertDoesNotThrow(
            () -> listener
                .onForumDomainEvent(
                    event));

        verify(participantHandler)
            .handle(event);

        verify(administratorHandler)
            .handle(event);

        verify(failingHandler)
            .handle(event);

        verify(succeedingHandler)
            .handle(event);
    }

    private ForumDomainEvent createEvent() {

        Instant now =
            Instant.parse(
                "2026-08-05T16:00:00Z");

        QuestionThreadResponseDTO question =
            QuestionThreadResponseDTO.builder()
                .id(10L)
                .taskAssignmentId(20L)
                .authorId(30L)
                .title("Question")
                .content("Question content")
                .status(
                    QuestionStatus.NEW)
                .visibility(
                    QuestionVisibility.PRIVATE)
                .state(
                    QuestionState.OPEN)
                .version(0L)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return new QuestionCreatedDomainEvent(
            question,
            now);
    }
}