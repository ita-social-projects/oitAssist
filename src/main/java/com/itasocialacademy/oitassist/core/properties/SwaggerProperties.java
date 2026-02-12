package com.itasocialacademy.oitassist.core.properties;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oit.swagger")
public record SwaggerProperties(List<ServerConfig> servers) {
    public record ServerConfig(String url, String description) {
    }
}
