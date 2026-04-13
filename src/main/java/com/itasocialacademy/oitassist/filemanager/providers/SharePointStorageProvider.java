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

/**
 * Implementation of {@link StorageProvider} for SharePoint storage. Uses
 * Microsoft Graph API to upload files to a configured drive and returns a web
 * URL for access.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SharePointStorageProvider implements StorageProvider {
    /**
     * Microsoft Graph client used to interact with SharePoint resources.
     */
    private final GraphServiceClient graphClient;

    /**
     * Configuration properties containing SharePoint-specific settings such as
     * drive ID.
     */
    private final GraphProperties graphProperties;

    /**
     * {@inheritDoc}
     *
     * @return {@link StorageProviderType#SHAREPOINT}
     */
    @Override
    public StorageProviderType getType() {
        return StorageProviderType.SHAREPOINT;
    }

    /**
     * Uploads a file to SharePoint and returns its web URL. The file is stored
     * relative to the root of the configured drive using path-based addressing.
     *
     * @param inputStream the data stream of the file
     * @param morphedName the unique filename to be used in SharePoint
     * @param path        the folder path relative to the drive root
     * @return a SharePoint web URL that can be used to access the uploaded file
     * @throws FileUploadException if upload fails or response is invalid
     */
    @Override
    public String upload(InputStream inputStream, String morphedName, String path) {
        try {
            if (path != null && path.contains("..")) {
                throw new FileUploadException("Invalid path", new IllegalArgumentException(path));
            }

            String fullPath = (path == null || path.isBlank())
                ? morphedName
                : path.replaceAll("/+$", "") + "/" + morphedName;

            String driveId = graphProperties.getDriveId();

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
