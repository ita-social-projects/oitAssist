package com.itasocialacademy.oitassist.chat.integration;

import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionState.OPEN;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.NEW;
import static com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PRIVATE;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.chat.config.RealtimeMessagingProperties;
import com.itasocialacademy.oitassist.chat.config.WebSocketMessagingConfig;
import com.itasocialacademy.oitassist.chat.controller.AdministratorQuestionController;
import com.itasocialacademy.oitassist.chat.controller.ParticipantForumController;
import com.itasocialacademy.oitassist.chat.controller.ParticipantQuestionController;
import com.itasocialacademy.oitassist.chat.dao.dto.response.QuestionThreadResponseDTO;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionMessage;
import com.itasocialacademy.oitassist.chat.dao.model.QuestionThread;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionMessageRepository;
import com.itasocialacademy.oitassist.chat.dao.repository.QuestionThreadRepository;
import com.itasocialacademy.oitassist.chat.event.QuestionCreatedDomainEvent;
import com.itasocialacademy.oitassist.chat.listener.ForumDomainEventListener;
import com.itasocialacademy.oitassist.chat.mapper.QuestionMessageMapper;
import com.itasocialacademy.oitassist.chat.mapper.QuestionThreadMapper;
import com.itasocialacademy.oitassist.chat.realtime.AdministratorRealtimeProjectionHandlerImpl;
import com.itasocialacademy.oitassist.chat.realtime.ParticipantRealtimeProjectionHandlerImpl;
import com.itasocialacademy.oitassist.chat.service.AdministratorQuestionServiceImpl;
import com.itasocialacademy.oitassist.chat.service.ParticipantForumServiceImpl;
import com.itasocialacademy.oitassist.chat.service.ParticipantQuestionServiceImpl;
import com.itasocialacademy.oitassist.chat.utils.QuestionAccessPolicy;
import com.itasocialacademy.oitassist.chat.utils.QuestionClaimFailureClassifier;
import com.itasocialacademy.oitassist.chat.utils.RealtimeSubscriptionDestinationParser;
import com.itasocialacademy.oitassist.chat.utils.StompAuthenticationChannelInterceptor;
import com.itasocialacademy.oitassist.chat.utils.StompAuthorizationChannelInterceptor;
import com.itasocialacademy.oitassist.core.web.AppExceptionHttpStatusMapper;
import com.itasocialacademy.oitassist.core.web.GlobalExceptionHandler;
import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import com.itasocialacademy.oitassist.security.api.facade.StompAuthenticationFacadeImpl;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityUserProvider;
import com.itasocialacademy.oitassist.security.api.interfaces.StompAuthenticationFacade;
import com.itasocialacademy.oitassist.security.jwt.JwtFilter;
import com.itasocialacademy.oitassist.security.jwt.JwtHelper;
import com.itasocialacademy.oitassist.security.jwt.JwtProperties;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Service;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.containers.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
    classes = AbstractForumRealtimeIT.ForumRealtimeIntegrationTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.config.location=classpath:/realtime-integration-test.properties"
    })
@AutoConfigureMockMvc
abstract class AbstractForumRealtimeIT {

    protected static final String ALLOWED_ORIGIN =
        "http://localhost:3000";

    protected static final String UNSUPPORTED_ORIGIN =
        "https://evil.example";

    protected static final Long USER_ID = 101L;
    protected static final Long OTHER_USER_ID = 102L;
    protected static final Long ADMIN_ID = 201L;
    protected static final Long OTHER_ADMIN_ID = 202L;

    protected static final String USER_EMAIL =
        "participant@example.com";

    protected static final String OTHER_USER_EMAIL =
        "other-participant@example.com";

    protected static final String ADMIN_EMAIL =
        "administrator@example.com";

    protected static final String OTHER_ADMIN_EMAIL =
        "other-administrator@example.com";

    protected static final Long ACCESSIBLE_TASK_ASSIGNMENT_ID =
        1001L;

    protected static final Long INACCESSIBLE_TASK_ASSIGNMENT_ID =
        1002L;

    protected static final String PERSONAL_QUESTIONS_DESTINATION =
        "/user/queue/questions";

