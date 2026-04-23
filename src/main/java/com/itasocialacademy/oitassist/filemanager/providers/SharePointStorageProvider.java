package com.itasocialacademy.oitassist.filemanager.providers;

import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileDeleteException;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileUploadException;
import com.itasocialacademy.oitassist.filemanager.exceptions.InvalidFilePathException;
import com.itasocialacademy.oitassist.filemanager.properties.GraphProperties;
import com.itasocialacademy.oitassist.filemanager.providers.interfaces.StorageProvider;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.ApiException;
import java.io.*;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link StorageProvider} for SharePoint storage. Uses
 * Microsoft Graph API to upload files to a configured drive.
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
     * Uploads a file to SharePoint storage. The file is stored relative to the root
     * of the configured drive using the provided path and filename. The resulting
     * storage key represents the file location within the drive and can be used for
     * further access.
     *
     * @param inputStream the data stream of the file
     * @param morphedName the unique filename to be used in SharePoint
     * @param path        the folder path relative to the drive root
     * @return a relative storage key identifying the file in SharePoint
     * @throws FileUploadException if upload fails or the path is invalid
     */
    @Override
    public String upload(InputStream inputStream, String morphedName, String path) {
        try {
            if (path != null && path.contains("..")) {
                throw new FileUploadException("Invalid path", new IllegalArgumentException(path));
            }

            String storageKey = (path == null || path.isBlank())
                ? morphedName
                : path + "/" + morphedName;

            String driveId = graphProperties.getDriveId();

            graphClient
                .drives()
                .byDriveId(driveId)
                .items()
                .byDriveItemId("root:/" + storageKey + ":")
                .content()
                .put(inputStream);

            log.info("File uploaded to SharePoint: {}", storageKey);

            return storageKey;
        } catch (FileUploadException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upload file {} to SharePoint path {}", morphedName, path, e);
            throw new FileUploadException("Failed to upload file to SharePoint", e);
        }
    }

    /**
     * Deletes a file from SharePoint storage using its relative storage key.
     *
     * @param storageKey the relative path of the file within the configured drive
     * @throws InvalidFilePathException if the storage key is null or blank
     * @throws FileDeleteException      if an error occurs during deletion via
     *                                  Microsoft Graph API
     */
    @Override
    public void deletePhysical(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new InvalidFilePathException("Cannot delete SharePoint file: blank storage key");
        }

        try {
            String driveId = graphProperties.getDriveId();

            graphClient
                .drives()
                .byDriveId(driveId)
                .items()
                .byDriveItemId("root:/" + storageKey + ":")
                .delete();

            log.info("File deleted from SharePoint: {}", storageKey);
        } catch (ApiException e) {
            if (e.getResponseStatusCode() == 404) {
                log.warn("File not found in SharePoint for deletion: {}", storageKey);
                return;
            }
            log.error("Graph API error while deleting file: {}", storageKey, e);
            throw new FileDeleteException("Failed to delete file from SharePoint", e);
        } catch (Exception e) {
            log.error("Unexpected error while deleting file: {}", storageKey, e);
            throw new FileDeleteException("Failed to delete file from SharePoint", e);
        }
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
