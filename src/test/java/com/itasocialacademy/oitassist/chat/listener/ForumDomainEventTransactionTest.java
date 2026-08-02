package com.itasocialacademy.oitassist.chat.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionState;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus;
import com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility;
import com.itasocialacademy.oitassist.chat.event.ForumDomainEvent;
import com.itasocialacademy.oitassist.chat.event.QuestionCreatedDomainEvent;
import com.itasocialacademy.oitassist.chat.realtime.AdministratorRealtimeProjectionHandler;
import com.itasocialacademy.oitassist.chat.realtime.ParticipantRealtimeProjectionHandler;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class ForumDomainEventTransactionTest {

    @Test
    void committedTransaction_shouldDispatchAfterCommit() {
        try (AnnotationConfigApplicationContext context =
            new AnnotationConfigApplicationContext(
                TestConfiguration.class)) {

            ApplicationEventPublisher publisher =
                context;

            TransactionTemplate transactionTemplate =
                new TransactionTemplate(
                    context.getBean(
                        PlatformTransactionManager.class));

            RecordingParticipantHandler participantHandler =
                context.getBean(
                    RecordingParticipantHandler.class);

            RecordingAdministratorHandler administratorHandler =
                context.getBean(
                    RecordingAdministratorHandler.class);

            ForumDomainEvent event =
                createEvent();

            transactionTemplate.executeWithoutResult(status -> {
                publisher.publishEvent(event);

                assertTrue(
                    participantHandler.events.isEmpty());

                assertTrue(
                    administratorHandler.events.isEmpty());
            });

            assertEquals(
                List.of(event),
                participantHandler.events);

            assertEquals(
                List.of(event),
                administratorHandler.events);
        }
    }

    @Test
    void rolledBackTransaction_shouldNotDispatch() {
        try (AnnotationConfigApplicationContext context =
            new AnnotationConfigApplicationContext(
                TestConfiguration.class)) {

            ApplicationEventPublisher publisher =
                context;

            TransactionTemplate transactionTemplate =
                new TransactionTemplate(
                    context.getBean(
                        PlatformTransactionManager.class));

            RecordingParticipantHandler participantHandler =
                context.getBean(
                    RecordingParticipantHandler.class);

            RecordingAdministratorHandler administratorHandler =
                context.getBean(
                    RecordingAdministratorHandler.class);

            transactionTemplate.executeWithoutResult(status -> {
                publisher.publishEvent(
                    createEvent());

                status.setRollbackOnly();
            });

            assertTrue(
                participantHandler.events.isEmpty());

            assertTrue(
                administratorHandler.events.isEmpty());
        }
    }

    private ForumDomainEvent createEvent() {
        Instant now =
            Instant.parse(
                "2026-08-02T16:00:00Z");

        QuestionThreadResponseDTO question =
            QuestionThreadResponseDTO.builder()
                .id(10L)
                .taskAssignmentId(20L)
                .authorId(30L)
                .title("Question")
                .content("Question content")
                .status(QuestionStatus.NEW)
                .visibility(QuestionVisibility.PRIVATE)
                .state(QuestionState.OPEN)
                .version(0L)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return new QuestionCreatedDomainEvent(
            question,
            now);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    static class TestConfiguration {

        @Bean
        PlatformTransactionManager transactionManager() {
            return new TestTransactionManager();
        }

        @Bean
        RecordingParticipantHandler participantHandler() {

            return new RecordingParticipantHandler();
        }

        @Bean
        RecordingAdministratorHandler administratorHandler() {

            return new RecordingAdministratorHandler();
        }

        @Bean
        ForumDomainEventListener forumDomainEventListener(
            ObjectProvider<ParticipantRealtimeProjectionHandler> participantHandlers,
            ObjectProvider<AdministratorRealtimeProjectionHandler> administratorHandlers) {

            return new ForumDomainEventListener(
                participantHandlers,
                administratorHandlers);
        }
    }

    static final class RecordingParticipantHandler
        implements ParticipantRealtimeProjectionHandler {

        private final List<ForumDomainEvent> events =
            new ArrayList<>();

        @Override
        public void handle(
            ForumDomainEvent event) {

            events.add(event);
        }
    }

    static final class RecordingAdministratorHandler
        implements AdministratorRealtimeProjectionHandler {

        private final List<ForumDomainEvent> events =
            new ArrayList<>();

        @Override
        public void handle(
            ForumDomainEvent event) {

            events.add(event);
        }
    }

    static final class TestTransactionManager
        extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(
            Object transaction,
            TransactionDefinition definition) {
            // No external transactional resource is required.
        }

        @Override
        protected void doCommit(
            DefaultTransactionStatus status) {
            // AbstractPlatformTransactionManager triggers AFTER_COMMIT callbacks.
        }

        @Override
        protected void doRollback(
            DefaultTransactionStatus status) {
            // No external transactional resource is required.
        }
    }
}