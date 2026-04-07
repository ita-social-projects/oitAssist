package com.itasocialacademy.oitassist.filemanager.providers;

import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import com.itasocialacademy.oitassist.filemanager.providers.interfaces.StorageProvider;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;

public class SharePointStorageProvider implements StorageProvider {
    @Override
    public StorageProviderType getType() {
        return StorageProviderType.SHAREPOINT;
    }

    @Override
    public String upload(InputStream inputStream, String morphedName, String path) {
        return "";
    }

    @Override
    public void deletePhysical(String filePath) {
        // Implement SharePoint deletion logic
    }

    @Override
    public List<String> listAllPhysicalKeys() {
        return List.of();
    }

    @Override
    public OffsetDateTime getLastModified(String storageKey) {
        return null;
    }
}
