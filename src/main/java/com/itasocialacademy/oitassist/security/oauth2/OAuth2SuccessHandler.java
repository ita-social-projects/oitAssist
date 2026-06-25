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
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
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
 * a compile-time dependency on {@code user}. See
 * {@code OAuthUserProvisioningPort} for the full rationale.
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

    @Override
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request,
        HttpServletResponse response,
        @NonNull Authentication authentication) throws IOException {
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