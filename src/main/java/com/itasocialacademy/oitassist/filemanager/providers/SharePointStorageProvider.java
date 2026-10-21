package com.itasocialacademy.oitassist.filemanager.providers;

import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileAssetNotFoundException;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileDeleteException;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileListingException;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileUploadException;
import com.itasocialacademy.oitassist.filemanager.exceptions.InvalidFilePathException;
import com.itasocialacademy.oitassist.filemanager.properties.GraphProperties;
import com.itasocialacademy.oitassist.filemanager.providers.interfaces.StorageProvider;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.ItemReference;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.ApiException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

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
     * Fetches the file content stream directly from the configured Microsoft Graph
     * SharePoint drive and wraps it in an {@link InputStreamResource}.
     *
     * @param storageKey the relative path of the file within the SharePoint drive
     * @return the {@link Resource} streaming the SharePoint file contents
     * @throws InvalidFilePathException   if the storage key is blank
     * @throws FileAssetNotFoundException if Microsoft Graph returns a 404 Not Found
     * @throws FileListingException       if reading or streaming from SharePoint
     *                                    fails
     */
    @Override
    public Resource getResource(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new InvalidFilePathException("Cannot get resource: blank storage key");
        }
        try {
            InputStream content = graphClient
                .drives()
                .byDriveId(graphProperties.getDriveId())
                .items()
                .byDriveItemId(DRIVE_ROOT_PREFIX + storageKey + DRIVE_ROOT_SUFFIX)
                .content()
                .get();

            if (content == null) {
                throw new FileListingException(
                    "Could not retrieve file content from SharePoint for: " + storageKey,
                    new IllegalArgumentException(storageKey));
            }

            return new InputStreamResource(content);
        } catch (ApiException e) {
            if (e.getResponseStatusCode() == 404) {
                log.warn("File not found in SharePoint when retrieving resource: {}", storageKey);
                throw new FileAssetNotFoundException("File not found in SharePoint storage: " + storageKey);
            }
            log.error("Failed to stream file from SharePoint: {}", storageKey, e);
            throw new FileListingException("Could not read file from SharePoint", e);
        } catch (Exception e) {
            log.error("Unexpected error while streaming file from SharePoint: {}", storageKey, e);
            throw new FileListingException("Could not read file from SharePoint", e);
        }
    }

    /**
     * Moves a file within the SharePoint drive from the source storage key to the
     * target. Leverages the Microsoft Graph API PATCH endpoint to update the parent
     * reference and item name.
     *
     * @param sourceKey the relative path of the existing file in the SharePoint
     *                  drive
     * @param targetKey the desired relative path
     * @return the final relative path after the move
     * @throws InvalidFilePathException if the storage keys are null or blank
     * @throws FileUploadException      if the Graph API returns an error or fails
     */
    @Override
    public String moveFile(String sourceKey, String targetKey) {
        if (sourceKey == null || sourceKey.isBlank() || targetKey == null || targetKey.isBlank()) {
            throw new InvalidFilePathException("Cannot move SharePoint file: blank storage key");
        }

        int slash = targetKey.lastIndexOf('/');
        if (slash < 0) {
            throw new InvalidFilePathException("Target key has no folder: " + targetKey);
        }
        String targetFolder = targetKey.substring(0, slash);
        String targetName = targetKey.substring(slash + 1);

        try {
            String driveId = graphProperties.getDriveId();

            DriveItem parentItem = ensureTargetFolderExistsAndGet(driveId, targetFolder);

            if (parentItem == null || parentItem.getId() == null) {
                throw new FileListingException("Target folder not found in SharePoint: " + targetFolder,
                    new IllegalStateException("Graph API returned null DriveItem ID after folder creation"));
            }

            DriveItem body = new DriveItem();
            body.setName(targetName);

            ItemReference ref = new ItemReference();

            ref.setId(parentItem.getId());
            body.setParentReference(ref);

            graphClient.drives().byDriveId(driveId)
                .items().byDriveItemId(DRIVE_ROOT_PREFIX + sourceKey + DRIVE_ROOT_SUFFIX)
                .patch(body);

            return targetKey;
        } catch (ApiException e) {
            if (e.getResponseStatusCode() == 404) {
                log.warn("File not found in SharePoint when moving: {}", sourceKey);
                throw new FileUploadException("Source file not found in SharePoint: " + sourceKey, e);
            }
            log.error("Graph API error while moving file from {} to {}", sourceKey, targetKey, e);
            throw new FileUploadException("Failed to move file in SharePoint", e);
        } catch (Exception e) {
            log.error("Unexpected error while moving SharePoint file from {} to {}", sourceKey, targetKey, e);
            throw new FileUploadException("Failed to move file in SharePoint", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Generates an Internet Shortcut ({@code task.url}) file containing a web link
     * to the task pool folder in SharePoint, and uploads it to the assignment
     * folder.
     * </p>
     *
     * @param assignmentPath the relative destination folder path in SharePoint
     *                       where the shortcut should be created
     * @param taskPoolPath   the relative source path pointing to the task pool
     *                       folder in SharePoint
     */
    @Override
    public void createTaskReference(String assignmentPath, String taskPoolPath) {
        if (assignmentPath == null || assignmentPath.isBlank() || taskPoolPath == null || taskPoolPath.isBlank()) {
            throw new InvalidFilePathException(
                "Cannot create .url file for task: blank assignment or taskPool path");
        }

        String cleanSiteUrl = getCleanSiteUrl();

        String normalizedTaskPoolPath = taskPoolPath.strip();

        if (normalizedTaskPoolPath.startsWith("/")) {
            normalizedTaskPoolPath = normalizedTaskPoolPath.substring(1);
        }

        String encodedTaskPoolPath = UriUtils.encodePath(normalizedTaskPoolPath, StandardCharsets.UTF_8);

        String content = "[InternetShortcut]\r\nURL=" + cleanSiteUrl + "/" + encodedTaskPoolPath + "\r\n";

        try (var is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
            upload(is, "task.url", assignmentPath);
        } catch (IOException e) {
            log.warn("Failed to create task.url in {}: {}",
                assignmentPath, e.getMessage());
        }
    }

    /**
     * Retrieves and sanitizes the SharePoint base site URL from configuration.
     *
     * <p>
     * Trims leading/trailing whitespace, strips any trailing slash, and encodes
     * spaces as {@code %20} so the returned URL can be safely concatenated with
     * relative paths.
     * </p>
     *
     * @return the sanitized base site URL
     * @throws InvalidFilePathException if {@code graph.site-url} is null or blank
     */
    private String getCleanSiteUrl() {
        String siteUrl = graphProperties.getSiteUrl();

        if (siteUrl == null || siteUrl.isBlank()) {
            throw new InvalidFilePathException(
                "Cannot create .url file for task: blank siteUrl");
        }

        String cleanSiteUrl = siteUrl.strip();

        if (cleanSiteUrl.endsWith("/")) {
            cleanSiteUrl = cleanSiteUrl.substring(0, cleanSiteUrl.length() - 1);
        }

        cleanSiteUrl = cleanSiteUrl.replace(" ", "%20");
        return cleanSiteUrl;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Deletes the {@code task.url} file located in the specified SharePoint
     * assignment folder.
     * </p>
     *
     * @param assignmentPath the relative destination folder path in SharePoint
     *                       containing the {@code task.url} file
     */
    @Override
    public void deleteTaskReference(String assignmentPath) {
        try {
            deletePhysical(assignmentPath + "/task.url");
        } catch (Exception e) {
            log.warn("Failed to delete task.url from {}: {}",
                assignmentPath, e.getMessage());
        }
    }

    /**
     * Ensures that the specified target folder exists in SharePoint and retrieves
     * it.
     *
     * <p>
     * First attempts to fetch the folder directly to avoid unnecessary API calls if
     * it already exists. If Microsoft Graph returns a 404 (Not Found), it triggers
     * the creation of the folder by uploading a temporary marker file, and then
     * fetches the newly created folder.
     * </p>
     *
     * @param driveId      the configured SharePoint drive ID
     * @param targetFolder the relative path of the target folder
     * @return the {@link DriveItem} representing the target folder
     * @throws ApiException if an unexpected error occurs communicating with
     *                      Microsoft Graph
     */
    private DriveItem ensureTargetFolderExistsAndGet(String driveId, String targetFolder) {
        try {
            return graphClient.drives().byDriveId(driveId)
                .items().byDriveItemId(DRIVE_ROOT_PREFIX + targetFolder + DRIVE_ROOT_SUFFIX)
                .get();
        } catch (ApiException e) {
            if (e.getResponseStatusCode() == 404) {
                log.debug("Target folder {} not found, creating via marker file...", targetFolder);
                createTargetFolderBeforeMove(targetFolder);

                return graphClient.drives().byDriveId(driveId)
                    .items().byDriveItemId(DRIVE_ROOT_PREFIX + targetFolder + DRIVE_ROOT_SUFFIX)
                    .get();
            }
            throw e;
        }
    }

    /**
     * Creates a folder in SharePoint by uploading and immediately deleting a
     * temporary marker file.
     *
     * <p>
     * This is a workaround for the Microsoft Graph API, which requires target
     * folders to exist before moving files into them via {@code PATCH}, but
     * uploading a file implicitly creates all missing parent directories.
     * </p>
     *
     * @param targetFolder the relative path of the target folder to create
     */
    private void createTargetFolderBeforeMove(String targetFolder) {
        String marker = ".keep-" + UUID.randomUUID();
        upload(InputStream.nullInputStream(), marker, targetFolder);
        try {
            deletePhysical(targetFolder + "/" + marker);
        } catch (FileDeleteException e) {
            log.warn("Could not remove marker {} in {}", marker, targetFolder, e);
        }
    }
}
