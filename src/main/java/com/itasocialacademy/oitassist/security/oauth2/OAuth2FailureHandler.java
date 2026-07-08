package com.itasocialacademy.oitassist.security.oauth2;

import com.itasocialacademy.oitassist.core.properties.WebClientProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Handles failed OAuth2 authentication by redirecting the client to the
 * frontend callback URL with an {@code error} query parameter describing the
 * failure reason.
 *
 * <p>
 * Error details travel as a query parameter rather than a fragment because they
 * are not sensitive — unlike tokens, which use the fragment to stay out of
 * server logs.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2FailureHandler implements AuthenticationFailureHandler {
    private final WebClientProperties webClientProperties;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception) throws IOException {
        log.warn("OAuth2 authentication failed: {}", exception.getMessage());

        String errorMessage = URLEncoder.encode(
            exception.getMessage() != null ? exception.getMessage() : "authentication_failed",
            StandardCharsets.UTF_8);

        String redirectUrl = UriComponentsBuilder
            .fromUriString(webClientProperties.origin()
                + webClientProperties.oauthCallbackPath())
            .queryParam("error", errorMessage)
            .build()
            .toUriString();

        response.sendRedirect(redirectUrl);
    }
}
