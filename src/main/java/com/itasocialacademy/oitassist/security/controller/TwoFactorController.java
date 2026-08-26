package com.itasocialacademy.oitassist.security.controller;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.security.dao.dto.request.TwoFactorConfirmRequest;
import com.itasocialacademy.oitassist.security.dao.dto.request.TwoFactorEnrollRequest;
import com.itasocialacademy.oitassist.security.dao.dto.response.TwoFactorEnrollResponse;
import com.itasocialacademy.oitassist.security.dao.dto.response.TwoFactorRecoveryCodesResponse;
import com.itasocialacademy.oitassist.security.jwt.JwtTokenIssuer;
import com.itasocialacademy.oitassist.security.service.interfaces.SecurityService;
import com.itasocialacademy.oitassist.security.service.interfaces.TwoFactorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Enrollment endpoints for two-factor authentication.
 *
 * <p>
 * Both endpoints here serve two different callers, and deliberately resolve
 * identity two different ways depending on which one is calling:
 * </p>
 * <ul>
 * <li><b>Voluntary opt-in</b> — an already-logged-in user (a normal Bearer
 * access token was sent, so {@code JwtFilter} already populated the security
 * context) turning 2FA on via account settings. Identity comes from
 * {@link SecurityService}.</li>
 * <li><b>Forced setup</b> — a mandatory-role user who just received
 * {@code TWO_FA_SETUP_REQUIRED} from {@code /signIn}. They have no normal
 * session yet — only the {@code pendingTwoFactorToken} from that response.
 * Identity comes from that token instead.</li>
 * </ul>
 *
 * <p>
 * Both endpoints must be reachable without a Bearer token (see
 * {@code SecurityConfig}'s {@code permitAll} list) to support the forced-setup
 * case. This doesn't weaken the voluntary-opt-in case: {@code JwtFilter} runs
 * on every request regardless of {@code permitAll} and populates the security
 * context whenever a valid Bearer token is actually present — {@code permitAll}
 * only means authentication isn't <i>required</i> to reach the route, not that
 * it's ignored when supplied.
 * </p>
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/security/2fa")
@RequiredArgsConstructor
@Tag(name = "Two-Factor Authentication v1", description = "Enrollment for two-factor authentication")
public class TwoFactorController {
    private final TwoFactorService twoFactorService;
    private final SecurityService securityService;
    private final JwtTokenIssuer jwtTokenIssuer;

    @Operation(
        summary = "Start 2FA enrollment",
        description = "Generates a TOTP secret + QR provisioning URI, or triggers an email-OTP send, plus ten "
            + "recovery codes shown once. Call as an already-authenticated user to opt in voluntarily, or with "
            + "pendingTwoFactorToken set (from a TWO_FA_SETUP_REQUIRED login) to complete forced setup.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Enrollment started; nothing is enforced yet "
                + "until /2fa/enroll/confirm succeeds"),
            @ApiResponse(responseCode = "401", description = "Neither an authenticated session nor a valid "
                + "pendingTwoFactorToken was supplied"),
            @ApiResponse(responseCode = "409", description = "2FA is already enabled for this account")
        })
    @PostMapping("/enroll")
    public TwoFactorEnrollResponse enroll(@Valid @RequestBody TwoFactorEnrollRequest request) {
        EnrollingIdentity identity = resolveEnrollingIdentity(request.getPendingTwoFactorToken());
        return twoFactorService.enroll(identity.userId(), identity.email(), request);
    }

    @Operation(
        summary = "Confirm 2FA enrollment",
        description = "Validates a code produced by the just-configured method. Only on success does 2FA "
            + "actually start being enforced on the account.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Enrollment confirmed; 2FA is now enabled"),
            @ApiResponse(responseCode = "401", description = "Neither an authenticated session nor a valid "
                + "pendingTwoFactorToken was supplied, or the code was invalid"),
            @ApiResponse(responseCode = "404", description = "No enrollment is in progress for this user")
        })
    @PostMapping("/enroll/confirm")
    public void confirmEnrollment(@Valid @RequestBody TwoFactorConfirmRequest request) {
        EnrollingIdentity identity = resolveEnrollingIdentity(request.getPendingTwoFactorToken());
        twoFactorService.confirmEnrollment(identity.userId(), request);
    }

    @Operation(
        summary = "Regenerate recovery codes",
        description = "Generates a fresh batch of ten recovery codes, invalidating every previously unused "
            + "one. An account-settings action — always requires a normal authenticated session, never a "
            + "pendingTwoFactorToken, since this isn't part of completing a login.",
        responses = {
            @ApiResponse(responseCode = "200", description = "New codes generated"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "404", description = "No enabled 2FA setup found for this user")
        })
    @PostMapping("/recovery-codes/regenerate")
    public TwoFactorRecoveryCodesResponse regenerateRecoveryCodes() {
        Long userId = securityService.getCurrentUserId()
            .orElseThrow(() -> new AuthenticationException(
                "Authentication required", ErrorCode.AUTHENTICATION_REQUIRED));
        List<String> codes = twoFactorService.regenerateRecoveryCodes(userId);
        return TwoFactorRecoveryCodesResponse.builder().recoveryCodes(codes).build();
    }

    /**
     * Resolves who's enrolling, trying the normal authenticated session first and
     * falling back to a pending {@code 2fa_setup} token. This order matters: an
     * already-logged-in user opting in voluntarily should never need to carry a
     * pending token around, so the normal session takes priority whenever one is
     * actually present.
     */
    private EnrollingIdentity resolveEnrollingIdentity(String pendingTwoFactorToken) {
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

        JwtTokenIssuer.PendingTwoFactorClaims claims = jwtTokenIssuer.readPendingTwoFactorToken(pendingTwoFactorToken);
        if (!JwtTokenIssuer.PURPOSE_TWO_FACTOR_SETUP.equals(claims.purpose())) {
            throw new AuthenticationException("Invalid token type", ErrorCode.INVALID_TOKEN_TYPE);
        }

        return new EnrollingIdentity(claims.userId(), claims.email());
    }

    private record EnrollingIdentity(Long userId, String email) {
    }
}