package com.itasocialacademy.oitassist.envvar.service;

import com.itasocialacademy.oitassist.envvar.properties.EnvVariableProperties;
import com.itasocialacademy.oitassist.envvar.provider.interfaces.EnvVariableProvider;
import com.itasocialacademy.oitassist.envvar.service.interfaces.EnvVariableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static com.itasocialacademy.oitassist.envvar.dao.enums.AccessMode.ALL;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnvVariableServiceImpl implements EnvVariableService {
    private final EnvVariableProvider envVariableProvider;
    private final EnvVariableProperties envVariableProperties;

    @Override
    public @NonNull Map<@NonNull String, @Nullable String> getenv() {
        Map<String, String> all = envVariableProvider.getenv();
        if (envVariableProperties.accessMode() == ALL) {
            return Collections.unmodifiableMap(all);
        }

        Map<String, String> result = new HashMap<>();
        all.forEach((key, value) -> {
            if (isAllowed(key)) {
                result.put(key, value);
            }
        });
        return Collections.unmodifiableMap(result);
    }

    private boolean isAllowed(@NonNull String key) {
        return switch (envVariableProperties.accessMode()) {
            case ALL -> true;
            case WHITELIST -> envVariableProperties.whitelist().contains(key);
            case BLACKLIST -> !envVariableProperties.blacklist().contains(key);
        };
    }
}
