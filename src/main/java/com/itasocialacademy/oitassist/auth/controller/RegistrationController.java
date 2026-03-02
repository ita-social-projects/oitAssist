package com.itasocialacademy.oitassist.auth.controller;

import com.itasocialacademy.oitassist.auth.dao.dto.request.RegisterRequest;
import com.itasocialacademy.oitassist.auth.service.interfaces.RegistrationService;
import com.itasocialacademy.oitassist.core.web.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller responsible for user registration operations. Provides API
 * endpoints for creating new users in the system. All incoming requests are
 * validated before being processed.
 */
@RestController
@Tag(name = "Registration API")
@RequestMapping("/api/v1/registration")
@RequiredArgsConstructor
public class RegistrationController {
    /**
     * Service responsible for handling registration business logic.
     */
    private final RegistrationService registrationService;

    /**
     * Creates a new user account. Accepts user registration data, validates it, and
     * delegates user creation to the {@link RegistrationService}.
     *
     * @param request the user registration request containing personal data and
     *                credentials
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new user account",
        description = "Creates a new user account with the provided registration data.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User created"),
        @ApiResponse(
            responseCode = "400",
            description = "Validation error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "409",
            description = "User already exists or account is not yet activated",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void createUser(@RequestBody @Valid RegisterRequest request) {
        registrationService.createUser(request);
    }
}
