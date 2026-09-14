package com.itasocialacademy.oitassist.envvar.provider.interfaces;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.util.Map;

public interface EnvVariableProvider {
    @NonNull
    Map<@NonNull String, @Nullable String> getenv();
}
