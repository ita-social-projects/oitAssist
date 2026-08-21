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

    /**
     * Claim distinguishing what a {@link JwtHelper#TWO_FACTOR_PENDING_TOKEN} grants
     * access to: either submitting a verification code
     * ({@link #PURPOSE_TWO_FACTOR_VERIFY}) or completing enrollment
     * ({@link #PURPOSE_TWO_FACTOR_SETUP}). The two purposes are not interchangeable
     * — a setup-purpose token must not be usable at the verify endpoint and vice
     * versa, since a user with no enrollment yet has nothing to verify against.
     */
    static final String CLAIM_PURPOSE = "purpose";
    public static final String PURPOSE_TWO_FACTOR_VERIFY = "2fa_verify";
    public static final String PURPOSE_TWO_FACTOR_SETUP = "2fa_setup";

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

    /**
     * Issues a short-lived pending-2FA token, scoped to exactly one purpose.
     *
     * <p>
     * Deliberately omits the {@code role} claim carried by access tokens: this
     * token grants no general API access (see {@link JwtHelper#extractUsername},
     * which every protected route relies on and which only accepts
     * {@link JwtHelper#ACCESS_TOKEN} — a {@code 2fa_pending} token is rejected
     * there automatically without any change to the existing filter chain).
     * </p>
     *
     * @param userDetails    the principal who just passed password verification
     * @param purpose        either {@link #PURPOSE_TWO_FACTOR_VERIFY} or
     *                       {@link #PURPOSE_TWO_FACTOR_SETUP}
     * @param validityMillis how long the token remains valid, in milliseconds
     * @return the signed+encrypted pending token
     */
    public String issuePendingTwoFactorToken(UserDetailsImpl userDetails, String purpose, long validityMillis) {
        Map<String, Object> claims = Map.of(
            CLAIM_ID, Objects.requireNonNull(userDetails.getId(), "user id must not be null"),
            CLAIM_TOKEN_TYPE, JwtHelper.TWO_FACTOR_PENDING_TOKEN,
            CLAIM_PURPOSE, purpose);
        return jwtHelper.createTwoFactorPendingToken(claims, userDetails.getUsername(), validityMillis);
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