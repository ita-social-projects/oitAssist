package com.itasocialacademy.oitassist.security.api.interfaces;

import com.itasocialacademy.oitassist.security.api.dto.UserDetailsImpl;
import org.springframework.modulith.NamedInterface;

/**
 * Port through which {@code security} triggers OAuth2 user provisioning without
 * depending on any {@code user}-owned type.
 *
 * <p>
 * Implemented by the {@code user} module — the same Dependency Inversion
 * pattern already used by {@link SecurityUserProvider}. The signature uses only
 * primitives and the {@code security}-owned {@link UserDetailsImpl}, so
 * implementing this in {@code user} does not create a {@code security ->
 * user} dependency, which would otherwise cycle with the existing, load-bearing
 * {@code user -> security} dependency (via {@link SecurityUserProvider},
 * {@link SecurityFacade}, and {@link UserDetailsImpl} itself).
 * </p>
 */
@NamedInterface("OAuthUserProvisioningPort")
public interface OAuthUserProvisioningPort {
    /**
     * Looks up or provisions a user from a verified OAuth2 / OIDC identity and
     * returns the security-side representation, ready for JWT issuance.
     *
     * @param email       the verified email address from the provider
     * @param firstName   the resolved first name; never blank
     * @param surname     the resolved surname, or {@code null}/blank if the
     *                    provider did not supply one
     * @param middleName  always {@code null} for OAuth2 — no supported provider
     *                    exposes this claim
     * @param phoneNumber always {@code null} for OAuth2 — no supported provider
     *                    exposes this claim
     * @return the security-side user representation
     */
    UserDetailsImpl provisionOAuthUser(String email, String firstName, String surname,
        String middleName, String phoneNumber);
}