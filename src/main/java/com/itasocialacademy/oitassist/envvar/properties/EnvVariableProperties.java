package com.itasocialacademy.oitassist.envvar.properties;

import com.itasocialacademy.oitassist.envvar.dao.enums.AccessMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.util.Set;

@Slf4j
@Validated
@ConfigurationProperties(prefix = "app.envvar")
public record EnvVariableProperties(@NotNull(message = "Access mode cannot be null") @NonNull AccessMode accessMode,
    @NonNull Set<@NotNull(message = "whitelist key cannot be null") @NotBlank(
        message = "whitelist key cannot be blank") String> whitelist,
    @NonNull Set<@NotNull(message = "blacklist key cannot be null") @NotBlank(
        message = "blacklist key cannot be blank") String> blacklist) {
    public EnvVariableProperties(AccessMode accessMode, @Nullable Set<String> whitelist,
        @Nullable Set<String> blacklist) {
        this.accessMode = accessMode;
        this.whitelist = whitelist == null ? Set.of() : Set.copyOf(whitelist);
        this.blacklist = blacklist == null ? Set.of() : Set.copyOf(blacklist);

        if (accessMode == AccessMode.ALL && (blacklist != null || whitelist != null)) {
            throw new IllegalStateException("accessMode=ALL, but a blacklist/whitelist is configured. "
                + "Lists are ignored in this mode: use AccessMode.BLACKLIST or AccessMode.WHITELIST.");
        }

        if (accessMode == AccessMode.WHITELIST) {
            if (whitelist == null) {
                log.warn("Access mode is '{}' but whitelist is not configured: every key will be denied", accessMode);
            }
            if (blacklist != null) {
                log.warn("Access mode is '{}' but blacklist is configured; it will be ignored", accessMode);
            }
        }

        if (accessMode == AccessMode.BLACKLIST) {
            if (blacklist == null) {
                log.warn("Access mode is '{}' but blacklist is not configured: every key will be allowed", accessMode);
            }
            if (whitelist != null) {
                log.warn("Access mode is '{}' but whitelist is configured; it will be ignored", accessMode);
            }
        }
    }
}
