package com.itasocialacademy.oitassist.security.controller;

import com.itasocialacademy.oitassist.security.service.interfaces.TokenService;
import com.itasocialacademy.oitassist.security.dao.dto.request.TokenRequest;
import com.itasocialacademy.oitassist.security.dao.dto.response.TokenResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecurityController {
    private final TokenService tokenService;

    public SecurityController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/token/create")
    public TokenResponse createToken(@RequestBody TokenRequest tokenRequest) {
        return tokenService.generateToken(tokenRequest);
    }

    @GetMapping("/api")
    public void api() {
        System.out.println("Token work");
    }

    @GetMapping("/api/get")
    public void apiGet() {
        System.out.println("Token work");
    }

    @GetMapping("/api/get/get")
    public void apiGetGet() {
        System.out.println("Token work");
    }

    @GetMapping("/api/admin")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void apiAdmin() {
        System.out.println("Token work");
    }

    @GetMapping("/api/user")
    @PreAuthorize("hasRole('ROLE_USER')")
    public void apiUser() {
        System.out.println("Token work");
    }
}
