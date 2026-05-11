package com.itasocialacademy.oitassist.core.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "web-client")
@Validated
public record WebClientProperties(
    @NotBlank @Pattern(regexp = ".*[^/]$", message = "must not end with '/'") String origin,
    @NotBlank @Pattern(regexp = "^/.*", message = "must start with '/'") String oauthCallbackPath) {
}