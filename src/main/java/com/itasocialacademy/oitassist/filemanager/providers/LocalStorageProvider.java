package com.itasocialacademy.oitassist.filemanager.providers;

import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileUploadFailureException;
import com.itasocialacademy.oitassist.filemanager.providers.interfaces.StorageProvider;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LocalStorageProvider implements StorageProvider {
    // For testing purposes AND may be used in upload() directory constructing
    private final String rootPath;

    public LocalStorageProvider(@Value("${app.storage.local.root:./uploads}") String rootPath) {
        this.rootPath = rootPath;
    }

    @Override
    public boolean supports(StorageProviderType source) {
        return source == StorageProviderType.LOCAL;
    }

    @Override
    public String upload(InputStream inputStream, String morphedName, String path) {
        try {
            Path root = Paths.get(rootPath).toAbsolutePath().normalize();
            Path normalizedDirectory = root.resolve(path).normalize();
            Path normalizedFile = normalizedDirectory.resolve(morphedName).normalize();

            if (!normalizedDirectory.startsWith(root) || !normalizedFile.startsWith(root)) {
                throw new FileUploadFailureException("Invalid upload path outside configured storage root",
                    new IllegalArgumentException(path));
            }
            Files.createDirectories(normalizedDirectory);

            //todo: are we okay with the silent overwrite?
            Files.copy(inputStream, normalizedFile, StandardCopyOption.REPLACE_EXISTING);

            log.info("File successfully uploaded to: {}", normalizedFile);

            return normalizedFile.toString();
        } catch (IOException e) {
            log.error("Failed to upload file {} to path {}", morphedName, path, e);
            throw new FileUploadFailureException("Could not store file locally", e);
        }
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
            throw new IllegalStateException("Could not delete physical file: " + fileFullPath, e);
        }
    }
}
