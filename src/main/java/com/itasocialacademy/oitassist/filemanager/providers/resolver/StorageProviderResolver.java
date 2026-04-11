package com.itasocialacademy.oitassist.filemanager.providers.resolver;

import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import com.itasocialacademy.oitassist.filemanager.exceptions.UnsupportedStorageException;
import com.itasocialacademy.oitassist.filemanager.providers.interfaces.StorageProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StorageProviderResolver {
    private final List<StorageProvider> providers;

    @Value("${storage.default-provider:LOCAL}")
    private StorageProviderType defaultProvider;

    /**
     * Resolves a {@link StorageProvider} that supports the given type.
     *
     * @param type the desired storage provider type
     * @return the matching {@link StorageProvider}
     * @throws UnsupportedStorageException if no registered provider supports the
     *                                     given type
     */
    public StorageProvider resolve(StorageProviderType type) {
        return providers.stream()
            .filter(p -> p.supports(type))
            .findFirst()
            .orElseThrow(() -> new UnsupportedStorageException(
                "Unsupported storage provider: " + type));
    }

    /**
     * Resolves the default {@link StorageProvider} as configured by the
     * {@code storage.default-provider} property (defaults to {@code LOCAL}).
     *
     * @return the default {@link StorageProvider}
     * @throws UnsupportedStorageException if the configured default type has no
     *                                     matching provider
     */
    public StorageProvider resolveDefault() {
        return resolve(defaultProvider);
    }
}
