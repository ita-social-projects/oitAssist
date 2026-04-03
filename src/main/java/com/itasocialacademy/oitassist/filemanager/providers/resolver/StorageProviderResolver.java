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

    public StorageProvider resolve(StorageProviderType type) {
        return providers.stream()
            .filter(p -> p.supports(type))
            .findFirst()
            .orElseThrow(() -> new UnsupportedStorageException(
                "Unsupported storage provider: " + type));
    }

    public StorageProvider resolveDefault() {
        return resolve(defaultProvider);
    }
}
