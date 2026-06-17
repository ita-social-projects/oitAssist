package com.itasocialacademy.oitassist.security.oauth2;

import com.itasocialacademy.oitassist.core.properties.WebClientProperties;
import com.itasocialacademy.oitassist.security.api.mapper.UserDetailsMapper;
import com.itasocialacademy.oitassist.security.dao.dto.response.TokenResponse;
import com.itasocialacademy.oitassist.security.jwt.JwtTokenIssuer;
import com.itasocialacademy.oitassist.user.api.dto.OAuthProvisionCommand;
import com.itasocialacademy.oitassist.user.api.dto.UserAuthDetails;
import com.itasocialacademy.oitassist.user.api.interfaces.UserFacade;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Handles successful OAuth2 authentication by issuing the application's own JWT
 * pair and redirecting the client to the frontend callback URL.
 *
 * <p>
 * Flow:
 * </p>
 * <ol>
 * <li>Extract user identity from the OIDC principal via
 * {@link OAuth2UserAttributeExtractor}.</li>
 * <li>Look up or create the user via
 * {@link UserFacade#provisionOAuthUser}.</li>
 * <li>Map to
 * {@link com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl} via
 * {@link UserDetailsMapper}.</li>
 * <li>Issue access and refresh JWTs via {@link JwtTokenIssuer}.</li>
 * <li>Redirect to {@code ${web-client.origin}${web-client.oauth-callback-path}}
 * with tokens in the URL fragment so they never appear in server logs.</li>
 * </ol>
 *
 * <p>
 * The URL fragment ({@code #token=...&refreshToken=...}) is intentional:
 * fragment values are not sent to the server by the browser, so the tokens are
 * not visible in access logs after the frontend reads them.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
    private final OAuth2UserAttributeExtractor extractor;
    private final UserFacade userFacade;
    private final UserDetailsMapper userDetailsMapper;
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

        OAuthProvisionCommand command = extractor.extract(principal);
        UserAuthDetails authDetails = userFacade.provisionOAuthUser(command);
        TokenResponse tokenResponse = jwtTokenIssuer.issueFor(
            userDetailsMapper.toUserDetails(authDetails));

        String redirectUrl = UriComponentsBuilder
            .fromUriString(webClientProperties.origin()
                + webClientProperties.oauthCallbackPath())
            .fragment("token=" + tokenResponse.getToken()
                + "&refreshToken=" + tokenResponse.getRefreshToken())
            .build()
            .toUriString();

        log.debug("Redirecting OAuth2 user to frontend callback email={}",
            authDetails.email());
        response.sendRedirect(redirectUrl);
    }
}
