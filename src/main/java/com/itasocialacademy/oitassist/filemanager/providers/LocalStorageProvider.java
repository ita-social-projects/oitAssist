package com.itasocialacademy.oitassist.filemanager.providers;

import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileDeleteException;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileUploadException;
import com.itasocialacademy.oitassist.filemanager.exceptions.InvalidFilePathException;
import com.itasocialacademy.oitassist.filemanager.providers.interfaces.StorageProvider;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LocalStorageProvider implements StorageProvider {
    private final String rootPath;

    public LocalStorageProvider(@Value("${app.storage.local.root:./uploads}") String rootPath) {
        this.rootPath = rootPath;
    }

    @Override
    public StorageProviderType getType() {
        return StorageProviderType.LOCAL;
    }

    @Override
    public String upload(InputStream inputStream, String morphedName, String path) {
        try {
            Path root = Paths.get(rootPath).toAbsolutePath().normalize();
            Path normalizedDirectory = root.resolve(path).normalize();
            Path normalizedFile = normalizedDirectory.resolve(morphedName).normalize();

            if (!normalizedDirectory.startsWith(root) || !normalizedFile.startsWith(root)) {
                throw new FileUploadException("Invalid upload path outside configured storage root",
                    new IllegalArgumentException(path));
            }
            Files.createDirectories(normalizedDirectory);

            // are we okay with the silent overwrite?
            Files.copy(inputStream, normalizedFile, StandardCopyOption.REPLACE_EXISTING);

            log.info("File successfully uploaded to: {}", normalizedFile);

            return normalizedFile.toString();
        } catch (IOException e) {
            log.error("Failed to upload file {} to path {}", morphedName, path, e);
            throw new FileUploadException("Could not store file locally", e);
        }
    }

    @Override
    public void deletePhysical(String fileFullPath) {
        if (fileFullPath == null || fileFullPath.isBlank()) {
            throw new InvalidFilePathException("Cannot delete local file: blank storage path");
        }

        try {
            Path root = Paths.get(rootPath).toAbsolutePath().normalize();
            Path inputPath = Paths.get(fileFullPath);
            Path target;

            if (inputPath.isAbsolute()) {
                target = inputPath.normalize();
            } else {
                target = root.resolve(fileFullPath).normalize();
                if (!Files.exists(target) && fileFullPath.startsWith(root.getFileName().toString())) {
                    Path strippedPath = inputPath.subpath(1, inputPath.getNameCount());
                    Path candidateTarget = root.resolve(strippedPath).normalize();
                    if (Files.exists(candidateTarget)) {
                        target = candidateTarget;
                    }
                }
            }

            if (!target.startsWith(root)) {
                throw new InvalidFilePathException("Invalid delete path outside configured storage root: "
                    + fileFullPath);
            }

            boolean deleted = Files.deleteIfExists(target);

            if (deleted) {
                log.info("File deleted successfully: {}", target);
            } else {
                log.warn("File not found at: {}", target.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Could not delete physical file at: {}", fileFullPath, e);
            throw new FileDeleteException("Could not delete physical file: " + fileFullPath, e);
        }
    }
}
