package com.itasocialacademy.oitassist.auth.controller;

import com.itasocialacademy.oitassist.auth.dto.request.ResendVerificationMailRequest;
import com.itasocialacademy.oitassist.core.web.ErrorResponse;
import com.itasocialacademy.oitassist.user.api.interfaces.UserFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller responsible for user account activation workflows. Delegates
 * all use-case logic to {@link UserFacade} — the only sanctioned entry point
 * into the {@code user} module's activation lifecycle.
 *
 * <p>
 * This controller is an HTTP adapter: it validates input, extracts parameters,
 * and calls the facade. It has no knowledge of tokens, repositories, or any
 * internal {@code user} concern.
 * </p>
 */
@RestController
@Tag(name = "User activation API")
@RequestMapping("/api/v1/user-activation")
@RequiredArgsConstructor
public class UserActivationController {
    private final UserFacade userFacade;

    /**
     * Activates a user account using the token from the verification email.
     *
     * @param token the activation token
     */
    @GetMapping("/verify")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Verify user email",
        description = "Activates a user account using the activation token from the email link.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User account successfully activated"),
        @ApiResponse(
            responseCode = "400",
            description = "Activation token is invalid or expired",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "409",
            description = "User account is already activated",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void verifyEmail(
        @Parameter(description = "Activation token from email link",
            required = true) @RequestParam @NotBlank String token) {
        userFacade.activate(token);
    }

    /**
     * Resends the activation email to a user whose account is still pending.
     *
     * @param request contains the user's email address
     */
    @PostMapping("/resend")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Resend activation email",
        description = "Resends the account activation email to a user whose account "
            + "is not yet activated. If the activation token has expired, "
            + "a new one will be generated.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Activation email successfully resent"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload or resend timeout not elapsed",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "409",
            description = "User already activated",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void resendVerificationEmail(@RequestBody @Valid ResendVerificationMailRequest request) {
        userFacade.resendActivation(request.getEmail());
    }
}
