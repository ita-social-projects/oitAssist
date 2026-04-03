package com.itasocialacademy.oitassist.filemanager.providers.interfaces;

import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import java.io.InputStream;

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
     * @param filePath full file path including filename.
     */
    void deletePhysical(String filePath);
}
