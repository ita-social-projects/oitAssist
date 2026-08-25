package com.itasocialacademy.oitassist.security.controller;

import com.itasocialacademy.oitassist.security.dao.dto.request.RefreshTokenRequest;
import com.itasocialacademy.oitassist.security.dao.dto.request.TokenRequest;
import com.itasocialacademy.oitassist.security.dao.dto.response.LoginResponse;
import com.itasocialacademy.oitassist.security.dao.dto.response.TokenResponse;
import com.itasocialacademy.oitassist.security.service.interfaces.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/security")
@Tag(name = "Security v1", description = "Operations related to security and token management")
public class SecurityController {
    private final TokenService tokenService;

    public SecurityController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/signIn")
    @Operation(
        summary = "Authenticate and obtain either full tokens or a 2FA challenge",
        description = "Validates credentials and returns one of three outcomes (see LoginResponse.outcome): "
            + "SUCCESS returns a full access+refresh token pair; TWO_FA_VERIFICATION_REQUIRED returns a "
            + "short-lived pendingTwoFactorToken to submit to /2fa/verify along with a code; "
            + "TWO_FA_SETUP_REQUIRED returns a pendingTwoFactorToken to submit to /2fa/enroll — the account's "
            + "role requires 2FA and none is configured yet.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Login processed; check outcome to see which of "
                + "the three cases applies"),
            @ApiResponse(responseCode = "400", description = "Invalid credentials")
        })
    public LoginResponse createToken(@RequestBody TokenRequest tokenRequest) {
        return tokenService.generateToken(tokenRequest);
    }

    @Operation(
        summary = "Generate new JWT access and refresh token",
        description = "Creates a new JWT access and refresh tokens based on send refresh token. Never triggers "
            + "a 2FA challenge — a refresh token represents an already-verified session.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Token created successfully"),
            @ApiResponse(responseCode = "403", description = "Invalid token")
        })
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return tokenService.refreshToken(request.getToken());
    }

    @GetMapping("/api")
    @Operation(
        summary = "Public API test",
        description = "Requires valid JWT token",
        security = @SecurityRequirement(name = "Bearer Authentication"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Request successful")
        })
    public void api() {
        log.info("/api/v1/security/api placeholder");
    }

    @GetMapping("/api/get")
    @Operation(
        summary = "Authenticated GET test",
        description = "Requires valid JWT token",
        security = @SecurityRequirement(name = "Bearer Authentication"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Request successful"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
        })
    public void apiGet() {
        log.info("/api/v1/security/api/get placeholder Token work");
    }

    @GetMapping("/api/get/get")
    @Operation(
        summary = "Nested authenticated GET test",
        description = "Requires valid JWT token for testing nested path",
        security = @SecurityRequirement(name = "Bearer Authentication"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Request successful"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
        })
    public void apiGetGet() {
        log.info("/api/v1/security/api/get/get placeholder Token work");
    }

    @GetMapping("/api/admin")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(
        summary = "Admin-only endpoint",
        description = "Accessible only by users with ROLE_ADMIN",
        security = @SecurityRequirement(name = "Bearer Authentication"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Request successful"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - user not admin")
        })
    public void apiAdmin() {
        log.info("/api/v1/security/api/admin placeholder Token work");
    }

    @GetMapping("/api/user")
    @PreAuthorize("hasRole('ROLE_USER')")
    @Operation(
        summary = "User-only endpoint",
        description = "Accessible only by users with ROLE_USER",
        security = @SecurityRequirement(name = "Bearer Authentication"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Request successful"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - user not authorized")
        })
    public void apiUser() {
        log.info("/api/v1/security/api/user placeholder Token work");
    }
}