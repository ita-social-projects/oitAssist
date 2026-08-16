package com.itasocialacademy.oitassist.security.oauth2;

import com.itasocialacademy.oitassist.core.properties.WebClientProperties;
import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import com.itasocialacademy.oitassist.security.api.interfaces.OAuthUserProvisioningPort;
import com.itasocialacademy.oitassist.security.dao.dto.response.TokenResponse;
import com.itasocialacademy.oitassist.security.jwt.JwtTokenIssuer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import java.io.IOException;

/**
 * Handles successful OAuth2 authentication by issuing the application's own JWT
 * pair and redirecting the client to the frontend callback URL.
 *
 * <p>
 * Depends only on {@link OAuthUserProvisioningPort} — not on {@code
 * UserFacade} or any {@code user}-owned DTO — to keep {@code security} free of
 * a compile-time dependency on {@code user}.
 * </p>
 *
 * <p>
 * <b>Why the try/catch:</b> Spring Security's
 * {@code AbstractAuthenticationProcessingFilter} only wraps the authentication
 * <i>attempt</i> in a catch for {@link AuthenticationException} — once
 * authentication is deemed successful, this handler runs with no surrounding
 * exception handling from the framework. Any exception thrown here (e.g.
 * {@code UnverifiedEmailException} from a missing email claim, or an unexpected
 * failure from {@link OAuthUserProvisioningPort}) would otherwise escape as a
 * raw, unhandled 500 instead of the clean redirect-with-error that
 * {@link OAuth2FailureHandler} provides for failures during the authentication
 * attempt itself. Catching here and delegating to the same failure handler
 * keeps both failure paths consistent for the frontend.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final OAuth2UserAttributeExtractor extractor;
    private final OAuthUserProvisioningPort provisioningPort;
    private final JwtTokenIssuer jwtTokenIssuer;
    private final WebClientProperties webClientProperties;
    private final OAuth2FailureHandler failureHandler;

    @Override
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull Authentication authentication) throws IOException {
        try {
            handleSuccess(response, authentication);
        } catch (AuthenticationException ex) {
            log.warn("OAuth2 post-authentication processing rejected: {}", ex.getMessage());
            failureHandler.onAuthenticationFailure(request, response, ex);
        } catch (RuntimeException ex) {
            log.error("Unexpected error during OAuth2 post-authentication processing", ex);
            failureHandler.onAuthenticationFailure(request, response,
                new OAuth2AuthenticationException(
                    new OAuth2Error("provisioning_failed"), "provisioning_failed", ex));
        }
    }

    private void handleSuccess(HttpServletResponse response,
        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User principal = token.getPrincipal();
        String registrationId = token.getAuthorizedClientRegistrationId();

        log.info("OAuth2 authentication success for provider={}", registrationId);

        OidcIdentity identity = extractor.extract(principal);
        UserDetailsImpl userDetails = provisioningPort.provisionOAuthUser(
            identity.email(), identity.firstName(), identity.surname(), null, null);
        TokenResponse tokenResponse = jwtTokenIssuer.issueFor(userDetails);

        String redirectUrl = UriComponentsBuilder
            .fromUriString(webClientProperties.origin() + webClientProperties.oauthCallbackPath())
            .fragment("token=" + tokenResponse.getToken()
                + "&refreshToken=" + tokenResponse.getRefreshToken())
            .build()
            .toUriString();

        log.debug("Redirecting OAuth2 user to frontend callback email={}", identity.email());
        response.sendRedirect(redirectUrl);
    }
}