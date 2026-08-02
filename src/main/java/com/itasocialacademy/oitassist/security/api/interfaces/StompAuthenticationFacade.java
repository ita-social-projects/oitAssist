package com.itasocialacademy.oitassist.security.api.interfaces;

import org.springframework.modulith.NamedInterface;
import org.springframework.security.core.Authentication;

/**
 * Authenticates STOMP sessions using application access tokens.
 */
@NamedInterface("StompAuthenticationFacade")
public interface StompAuthenticationFacade {
    /**
     * Validates an application access token and creates an authenticated STOMP
     * principal.
     *
     * <p>
     * The returned authentication uses the user identifier as its principal name so
     * that personal destinations can be addressed through {@code /user/queue/**}.
     * </p>
     *
     * @param accessToken raw JWT without the Bearer prefix
     * @return authenticated STOMP principal
     */
    Authentication authenticateAccessToken(String accessToken);
}