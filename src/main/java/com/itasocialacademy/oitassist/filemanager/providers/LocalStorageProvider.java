package com.itasocialacademy.oitassist.filemanager.providers;

import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import com.itasocialacademy.oitassist.filemanager.providers.interfaces.StorageProvider;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
public class LocalStorageProvider implements StorageProvider {
    // For testing purposes AND may be used in upload()
    private final String rootPath;

    public LocalStorageProvider(@Value("${app.storage.local.root}") String rootPath) {
        this.rootPath = rootPath;
    }

    @Override
    public boolean supports(StorageProviderType source) {
        return source == StorageProviderType.LOCAL;
    }

    @Override
    public String upload(InputStream inputStream, String morphedName, String path) {
        return "";
    }

    @Override
    public void deletePhysical(String fileFullPath) {
        try {
            Path path = Paths.get(fileFullPath);

            boolean deleted = Files.deleteIfExists(path);

            if (deleted) {
                log.info("Physically deleted file: {}", fileFullPath);
            } else {
                log.warn("Attempted to delete file, but it did not exist: {}", fileFullPath);
            }
        } catch (NoSuchFileException e) {
            log.error("File deletion failed. No file found: {}", fileFullPath, e);
        } catch (IOException e) {
            log.error("Could not delete physical file at: {}", fileFullPath, e);
        }
    }
}
