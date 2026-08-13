package com.itasocialacademy.oitassist.chat.config;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import java.util.List;
import com.itasocialacademy.oitassist.security.api.interfaces.StompAuthenticationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthenticationChannelInterceptor
    implements ChannelInterceptor {
    private static final String BEARER_PREFIX = "Bearer ";

    private static final String INVALID_AUTHENTICATION =
        "Invalid STOMP authentication";

    private final StompAuthenticationFacade stompAuthenticationFacade;

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

        if (command == StompCommand.CONNECT
            || command == StompCommand.STOMP) {
            authenticate(accessor);
        }

        return message;
    }

    private void authenticate(
        StompHeaderAccessor accessor) {
        List<String> authorizationHeaders =
            accessor.getNativeHeader(AUTHORIZATION);

        if (authorizationHeaders == null
            || authorizationHeaders.size() != 1) {
            throw invalidAuthentication();
        }

        String authorizationHeader =
            authorizationHeaders.get(0);

        if (authorizationHeader == null
            || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw invalidAuthentication();
        }

        String accessToken = authorizationHeader
            .substring(BEARER_PREFIX.length());

        if (accessToken.isBlank()
            || !accessToken.equals(accessToken.strip())) {
            throw invalidAuthentication();
        }

        Authentication authentication =
            stompAuthenticationFacade
                .authenticateAccessToken(accessToken);

        accessor.setUser(authentication);
    }

    private static BadCredentialsException invalidAuthentication() {
        return new BadCredentialsException(
            INVALID_AUTHENTICATION);
    }
}
