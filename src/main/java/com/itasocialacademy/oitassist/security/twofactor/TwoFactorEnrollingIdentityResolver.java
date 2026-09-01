package com.itasocialacademy.oitassist.security.twofactor;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.security.jwt.JwtTokenIssuer;
import com.itasocialacademy.oitassist.security.service.interfaces.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolves who is enrolling in 2FA, trying the normal authenticated session
 * first and falling back to a pending {@code 2fa_setup} token. Session takes
 * priority: an already-logged-in user opting in voluntarily should never need
 * to carry a pending token around.
 */
@Component
@RequiredArgsConstructor
public class TwoFactorEnrollingIdentityResolver {
    private final SecurityService securityService;
    private final JwtTokenIssuer jwtTokenIssuer;

    public EnrollingIdentity resolve(String pendingTwoFactorToken) {
        var currentUserId = securityService.getCurrentUserId();
        if (currentUserId.isPresent()) {
            String email = securityService.getCurrentUserEmail()
                .orElseThrow(() -> new AuthenticationException(
                    "Authentication required", ErrorCode.AUTHENTICATION_REQUIRED));
            return new EnrollingIdentity(currentUserId.get(), email);
        }

        if (pendingTwoFactorToken == null || pendingTwoFactorToken.isBlank()) {
            throw new AuthenticationException("Authentication required", ErrorCode.AUTHENTICATION_REQUIRED);
        }

        JwtTokenIssuer.PendingTwoFactorClaims claims = jwtTokenIssuer.readPendingTwoFactorToken(
            pendingTwoFactorToken, JwtTokenIssuer.PURPOSE_TWO_FACTOR_SETUP);

        return new EnrollingIdentity(claims.userId(), claims.email());
    }

    public record EnrollingIdentity(Long userId, String email) {
    }
}