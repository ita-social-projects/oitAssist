package com.itasocialacademy.oitassist.security.controller;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthenticationException;
import com.itasocialacademy.oitassist.security.dao.dto.request.TwoFactorConfirmRequest;
import com.itasocialacademy.oitassist.security.dao.dto.request.TwoFactorEnrollRequest;
import com.itasocialacademy.oitassist.security.dao.dto.response.TokenResponse;
import com.itasocialacademy.oitassist.security.dao.dto.response.TwoFactorEnrollResponse;
import com.itasocialacademy.oitassist.security.dao.dto.response.TwoFactorRecoveryCodesResponse;
import com.itasocialacademy.oitassist.security.dao.dto.response.TwoFactorStatusResponse;
import com.itasocialacademy.oitassist.security.service.interfaces.SecurityService;
import com.itasocialacademy.oitassist.security.service.interfaces.TokenService;
import com.itasocialacademy.oitassist.security.service.interfaces.TwoFactorService;
import com.itasocialacademy.oitassist.security.twofactor.TwoFactorEnrollingIdentityResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Enrollment, recovery-code, and status endpoints for two-factor
 * authentication.
 *
 * <p>
 * {@code enroll} and {@code enroll/confirm} serve two different callers, and
 * need identity resolved two different ways depending on which one is calling:
 * </p>
 * <ul>
 * <li><b>Voluntary opt-in</b> — an already-logged-in user (a normal Bearer
 * access token was sent, so {@code JwtFilter} already populated the security
 * context) turning 2FA on via account settings.</li>
 * <li><b>Forced setup</b> — a mandatory-role user who just received
 * {@code TWO_FA_SETUP_REQUIRED} from {@code /signIn}. They have no normal
 * session yet — only the {@code pendingTwoFactorToken} from that response.</li>
 * </ul>
 * <p>
 * That session-or-pending-token resolution is delegated to
 * {@link TwoFactorEnrollingIdentityResolver} rather than duplicated here — it's
 * authentication-resolution logic, not an HTTP-translation concern, so it
 * doesn't belong in the controller itself.
 * </p>
 *
 * <p>
 * {@code recovery-codes/regenerate} and {@code status} both always require a
 * normal authenticated session — neither is part of completing a login, so
 * neither accepts a {@code pendingTwoFactorToken}; identity comes straight from
 * {@link SecurityService}.
 * </p>
 *
 * <p>
 * {@code enroll} and {@code enroll/confirm} must be reachable without a Bearer
 * token (see {@code SecurityConfig}'s {@code permitAll} list) to support the
 * forced-setup case. This doesn't weaken the voluntary-opt-in case:
 * {@code JwtFilter} runs on every request regardless of {@code permitAll} and
 * populates the security context whenever a valid Bearer token is actually
 * present — {@code permitAll} only means authentication isn't <i>required</i>
 * to reach the route, not that it's ignored when supplied.
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
    private final TwoFactorEnrollingIdentityResolver identityResolver;
    private final TokenService tokenService;

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
        var identity = identityResolver.resolve(request.getPendingTwoFactorToken());
        return twoFactorService.enroll(identity.userId(), identity.email(), request);
    }

    @Operation(
        summary = "Confirm 2FA enrollment",
        description = "Validates a code produced by the just-configured method. Only on success does 2FA "
            + "actually start being enforced on the account. If confirmed via pendingTwoFactorToken (forced "
            + "setup, no session yet), returns a full access+refresh token pair — completing the login that "
            + "triggered setup, so no second sign-in is required. If confirmed via an existing authenticated "
            + "session (voluntary opt-in), returns no content, since the caller's session was already valid.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Enrollment confirmed via pendingTwoFactorToken; "
                + "2FA is now enabled and a full token pair is returned"),
            @ApiResponse(responseCode = "204", description = "Enrollment confirmed via an existing session; "
                + "2FA is now enabled, no content returned"),
            @ApiResponse(responseCode = "401", description = "Neither an authenticated session nor a valid "
                + "pendingTwoFactorToken was supplied, or the code was invalid"),
            @ApiResponse(responseCode = "404", description = "No enrollment is in progress for this user")
        })
    @PostMapping("/enroll/confirm")
    public ResponseEntity<TokenResponse> confirmEnrollment(@Valid @RequestBody TwoFactorConfirmRequest request) {
        var identity = identityResolver.resolve(request.getPendingTwoFactorToken());
        if (identity.viaPendingToken()) {
            TokenResponse tokens = tokenService.completeTwoFactorSetup(identity.userId(), identity.email(), request);
            return ResponseEntity.ok(tokens);
        }
        twoFactorService.confirmEnrollment(identity.userId(), request);
        return ResponseEntity.noContent().build();
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

    @Operation(
        summary = "Get current 2FA status",
        description = "Reports whether 2FA is enabled and, if so, which method. An unconfirmed "
            + "enrollment in progress is reported the same as no enrollment. Always requires a "
            + "normal authenticated session — never a pendingTwoFactorToken.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Status returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
        })
    @GetMapping("/status")
    public TwoFactorStatusResponse status() {
        Long userId = securityService.getCurrentUserId()
            .orElseThrow(() -> new AuthenticationException(
                "Authentication required", ErrorCode.AUTHENTICATION_REQUIRED));
        return twoFactorService.getStatus(userId);
    }
}