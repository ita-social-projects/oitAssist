package com.itasocialacademy.oitassist.filemanager.providers;

import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileUploadException;
import com.itasocialacademy.oitassist.filemanager.properties.GraphProperties;
import com.itasocialacademy.oitassist.filemanager.providers.interfaces.StorageProvider;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import java.io.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SharePointStorageProvider implements StorageProvider {
    private final GraphServiceClient graphClient;
    private final GraphProperties graphProperties;

    @Override
    public StorageProviderType getType() {
        return StorageProviderType.SHAREPOINT;
    }

    @Override
    public String upload(InputStream inputStream, String morphedName, String path) {
        try {
            String driveId = graphProperties.getDriveId();
            String fullPath = path + "TestUploads/" + morphedName;

            DriveItem item = graphClient
                .drives()
                .byDriveId(driveId)
                .items()
                .byDriveItemId("root:/" + fullPath + ":")
                .content()
                .put(inputStream);

            String webUrl = Optional.ofNullable(item)
                .map(DriveItem::getWebUrl)
                .orElseThrow(() -> new FileUploadException(
                    morphedName,
                    new IllegalStateException("Missing webUrl")));

            log.info("File uploaded to SharePoint: {}", fullPath);

            return webUrl;
        } catch (Exception e) {
            log.error("Failed to upload file {} to SharePoint path {}", morphedName, path, e);
            throw new FileUploadException("Failed to upload file to SharePoint", e);
        }
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