    protected static final String PERSONAL_QUESTIONS_SUFFIX =
        "/queue/questions";

    protected static final String PERSONAL_REVIEWS_DESTINATION =
        "/user/queue/reviews";

    protected static final String PERSONAL_REVIEWS_SUFFIX =
        "/queue/reviews";

    protected static final String ADMIN_INBOX_DESTINATION =
        "/topic/admin/questions/inbox";

    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void databaseProperties(
        DynamicPropertyRegistry registry) {

        registry.add(
            "spring.datasource.url",
            POSTGRES::getJdbcUrl);

        registry.add(
            "spring.datasource.username",
            POSTGRES::getUsername);

        registry.add(
            "spring.datasource.password",
            POSTGRES::getPassword);

        registry.add(
            "spring.datasource.driver-class-name",
            POSTGRES::getDriverClassName);
    }

    @LocalServerPort
    protected int port;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtHelper jwtHelper;

    @Autowired
    protected SimpMessagingTemplate messagingTemplate;

    @Autowired
    protected SimpUserRegistry simpUserRegistry;

    @Autowired
    protected QuestionThreadRepository questionThreadRepository;

    @Autowired
    protected QuestionMessageRepository questionMessageRepository;

    private final List<WebSocketStompClient> clients =
        new CopyOnWriteArrayList<>();

    private final List<StompSession> sessions =
        new CopyOnWriteArrayList<>();

    @BeforeEach
    void cleanDatabase() {
        questionMessageRepository.deleteAllInBatch();
        questionThreadRepository.deleteAllInBatch();
    }

    @AfterEach
    void closeWebSocketClients() {
        sessions.stream()
            .filter(StompSession::isConnected)
            .forEach(StompSession::disconnect);

        clients.forEach(WebSocketStompClient::stop);

        sessions.clear();
        clients.clear();
    }

    protected String userToken() {
        return accessToken(USER_EMAIL);
    }

    protected String otherUserToken() {
        return accessToken(OTHER_USER_EMAIL);
    }

    protected String adminToken() {
        return accessToken(ADMIN_EMAIL);
    }

    protected String otherAdminToken() {
        return accessToken(OTHER_ADMIN_EMAIL);
    }

    protected String accessToken(
        String email) {

        return jwtHelper.createToken(
            Map.of(
                "token_type",
                JwtHelper.ACCESS_TOKEN),
            email);
    }

    protected String wrongTypeToken(
        String email) {

        return jwtHelper.createToken(
            Map.of(
                "token_type",
                JwtHelper.REFRESH_TOKEN),
            email);
    }

    protected String bearer(
        String token) {

        return "Bearer " + token;
    }

    protected String forumDestination(
        Long taskAssignmentId) {

        return "/topic/task-assignments/%d/questions"
            .formatted(taskAssignmentId);
    }

    protected String questionDestination(
        Long questionId) {

        return "/topic/questions/%d"
            .formatted(questionId);
    }

    protected StompSession connect(
        String authorizationHeader)
        throws Exception {

        return connect(
            ALLOWED_ORIGIN,
            authorizationHeader,
            new RecordingSessionHandler());
    }

    protected StompSession connect(
        String origin,
        String authorizationHeader,
        RecordingSessionHandler sessionHandler)
        throws Exception {

        WebSocketStompClient client =
            createClient();

        WebSocketHttpHeaders handshakeHeaders =
            new WebSocketHttpHeaders();

        handshakeHeaders.setOrigin(origin);

        StompHeaders connectHeaders =
            new StompHeaders();

        if (authorizationHeader != null) {
            connectHeaders.add(
                HttpHeaders.AUTHORIZATION,
                authorizationHeader);
        }

        StompSession session =
            client.connectAsync(
                webSocketUrl(),
                handshakeHeaders,
                connectHeaders,
                sessionHandler)
                .get(
                    5,
                    TimeUnit.SECONDS);

        sessions.add(session);

        return session;
    }

