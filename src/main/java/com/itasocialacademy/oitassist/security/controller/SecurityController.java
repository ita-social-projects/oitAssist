package com.itasocialacademy.oitassist.security.controller;

import com.itasocialacademy.oitassist.security.dao.dto.request.RefreshTokenRequest;
import com.itasocialacademy.oitassist.security.service.interfaces.TokenService;
import com.itasocialacademy.oitassist.security.dao.dto.request.TokenRequest;
import com.itasocialacademy.oitassist.security.dao.dto.response.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Security v1", description = "Operations related to security and token management")
public class SecurityController {
    private final TokenService tokenService;

    public SecurityController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/signIn")
    @Operation(
        summary = "Generate JWT access and refresh tokens",
        description = "Creates a JWT token based on provided credentials",
        responses = {
            @ApiResponse(responseCode = "200", description = "Token created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid credentials")
        })
    public TokenResponse createToken(@RequestBody TokenRequest tokenRequest) {
        return tokenService.generateToken(tokenRequest);
    }

    @Operation(
        summary = "Generate new JWT access and refresh token",
        description = "Creates a new JWT access and refresh tokens based on send refresh token",
        responses = {
            @ApiResponse(responseCode = "200", description = "Token created successfully"),
            @ApiResponse(responseCode = "403", description = "Invalid token")
        })
    @PostMapping("/refresh")
    public TokenResponse refresh(@RequestBody RefreshTokenRequest request) {
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
        System.out.println("Token work");
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
        System.out.println("Token work");
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
        System.out.println("Token work");
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
        System.out.println("Token work");
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
        System.out.println("Token work");
    }
}
