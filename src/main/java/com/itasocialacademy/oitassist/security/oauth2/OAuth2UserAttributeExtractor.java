package com.itasocialacademy.oitassist.security.oauth2;

import com.itasocialacademy.oitassist.security.exceptions.UnverifiedEmailException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

/**
 * Extracts user identity attributes from an OIDC-authenticated principal and
 * converts them into an {@link OAuthProvisionCommand}.
 *
 * <p>
 * Both supported providers (Google, Microsoft Entra ID) are full OIDC
 * providers. Spring Security resolves the authenticated principal as an
 * {@link OidcUser}, which exposes typed accessors for standard claims — no
 * per-provider claim-name mapping is needed.
 * </p>
 *
 * <p>
 * Email verification contract: if the provider explicitly sets
 * {@code email_verified} to {@code false}, the login is rejected. A
 * {@code null} value (claim absent) is treated as implicitly verified — the
 * OIDC flow itself is the verification guarantee for providers like Microsoft
 * Entra ID that may omit the claim.
 * </p>
 */
@Slf4j
@Component
public class OAuth2UserAttributeExtractor {
    /**
     * Extracts identity attributes from the given principal.
     *
     * @param principal the authenticated principal from Spring Security
     * @return identity attributes ready to pass to
     *         {@code OAuthUserProvisioningPort.provisionOAuthUser}
     * @throws UnverifiedEmailException if {@code email_verified} is explicitly
     *                                  {@code false}
     * @throws IllegalArgumentException if the principal is not an {@link OidcUser}
     *                                  — indicates a misconfigured non-OIDC
     *                                  provider
     */
    OidcIdentity extract(OAuth2User principal) {
        if (!(principal instanceof OidcUser oidcUser)) {
            throw new IllegalArgumentException(
                "Expected OidcUser but received " + principal.getClass().getSimpleName()
                    + " — only OIDC providers are supported");
        }

        if (Boolean.FALSE.equals(oidcUser.getEmailVerified())) {
            log.warn("OAuth2 login rejected — email_verified is explicitly false");
            throw new UnverifiedEmailException();
        }

        String email = oidcUser.getEmail();
        return new OidcIdentity(email, resolveFirstName(oidcUser, email), oidcUser.getFamilyName());
    }

    private String resolveFirstName(OidcUser oidcUser, String email) {
        String givenName = oidcUser.getGivenName();
        if (givenName == null || givenName.isBlank()) {
            String fallback = email.split("@")[0];
            log.debug("Given name claim absent; falling back to email local-part '{}'", fallback);
            return fallback;
        }
        return givenName;
    }
}