    protected void assertConnectRejected(
        String origin,
        String authorizationHeader) {

        WebSocketStompClient client =
            createClient();

        WebSocketHttpHeaders handshakeHeaders =
            new WebSocketHttpHeaders();

        handshakeHeaders.setOrigin(origin);

        StompHeaders connectHeaders =
            new StompHeaders();

        if (authorizationHeader != null) {
            connectHeaders.add(
                HttpHeaders.AUTHORIZATION,
                authorizationHeader);
        }

        CompletableFuture<StompSession> connection =
            client.connectAsync(
                webSocketUrl(),
                handshakeHeaders,
                connectHeaders,
                new RecordingSessionHandler());

        try {
            connection.get(
                5,
                TimeUnit.SECONDS);

            fail(
                "Expected STOMP connection to be rejected");
        } catch (ExecutionException expected) {
            // Expected.
        } catch (Exception exception) {
            fail(
                "Connection did not fail normally",
                exception);
        }
    }

    protected void assertSubscriptionRejected(
        StompSession session,
        RecordingSessionHandler sessionHandler,
        String destination)
        throws InterruptedException {

        session.subscribe(
            destination,
            new NoOpFrameHandler());

        if (!sessionHandler.awaitFailure(
            Duration.ofSeconds(5))) {

            fail(
                "Expected subscription to be rejected: "
                    + destination);
        }
    }

    protected SubscriptionProbe subscribeShared(
        StompSession session,
        Long userId,
        String destination)
        throws InterruptedException {

        return subscribeAndAwaitReady(
            session,
            userId.toString(),
            destination,
            marker -> messagingTemplate.convertAndSend(
                destination,
                marker));
    }

    protected SubscriptionProbe subscribePersonal(
        StompSession session,
        Long userId,
        String subscriptionDestination,
        String destinationSuffix)
        throws InterruptedException {

        return subscribeAndAwaitReady(
            session,
            userId.toString(),
            subscriptionDestination,
            marker -> messagingTemplate.convertAndSendToUser(
                userId.toString(),
                destinationSuffix,
                marker));
    }

    private SubscriptionProbe subscribeAndAwaitReady(
        StompSession session,
        String registryUserName,
        String destination,
        Consumer<String> markerSender)
        throws InterruptedException {

        String marker =
            "__subscription_ready__:"
                + java.util.UUID.randomUUID();

        SubscriptionProbe probe =
            new SubscriptionProbe(marker);

        session.subscribe(
            destination,
            new ProbeFrameHandler(probe));

        awaitSubscriptionRegistered(
            registryUserName,
            destination);

        long deadline =
            System.nanoTime()
                + TimeUnit.SECONDS.toNanos(5);

        while (System.nanoTime() < deadline) {
            markerSender.accept(marker);

            if (probe.awaitReadiness(
                Duration.ofMillis(100))) {

                return probe;
            }
        }

        fail(
            "STOMP subscription did not become delivery-ready: "
                + destination);

        throw new AssertionError(
            "Unreachable");
    }

    private void awaitSubscriptionRegistered(
        String userName,
        String expectedDestination)
        throws InterruptedException {

        long deadline =
            System.nanoTime()
                + TimeUnit.SECONDS.toNanos(5);

        while (System.nanoTime() < deadline) {
            var user =
                simpUserRegistry.getUser(
                    userName);

            boolean registered =
                user != null
                    && user.getSessions()
                        .stream()
                        .flatMap(session -> session.getSubscriptions()
                            .stream())
                        .anyMatch(subscription -> expectedDestination.equals(
                            subscription
                                .getDestination()));

            if (registered) {
                return;
            }

            Thread.sleep(25);
        }

        fail(
            "Subscription was not registered: "
                + "user="
                + userName
                + ", destination="
                + expectedDestination);
    }

    protected JsonNode awaitEvent(
        SubscriptionProbe probe,
        String expectedType,
        Long expectedQuestionId)
        throws Exception {

        long deadline =
            System.nanoTime()
                + TimeUnit.SECONDS.toNanos(5);

        while (System.nanoTime() < deadline) {
            String payload =
                probe.poll(
                    Duration.ofMillis(100));

            if (payload == null) {
                continue;
            }

            JsonNode event =
                objectMapper.readTree(payload);

            if (expectedType.equals(
                event.path("type").asText())
                && expectedQuestionId.equals(
                    event.path("questionId")
                        .asLong())) {

                return event;
            }
        }

        fail(
            "Expected realtime event was not received: "
                + "type="
                + expectedType
                + ", questionId="
                + expectedQuestionId);

        throw new AssertionError(
            "Unreachable");
    }

