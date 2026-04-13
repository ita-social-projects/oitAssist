package com.itasocialacademy.oitassist.auth.controller;

import com.itasocialacademy.oitassist.auth.dto.request.ResendVerificationMailRequest;
import com.itasocialacademy.oitassist.auth.exceptions.InvalidActivationTokenException;
import com.itasocialacademy.oitassist.user.exceptions.ActivationTokenSendingTimeoutException;
import com.itasocialacademy.oitassist.auth.service.interfaces.UserActivationService;
import com.itasocialacademy.oitassist.core.web.ErrorResponse;
import com.itasocialacademy.oitassist.auth.exceptions.UserAlreadyActivatedException;
import com.itasocialacademy.oitassist.user.exceptions.UserNotFoundException;
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
 * REST controller responsible for user account activation workflows. Provides
 * endpoints for resending activation emails for users who have not completed
 * account verification.
 */
@RestController
@Tag(name = "User activation API")
@RequestMapping("/api/v1/user-activation")
@RequiredArgsConstructor
public class UserActivationController {
    private final UserActivationService userActivationService;

    /**
     * Activates a user account by activation token.
     *
     * @param token activation token sent by email
     * @throws InvalidActivationTokenException if the token is invalid or expired
     * @throws UserAlreadyActivatedException   if the account is already activated
     */
    @GetMapping("/verify")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Verify user email",
        description = "Activates a user account using the activation token from the email link.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User account successfully activated"),
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
        userActivationService.verifyEmail(token);
    }

    /**
     * Resends an activation email to a user whose account is not yet activated.
     *
     * @param request request containing the user's email address
     *
     * @throws UserNotFoundException                  if no user exists with the
     *                                                provided email
     * @throws UserAlreadyActivatedException          if the account is already
     *                                                activated
     * @throws ActivationTokenSendingTimeoutException if resend request is made
     *                                                before timeout expires
     */
    @PostMapping("/resend")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
        summary = "Resend activation email",
        description = "Resends the account activation email to a user "
            + "whose account is not yet activated. "
            + "If the activation token has expired, a new one will be generated.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Activation email successfully resent"),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload",
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
            description = "User already activated or resend timeout has not expired",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void resendVerificationEmail(@RequestBody @Valid ResendVerificationMailRequest request) {
        userActivationService.resendVerificationEmail(request.getEmail());
    }
}
