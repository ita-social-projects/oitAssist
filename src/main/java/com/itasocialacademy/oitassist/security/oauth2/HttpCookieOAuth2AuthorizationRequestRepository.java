package com.itasocialacademy.oitassist.security.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Stores the {@link OAuth2AuthorizationRequest} (including the {@code state}
 * parameter) in a short-lived {@code HttpOnly} cookie instead of the HTTP
 * session, keeping the application fully stateless.
 *
 * <p>
 * <b>Serialization:</b> the cookie value is a small JSON snapshot of only the
 * fields needed to rebuild the request, encoded via the application's existing
 * {@link ObjectMapper} bean — deliberately NOT Java's native
 * {@code ObjectOutputStream}/{@code ObjectInputStream}. A cookie value is fully
 * attacker-controlled (any client can send arbitrary bytes as a cookie), and
 * deserializing untrusted bytes with native Java serialization is a known
 * remote-code-execution vector via classpath "gadget chains". Deserializing
 * into a fixed, non-polymorphic record with Jackson carries no such risk — only
 * the declared fields are populated, no arbitrary constructors or
 * {@code readObject} methods run.
 * </p>
 *
 * <p>
 * The cookie is:
 * </p>
 * <ul>
 * <li>{@code HttpOnly} — not readable by JavaScript.</li>
 * <li>{@code Secure} whenever the incoming request itself arrived over HTTPS —
 * stays unset only for local HTTP development.</li>
 * <li>{@code SameSite=Lax} — sent on top-level navigations (the OAuth2
 * redirect) but not on cross-site sub-resource requests.</li>
 * <li>Max-age 3 minutes — long enough for any realistic OAuth2 dance; short
 * enough to limit exposure if the flow is abandoned.</li>
 * </ul>
 *
 * <p>
 * <b>Production note:</b> behind a reverse proxy/load balancer that terminates
 * TLS, {@code request.isSecure()} only reflects HTTPS correctly if Spring Boot
 * trusts {@code X-Forwarded-*} headers. Set
 * {@code server.forward-headers-strategy=framework} (or {@code native}) so the
 * {@code Secure} flag is applied correctly in that deployment.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpCookieOAuth2AuthorizationRequestRepository
    implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    static final String COOKIE_NAME = "oauth2_auth_request";
    private static final int COOKIE_MAX_AGE_SECONDS = 180;

    private final ObjectMapper objectMapper;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return findCookie(request)
            .map(Cookie::getValue)
            .map(this::deserialize)
            .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
        HttpServletRequest request,
        HttpServletResponse response) {
        if (authorizationRequest == null) {
            deleteCookie(request, response);
            return;
        }
        addCookie(request, response, serialize(authorizationRequest), COOKIE_MAX_AGE_SECONDS);
        log.debug("OAuth2 authorization request saved to cookie");
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
        HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        deleteCookie(request, response);
        return authorizationRequest;
    }

    private void deleteCookie(HttpServletRequest request, HttpServletResponse response) {
        addCookie(request, response, "", 0);
    }

    private void addCookie(HttpServletRequest request, HttpServletResponse response,
        String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(COOKIE_NAME, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setMaxAge(maxAgeSeconds);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private Optional<Cookie> findCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
            .filter(c -> COOKIE_NAME.equals(c.getName()))
            .findFirst();
    }

    private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        AuthorizationRequestSnapshot snapshot = new AuthorizationRequestSnapshot(
            authorizationRequest.getAuthorizationUri(),
            authorizationRequest.getClientId(),
            authorizationRequest.getRedirectUri(),
            authorizationRequest.getScopes(),
            authorizationRequest.getState(),
            stringifyValues(authorizationRequest.getAdditionalParameters()),
            stringifyValues(authorizationRequest.getAttributes()));

        String json = objectMapper.writeValueAsString(snapshot);
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private OAuth2AuthorizationRequest deserialize(String cookieValue) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(cookieValue);
            String json = new String(decoded, StandardCharsets.UTF_8);
            AuthorizationRequestSnapshot snapshot =
                objectMapper.readValue(json, AuthorizationRequestSnapshot.class);

            return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri(snapshot.authorizationUri())
                .clientId(snapshot.clientId())
                .redirectUri(snapshot.redirectUri())
                .scopes(snapshot.scopes())
                .state(snapshot.state())
                .additionalParameters(new HashMap<>(snapshot.additionalParameters()))
                .attributes(new HashMap<>(snapshot.attributes()))
                .build();
        } catch (RuntimeException e) {
            log.warn("Failed to deserialize OAuth2 authorization request cookie — "
                + "user may need to restart the login flow");
            return null;
        }
    }

    private Map<String, String> stringifyValues(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return source.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));
    }

    /**
     * Minimal, non-polymorphic snapshot of only the fields needed to rebuild an
     * {@link OAuth2AuthorizationRequest}. Exists purely as a JSON serialization
     * target — see the class-level Javadoc for why this replaces Java native
     * serialization.
     */
    private record AuthorizationRequestSnapshot(
        String authorizationUri,
        String clientId,
        String redirectUri,
        Set<String> scopes,
        String state,
        Map<String, String> additionalParameters,
        Map<String, String> attributes) {
    }
}