    protected void assertNoMessage(
        SubscriptionProbe probe,
        Duration duration)
        throws InterruptedException {

        String message =
            probe.poll(duration);

        if (message != null) {
            fail(
                "Unexpected STOMP message: "
                    + message);
        }
    }

    protected QuestionThread saveQuestion(
        com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility visibility) {

        return saveQuestion(
            visibility,
            null);
    }

    protected QuestionThread saveQuestion(
        com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility visibility,
        Long reviewerId) {

        Instant now =
            Instant.now();

        return questionThreadRepository.saveAndFlush(
            QuestionThread.builder()
                .taskAssignmentId(
                    ACCESSIBLE_TASK_ASSIGNMENT_ID)
                .authorId(USER_ID)
                .assignedReviewerId(reviewerId)
                .title("Integration question")
                .content(
                    "Integration question content")
                .status(
                    reviewerId == null
                        ? NEW
                        : com.itasocialacademy.oitassist.chat.dao.enums.QuestionStatus.IN_REVIEW)
                .state(OPEN)
                .visibility(visibility)
                .version(0L)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private WebSocketStompClient createClient() {
        WebSocketStompClient client =
            new WebSocketStompClient(
                new StandardWebSocketClient());

        client.start();

        clients.add(client);

        return client;
    }

    private String webSocketUrl() {
        return "ws://localhost:%d/ws"
            .formatted(port);
    }

    protected static Optional<Long> currentUserId() {

        Authentication authentication =
            SecurityContextHolder.getContext()
                .getAuthentication();

        if (authentication == null
            || !authentication.isAuthenticated()) {

            return Optional.empty();
        }

        Object principal =
            authentication.getPrincipal();

        if (principal instanceof UserDetailsImpl user) {
            return Optional.ofNullable(
                user.getId());
        }

        if (principal instanceof Principal named) {
            try {
                return Optional.of(
                    Long.valueOf(
                        named.getName()));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }

        try {
            return Optional.of(
                Long.valueOf(
                    authentication.getName()));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    protected static boolean currentUserHasRole(
        String role) {

        Authentication authentication =
            SecurityContextHolder.getContext()
                .getAuthentication();

        if (authentication == null
            || !authentication.isAuthenticated()) {

            return false;
        }

        String expectedAuthority =
            role.startsWith("ROLE_")
                ? role
                : "ROLE_" + role;

        return authentication.getAuthorities()
            .stream()
            .anyMatch(authority -> expectedAuthority.equals(
                authority.getAuthority()));
    }

    protected static Long requireCurrentUserId() {
        return currentUserId()
            .orElseThrow(() -> new AuthenticationCredentialsNotFoundException(
                "Authentication is required"));
    }

    protected static final class SubscriptionProbe {

        private final String readinessMarker;

        private final java.util.concurrent.CountDownLatch readiness =
            new java.util.concurrent.CountDownLatch(1);

        private final BlockingQueue<String> messages =
            new LinkedBlockingQueue<>();

        private SubscriptionProbe(
            String readinessMarker) {

            this.readinessMarker =
                readinessMarker;
        }

        private void accept(
            String payload) {

            if (readinessMarker.equals(payload)) {
                readiness.countDown();
                return;
            }

            messages.add(payload);
        }

        private boolean awaitReadiness(
            Duration duration)
            throws InterruptedException {

            return readiness.await(
                duration.toMillis(),
                TimeUnit.MILLISECONDS);
        }

        protected String poll(
            Duration duration)
            throws InterruptedException {

            return messages.poll(
                duration.toMillis(),
                TimeUnit.MILLISECONDS);
        }
    }

    protected static final class RecordingSessionHandler
        extends StompSessionHandlerAdapter {

        private final CompletableFuture<Throwable> failure =
            new CompletableFuture<>();

        @Override
        public Type getPayloadType(
            StompHeaders headers) {

            return byte[].class;
        }

        @Override
        public void handleFrame(
            StompHeaders headers,
            Object payload) {

            failure.complete(
                new IllegalStateException(
                    decodePayload(payload)));
        }

        @Override
        public void handleException(
            StompSession session,
            StompCommand command,
            StompHeaders headers,
            byte[] payload,
            Throwable exception) {

            failure.complete(exception);
        }

        @Override
        public void handleTransportError(
            StompSession session,
            Throwable exception) {

            failure.complete(exception);
        }

        protected boolean awaitFailure(
            Duration duration)
            throws InterruptedException {

            try {
                failure.get(
                    duration.toMillis(),
                    TimeUnit.MILLISECONDS);

                return true;
            } catch (TimeoutException exception) {
                return false;
            } catch (ExecutionException exception) {
                return true;
            }
        }
    }

    private static final class ProbeFrameHandler
        implements StompFrameHandler {

        private final SubscriptionProbe probe;

        private ProbeFrameHandler(
            SubscriptionProbe probe) {

            this.probe = probe;
        }

        @Override
        public Type getPayloadType(
            StompHeaders headers) {

            return byte[].class;
        }

        @Override
        public void handleFrame(
            StompHeaders headers,
            Object payload) {

            probe.accept(
                decodePayload(payload));
        }
    }

    private static final class NoOpFrameHandler
        implements StompFrameHandler {

        @Override
        public Type getPayloadType(
            StompHeaders headers) {

            return byte[].class;
        }

        @Override
        public void handleFrame(
            StompHeaders headers,
            Object payload) {
            // Nothing to record.
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(
        basePackageClasses = {
            QuestionThread.class,
            QuestionMessage.class
        })
    @EnableJpaRepositories(
        basePackageClasses = QuestionThreadRepository.class)
    @EnableJpaAuditing(
        auditorAwareRef = "integrationAuditorAware")
    @EnableMethodSecurity
    @EnableConfigurationProperties(RealtimeMessagingProperties.class)
    @Import({
        WebSocketMessagingConfig.class,
        StompAuthenticationChannelInterceptor.class,
        StompAuthorizationChannelInterceptor.class,
        RealtimeSubscriptionDestinationParser.class,

        ParticipantForumController.class,
        ParticipantQuestionController.class,
        AdministratorQuestionController.class,

        ParticipantForumServiceImpl.class,
        ParticipantQuestionServiceImpl.class,
        AdministratorQuestionServiceImpl.class,

        ForumDomainEventListener.class,
        ParticipantRealtimeProjectionHandlerImpl.class,
        AdministratorRealtimeProjectionHandlerImpl.class,

        AppExceptionHttpStatusMapper.class,
        GlobalExceptionHandler.class,

        IntegrationBeansConfiguration.class,
        RollbackProbeController.class,
        RollbackProbeService.class
    })
    static class ForumRealtimeIntegrationTestApplication {
    }

    @Configuration(proxyBeanMethods = false)
    static class IntegrationBeansConfiguration {

        private static final String JWT_ENCRYPTED_KEY =
            encodeKey(
                48,
                (byte) 11);

        private static final String JWT_SIGN_KEY =
            encodeKey(
                64,
                (byte) 29);

        @Bean
        JwtProperties jwtProperties() {
            JwtProperties properties =
                new JwtProperties();

            properties.setEncryptedKey(
                JWT_ENCRYPTED_KEY);

            properties.setSignKey(
                JWT_SIGN_KEY);

            properties.setValidity(
                Duration.ofHours(1)
                    .toMillis());

            properties.setRefreshValidity(
                Duration.ofDays(1)
                    .toMillis());

            return properties;
        }

        @Bean
        JwtHelper jwtHelper(
            JwtProperties properties) {

            return new JwtHelper(properties);
        }

        @Bean
        @Primary
        SecurityUserProvider securityUserProvider() {
            Map<String, UserDetailsImpl> users =
                Map.of(
                    USER_EMAIL,
                    user(
                        USER_ID,
                        USER_EMAIL,
                        "ROLE_USER"),

                    OTHER_USER_EMAIL,
                    user(
                        OTHER_USER_ID,
                        OTHER_USER_EMAIL,
                        "ROLE_USER"),

                    ADMIN_EMAIL,
                    user(
                        ADMIN_ID,
                        ADMIN_EMAIL,
                        "ROLE_ADMIN"),

                    OTHER_ADMIN_EMAIL,
                    user(
                        OTHER_ADMIN_ID,
                        OTHER_ADMIN_EMAIL,
                        "ROLE_ADMIN"));

            return email -> Optional.ofNullable(
                users.get(email));
        }

        @Bean
        @Primary
        UserDetailsService userDetailsService(
            SecurityUserProvider provider) {

            return email -> provider.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                    email));
        }

        @Bean
        @Primary
        StompAuthenticationFacade stompAuthenticationFacade(
            JwtHelper jwtHelper,
            SecurityUserProvider provider) {

            return new StompAuthenticationFacadeImpl(
                jwtHelper,
                provider);
        }

        @Bean
        JwtFilter jwtFilter(
            UserDetailsService userDetailsService,
            JwtHelper jwtHelper,
            GlobalExceptionHandler handler,
            ObjectMapper objectMapper) {

            return new JwtFilter(
                userDetailsService,
                jwtHelper,
                handler,
                objectMapper);
        }

        @Bean
        SecurityFilterChain integrationSecurityChain(
            HttpSecurity http,
            JwtFilter jwtFilter)
            throws Exception {

            return http
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers(
                        "/ws",
                        "/ws/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
                .addFilterBefore(
                    jwtFilter,
                    UsernamePasswordAuthenticationFilter.class)
                .build();
        }

        @Bean
        QuestionThreadMapper questionThreadMapper() {
            return Mappers.getMapper(
                QuestionThreadMapper.class);
        }

        @Bean
        QuestionMessageMapper questionMessageMapper() {
            return Mappers.getMapper(
                QuestionMessageMapper.class);
        }

        @Bean
        QuestionAccessPolicy questionAccessPolicy() {
            QuestionAccessPolicy policy =
                mock(QuestionAccessPolicy.class);

            when(policy
                .requireTaskAssignmentForumAccess(
                    anyLong()))
                .thenAnswer(invocation -> requireAssignmentAccess(
                    invocation.getArgument(0)));

            when(policy
                .requireTaskAssignmentQuestionCreationAccess(
                    anyLong()))
                .thenAnswer(invocation -> requireAssignmentAccess(
                    invocation.getArgument(0)));

            when(policy
                .requireQuestionCommentAccess(
                    any(QuestionThread.class)))
                .thenAnswer(invocation -> {
                    QuestionThread question =
                        invocation.getArgument(0);

                    requireQuestionAccess(
                        question);

                    return requireCurrentUserId();
                });

            doAnswer(invocation -> {
                QuestionThread question =
                    invocation.getArgument(0);

                requireQuestionAccess(
                    question);

                return null;
            }).when(policy)
                .requireQuestionViewAccess(
                    any(QuestionThread.class));

            when(policy.isAdministrator())
                .thenAnswer(invocation -> currentUserHasRole(
                    "ADMIN"));

            when(policy.isAuthor(
                any(QuestionThread.class)))
                .thenAnswer(invocation -> {
                    QuestionThread question =
                        invocation.getArgument(0);

                    return currentUserId()
                        .map(id -> id.equals(
                            question.getAuthorId()))
                        .orElse(false);
                });

            when(policy.isAssignedReviewer(
                any(QuestionThread.class)))
                .thenAnswer(invocation -> {
                    QuestionThread question =
                        invocation.getArgument(0);

                    return currentUserId()
                        .map(id -> id.equals(
                            question
                                .getAssignedReviewerId()))
                        .orElse(false);
                });

            return policy;
        }

        @Bean
        SecurityFacade securityFacade() {
            return new SecurityFacade() {

                @Override
                public Optional<String> getCurrentUserEmail() {

                    Authentication authentication =
                        SecurityContextHolder
                            .getContext()
                            .getAuthentication();

                    if (authentication == null) {
                        return Optional.empty();
                    }

                    Object principal =
                        authentication.getPrincipal();

                    if (principal instanceof UserDetailsImpl user) {

                        return Optional.ofNullable(
                            user.getEmail());
                    }

                    return Optional.empty();
                }

                @Override
                public Optional<Long> getCurrentUserId() {

                    return currentUserId();
                }

                @Override
                public boolean isOwner(
                    Long ownerId) {

                    return currentUserId()
                        .map(ownerId::equals)
                        .orElse(false);
                }

                @Override
                public boolean hasRole(
                    String role) {

                    return currentUserHasRole(
                        role);
                }
            };
        }

        @Bean
        QuestionClaimFailureClassifier questionClaimFailureClassifier() {

            return mock(
                QuestionClaimFailureClassifier.class);
        }

        @Bean
        AuditorAware<Long> integrationAuditorAware() {

            return AbstractForumRealtimeIT::currentUserId;
        }

        private static Long requireAssignmentAccess(
            Long taskAssignmentId) {

            Long currentUserId =
                requireCurrentUserId();

            if (!ACCESSIBLE_TASK_ASSIGNMENT_ID
                .equals(taskAssignmentId)) {

                throw new org.springframework.security.access.AccessDeniedException(
                    "TaskAssignment access is restricted");
            }

            return currentUserId;
        }

        private static void requireQuestionAccess(
            QuestionThread question) {

            requireAssignmentAccess(
                question.getTaskAssignmentId());

            Long currentUserId =
                requireCurrentUserId();

            boolean allowed =
                currentUserHasRole("ADMIN")
                    || question
                        .getVisibility() == com.itasocialacademy.oitassist.chat.dao.enums.QuestionVisibility.PUBLIC
                    || currentUserId.equals(
                        question.getAuthorId())
                    || currentUserId.equals(
                        question.getAssignedReviewerId());

            if (!allowed) {
                throw new org.springframework.security.access.AccessDeniedException(
                    "Question access is restricted");
            }
        }

        private static UserDetailsImpl user(
            Long id,
            String email,
            String role) {

            return new UserDetailsImpl(
                id,
                email,
                "unused",
                true,
                true,
                true,
                List.of(
                    new SimpleGrantedAuthority(
                        role)));
        }

        private static String encodeKey(
            int length,
            byte value) {

            byte[] bytes =
                new byte[length];

            Arrays.fill(
                bytes,
                value);

            return Base64.getEncoder()
                .encodeToString(bytes);
        }
    }

    @RestController
    @RequestMapping("/test/realtime")
    static class RollbackProbeController {

        private final RollbackProbeService service;

        RollbackProbeController(
            RollbackProbeService service) {

            this.service = service;
        }

        @PostMapping("/rollback")
        ResponseEntity<Void> rollback() {
            service.publishThenRollback();

            return ResponseEntity
                .noContent()
                .build();
        }
    }

    @Service
    static class RollbackProbeService {

        private final QuestionThreadRepository repository;
        private final QuestionThreadMapper mapper;
        private final ApplicationEventPublisher publisher;

        RollbackProbeService(
            QuestionThreadRepository repository,
            QuestionThreadMapper mapper,
            ApplicationEventPublisher publisher) {

            this.repository = repository;
            this.mapper = mapper;
            this.publisher = publisher;
        }

        @Transactional
        public void publishThenRollback() {
            Instant now =
                Instant.now();

            QuestionThread saved =
                repository.saveAndFlush(
                    QuestionThread.builder()
                        .taskAssignmentId(
                            ACCESSIBLE_TASK_ASSIGNMENT_ID)
                        .authorId(USER_ID)
                        .title("Rollback question")
                        .content(
                            "Must never produce realtime data")
                        .status(NEW)
                        .state(OPEN)
                        .visibility(PRIVATE)
                        .version(0L)
                        .createdAt(now)
                        .updatedAt(now)
                        .build());

            QuestionThreadResponseDTO snapshot =
                mapper.toResponse(saved);

            publisher.publishEvent(
                new QuestionCreatedDomainEvent(
                    snapshot,
                    Instant.now()));

            throw new RollbackProbeException();
        }
    }

    static class RollbackProbeException
        extends RuntimeException {

        RollbackProbeException() {
            super(
                "Forced transaction rollback");
        }
    }

    private static String decodePayload(
        Object payload) {

        if (payload instanceof byte[] bytes) {
            return new String(
                bytes,
                StandardCharsets.UTF_8);
        }

        return String.valueOf(payload);
    }
}