package com.itasocialacademy.oitassist.security.jwt;

import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import com.itasocialacademy.oitassist.security.dao.dto.response.TokenResponse;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Builds the application-specific access + refresh JWT pair for an
 * authenticated principal.
 *
 * <p>
 * Encapsulates the claim contract used across the application:
 * <ul>
 * <li>{@code id} — the user's database identifier (access token only).</li>
 * <li>{@code role} — the role name without the {@code ROLE_} prefix (access
 * token only).</li>
 * <li>{@code token_type} — distinguishes {@code access} from {@code refresh} on
 * every token; enforced when the token is later parsed by
 * {@link JwtHelper#extractUsername}.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Decoupled from any specific authentication source. The same issuer is used by
 * the password sign-in flow ({@code TokenServiceImpl}) and the OAuth2 social
 * login flow ({@code OAuth2SuccessHandler}). Callers supply a
 * {@link UserDetailsImpl}; the issuer produces a {@link TokenResponse}.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class JwtTokenIssuer {
    static final String CLAIM_ID = "id";
    static final String CLAIM_ROLE = "role";
    static final String CLAIM_TOKEN_TYPE = "token_type";
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtHelper jwtHelper;

    /**
     * Issues a fresh access + refresh JWT pair for the given principal.
     *
     * @param userDetails the authenticated principal; must carry a non-null id
     * @return a {@link TokenResponse} with both tokens, ready to return to the
     *         client
     */
    public TokenResponse issueFor(UserDetailsImpl userDetails) {
        String subject = userDetails.getUsername();
        String accessToken = jwtHelper.createToken(buildAccessClaims(userDetails), subject);
        String refreshToken = jwtHelper.createRefreshToken(buildRefreshClaims(), subject);

        return TokenResponse.builder()
            .token(accessToken)
            .refreshToken(refreshToken)
            .build();
    }

    private Map<String, Object> buildAccessClaims(UserDetailsImpl userDetails) {
        return Map.of(
            CLAIM_ID, Objects.requireNonNull(userDetails.getId(), "user id must not be null"),
            CLAIM_ROLE, extractRole(userDetails),
            CLAIM_TOKEN_TYPE, JwtHelper.ACCESS_TOKEN);
    }

    private Map<String, Object> buildRefreshClaims() {
        return Map.of(CLAIM_TOKEN_TYPE, JwtHelper.REFRESH_TOKEN);
    }

    private String extractRole(UserDetailsImpl userDetails) {
        return userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(Objects::nonNull)
            .filter(authority -> authority.startsWith(ROLE_PREFIX))
            .map(authority -> authority.substring(ROLE_PREFIX.length()))
            .findFirst()
            .orElse("");
    }
}
