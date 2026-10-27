package com.itasocialacademy.oitassist.filemanager.providers.interfaces;

import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileListingException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileUploadException;
import org.springframework.core.io.Resource;

public interface StorageProvider {
    /**
     * Determines if this provider supports the given type (e.g., LOCAL,
     * SHAREPOINT).
     */
    default boolean supports(StorageProviderType type) {
        return getType() == type;
    }

    /**
     * Retrieves the type of storage provider represented by this implementation.
     *
     * @return the specific {@code StorageProviderType} associated with this storage
     *         provider, such as {@code LOCAL} or {@code SHAREPOINT}.
     */
    StorageProviderType getType();

    /**
     * Handles the physical byte transfer.
     */
    String upload(InputStream inputStream, String morphedName, String path);

    /**
     * Physically deletes a file.
     *
     * @param filePath relative path including filename.
     */
    void deletePhysical(String filePath);

    /**
     * Scans physical storage and returns all relative keys.
     *
     * @throws FileListingException if scanning or key construction fails.
     */
    List<String> listAllPhysicalKeys();

    /**
     * Returns the last modified time of a physical file.
     *
     * @param storageKey the relative path of the file
     * @return the OffsetDateTime of last modification, or a fallback (e.g., now) if
     *         unavailable
     * @throws FileListingException if the file metadata cannot be accessed
     */
    OffsetDateTime getLastModified(String storageKey);

    /**
     * Retrieves the file as a Spring {@link Resource} using its relative storage
     * key.
     *
     * @param storageKey the relative path or unique key of the file
     * @return the {@link Resource} representing the file byte stream
     */
    Resource getResource(String storageKey);

    /**
     * Moves a file from one location to another within the storage provider. This
     * is primarily used to route temporary files to their final hierarchical
     * destination once they are permanently linked to an entity.
     *
     * @param sourceKey the current relative path of the file
     * @param targetKey the desired new relative path of the file
     * @return the final relative path (storage key) after the move
     * @throws FileUploadException if the file cannot be moved
     */
    String moveFile(String sourceKey, String targetKey);

    /**
     * Creates a reference to a task in the specified assignment folder.
     *
     * <p>
     * Depending on the storage provider implementation, this creates either an OS
     * symbolic link (for local filesystem storage) or an Internet Shortcut
     * {@code task.url} file (for SharePoint storage).
     * </p>
     *
     * @param assignmentPath the relative destination folder path where the
     *                       reference should be placed (e.g.,
     *                       {@code "olympiads/Comp/Stage/Tour/Task"})
     * @param taskPoolPath   the relative source path pointing to the canonical task
     *                       folder in the task pool (e.g., {@code "tasks/Task"})
     */
    void createTaskReference(String assignmentPath, String taskPoolPath);

    /**
     * Deletes the task reference (symlink or {@code task.url} file) from the
     * specified assignment folder.
     *
     * @param assignmentPath the relative destination folder path containing the
     *                       reference to delete
     */
    void deleteTaskReference(String assignmentPath);
}
