package com.itasocialacademy.oitassist.envvar.service.interfaces;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.util.Map;

public interface EnvVariableService {
    Map<@NonNull String, @Nullable String> getenv();
}
