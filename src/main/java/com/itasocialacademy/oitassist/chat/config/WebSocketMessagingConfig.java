package com.itasocialacademy.oitassist.chat.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.SimpleBrokerRegistration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketMessagingConfig implements WebSocketMessageBrokerConfigurer {
    public static final String WEBSOCKET_ENDPOINT = "/ws";
    public static final String APPLICATION_PREFIX = "/app";
    public static final String USER_PREFIX = "/user";
    public static final String TOPIC_PREFIX = "/topic";
    public static final String QUEUE_PREFIX = "/queue";

    private static final int SCHEDULER_POOL_SIZE = 1;

    private final RealtimeMessagingProperties properties;

    private final StompAuthenticationChannelInterceptor stompAuthenticationChannelInterceptor;

    private final StompAuthorizationChannelInterceptor stompAuthorizationChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(WEBSOCKET_ENDPOINT)
            .setAllowedOrigins(
                properties.allowedOrigins()
                    .toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes(APPLICATION_PREFIX);
        registry.setUserDestinationPrefix(USER_PREFIX);

        SimpleBrokerRegistration brokerRegistration =
            registry.enableSimpleBroker(
                TOPIC_PREFIX,
                QUEUE_PREFIX);

        brokerRegistration.setTaskScheduler(realtimeMessageBrokerTaskScheduler());

        brokerRegistration.setHeartbeatValue(
            new long[] {
                properties.serverHeartbeat(),
                properties.clientHeartbeat()
            });
    }

    @Bean
    public ThreadPoolTaskScheduler realtimeMessageBrokerTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(SCHEDULER_POOL_SIZE);
        scheduler.setThreadNamePrefix("realtime-message-broker-");
        scheduler.setRemoveOnCancelPolicy(true);

        return scheduler;
    }

    @Override
    public void configureClientInboundChannel(
        ChannelRegistration registration) {
        registration.interceptors(
            stompAuthenticationChannelInterceptor,
            new SecurityContextChannelInterceptor(),
            stompAuthorizationChannelInterceptor);
    }
}
