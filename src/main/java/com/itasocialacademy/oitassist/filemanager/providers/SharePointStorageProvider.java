package com.itasocialacademy.oitassist.filemanager.providers;

import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileDeleteException;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileListingException;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileUploadException;
import com.itasocialacademy.oitassist.filemanager.exceptions.InvalidFilePathException;
import com.itasocialacademy.oitassist.filemanager.properties.GraphProperties;
import com.itasocialacademy.oitassist.filemanager.providers.interfaces.StorageProvider;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.ApiException;
import java.io.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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
     * Prefix used to address items by path relative to the SharePoint drive root.
     */
    private static final String DRIVE_ROOT_PREFIX = "root:/";

    /**
     * Suffix required to close the path-based item addressing syntax in Graph API.
     */
    private static final String DRIVE_ROOT_SUFFIX = ":";

    /**
     * Delimiter used to separate segments in SharePoint storage keys.
     */
    private static final String PATH_DELIMITER = "/";

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
                .byDriveItemId(DRIVE_ROOT_PREFIX + storageKey + DRIVE_ROOT_SUFFIX)
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
                .byDriveItemId(DRIVE_ROOT_PREFIX + storageKey + DRIVE_ROOT_SUFFIX)
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

    /**
     * Recursively lists all physical file keys stored in the configured SharePoint
     * drive.
     *
     * <p>
     * Performs a recursive traversal starting from the drive root, handling
     * paginated responses from the Graph API to ensure all files are collected
     * regardless of folder size.
     * </p>
     *
     * @return a {@link List} of relative storage keys (e.g., "news/image.jpg")
     * @throws FileListingException if the Graph API returns an error during
     *                              traversal
     */
    @Override
    public List<String> listAllPhysicalKeys() {
        try {
            List<String> keys = new ArrayList<>();

            String driveId = graphProperties.getDriveId();

            traverseFolder("root", "", driveId, keys);

            log.debug("Found {} files in SharePoint storage", keys.size());
            return keys;
        } catch (Exception e) {
            throw new FileListingException("Failed to list files from SharePoint", e);
        }
    }

    /**
     * Recursively collects relative storage keys for all files under a given drive
     * item, handling pagination via {@code @odata.nextLink}.
     *
     * @param itemId  the ID of the current drive item to inspect
     * @param path    the relative path accumulated so far
     * @param driveId the configured SharePoint drive ID
     * @param keys    the list being populated with discovered file keys
     */
    private void traverseFolder(String itemId, String path, String driveId, List<String> keys) {
        var page = graphClient
            .drives()
            .byDriveId(driveId)
            .items()
            .byDriveItemId(itemId)
            .children()
            .get();

        while (page != null && page.getValue() != null) {
            log.debug("Processing page for path: {}", path);
            for (var item : page.getValue()) {
                String currentPath = path.isBlank()
                    ? item.getName()
                    : path + PATH_DELIMITER + item.getName();

                if (item.getFolder() != null) {
                    traverseFolder(item.getId(), currentPath, driveId, keys);
                } else {
                    keys.add(currentPath);
                }
            }

            String nextLink = page.getOdataNextLink();
            if (nextLink == null) {
                break;
            }

            log.debug("Fetching next page...");

            page = graphClient
                .drives()
                .byDriveId(driveId)
                .items()
                .byDriveItemId(itemId)
                .children()
                .withUrl(nextLink)
                .get();
        }
    }

    /**
     * Retrieves the last modified timestamp of a file in SharePoint using its
     * relative storage key.
     *
     * <p>
     * Returns {@code null} if the timestamp cannot be determined (file not found,
     * or metadata unavailable). The caller ({@code FileCleanupServiceImpl}) treats
     * {@code null} as "skip this file" — the safe, conservative path.
     * </p>
     *
     * @param storageKey the relative path of the file as stored in the database
     * @return the {@link OffsetDateTime} of the file's last modification, or
     *         {@code null} if unavailable
     * @throws InvalidFilePathException if the storage key is null or blank
     * @throws FileListingException     if the Graph API returns an unexpected error
     */
    @Override
    public OffsetDateTime getLastModified(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new InvalidFilePathException("Cannot get last modified: blank storage key");
        }

        try {
            String driveId = graphProperties.getDriveId();

            var item = graphClient
                .drives()
                .byDriveId(driveId)
                .items()
                .byDriveItemId(DRIVE_ROOT_PREFIX + storageKey + DRIVE_ROOT_SUFFIX)
                .get();

            if (item == null || item.getLastModifiedDateTime() == null) {
                return null;
            }
            return item.getLastModifiedDateTime();
        } catch (ApiException e) {
            if (e.getResponseStatusCode() == 404) {
                log.warn("File not found in SharePoint when retrieving lastModified: {}", storageKey);
                return null;
            }
            log.error("Graph API error while retrieving lastModified for file: {}", storageKey, e);
            throw new FileListingException("Failed to get last modified from SharePoint", e);
        } catch (Exception e) {
            log.error("Unexpected error while retrieving lastModified for file: {}", storageKey, e);
            throw new FileListingException("Failed to get last modified from SharePoint", e);
        }
    }

    /**
     * Constructs a URL for accessing the file in SharePoint based on its storage
     * key.
     *
     * @param storageKey the relative path of the file within the configured drive
     * @return a URL string that can be used to access the file in SharePoint
     */
    @Override
    public String getFileUrl(String storageKey) {
        try {
            String driveId = graphProperties.getDriveId();

            var item = graphClient
                .drives()
                .byDriveId(driveId)
                .items()
                .byDriveItemId(DRIVE_ROOT_PREFIX + storageKey + DRIVE_ROOT_SUFFIX)
                .get();

            if (item == null || item.getWebUrl() == null) {
                log.warn("Could not retrieve webUrl for file: {}", storageKey);
                return null;
            }

            return item.getWebUrl();
        } catch (ApiException e) {
            log.error("Graph API error while retrieving webUrl for: {}", storageKey, e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error while retrieving webUrl for: {}", storageKey, e);
            return null;
        }
    }
}
