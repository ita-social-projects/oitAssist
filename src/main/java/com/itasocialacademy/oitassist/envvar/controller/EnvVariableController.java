package com.itasocialacademy.oitassist.envvar.controller;

import com.itasocialacademy.oitassist.envvar.service.interfaces.EnvVariableService;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/environment-variables")
@Hidden
public class EnvVariableController {
    private final SecurityFacade securityFacade;
    private final EnvVariableService envVariableService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public @NonNull Map<@NonNull String, @Nullable String> getMap() {
        log.info("User with ID '{}' requested all environment variables", securityFacade.getCurrentUserId());
        return envVariableService.getenv();
    }
}
