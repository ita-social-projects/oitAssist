package com.itasocialacademy.oitassist.filemanager.providers.interfaces;

import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import java.io.InputStream;

public interface StorageProvider {
    /**
     * Determines if this provider supports the given type (e.g., LOCAL,
     * SHAREPOINT).
     */
    boolean supports(StorageProviderType source);

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
