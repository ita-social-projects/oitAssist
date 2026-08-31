package com.itasocialacademy.oitassist.chat.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.realtime")
public record RealtimeMessagingProperties(
        @NotEmpty List<@NotBlank String> allowedOrigins,
        @PositiveOrZero long serverHeartbeat,
        @PositiveOrZero long clientHeartbeat) {
    public RealtimeMessagingProperties {
        Objects.requireNonNull(
                allowedOrigins,
                "Realtime allowed origins must not be null");
        allowedOrigins = List.copyOf(allowedOrigins);
    }
}