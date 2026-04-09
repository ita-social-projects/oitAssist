package com.itasocialacademy.oitassist.filemanager.providers.interfaces;

import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileListingException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;

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
}
