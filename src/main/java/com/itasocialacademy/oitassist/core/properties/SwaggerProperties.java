package com.itasocialacademy.oitassist.core.properties;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oit.swagger")
public record SwaggerProperties(
    @NotNull List<ServerConfig> servers) {
    public SwaggerProperties {
        servers = servers == null ? List.of() : List.copyOf(servers);
    }

    public record ServerConfig(
        @NotNull String url,
        @NotNull String description) {
    }
}
