package com.itasocialacademy.oitassist.envvar.provider;

import com.itasocialacademy.oitassist.envvar.provider.interfaces.EnvVariableProvider;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class SystemEnvVariableProvider implements EnvVariableProvider {
    @Override
    public @NonNull Map<@NonNull String, @Nullable String> getenv() {
        return System.getenv();
    }
}
