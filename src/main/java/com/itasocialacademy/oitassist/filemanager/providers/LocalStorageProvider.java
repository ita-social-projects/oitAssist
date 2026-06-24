package com.itasocialacademy.oitassist.filemanager.providers;

import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileDeleteException;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileListingException;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileUploadException;
import com.itasocialacademy.oitassist.filemanager.exceptions.InvalidFilePathException;
import com.itasocialacademy.oitassist.filemanager.providers.interfaces.StorageProvider;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link StorageProvider} for local file system storage. This
 * provider manages files within a configured root directory and returns
 * OS-independent relative paths for database persistence. This ensures
 * portability across different environments (Windows, Linux, Docker).
 */
@Slf4j
@Component
public class LocalStorageProvider implements StorageProvider {
    /**
     * The absolute, normalized root path where all files are stored.
     */
    private final Path root;

    /**
     * Constructs the provider and pre-calculates the absolute root path.
     *
     * @param rootPath the base directory for storage, defaults to "./uploads"
     */
    public LocalStorageProvider(@Value("${app.storage.local.root:./uploads}") String rootPath) {
        this.root = Paths.get(rootPath).toAbsolutePath().normalize();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link StorageProviderType#LOCAL}
     */
    @Override
    public StorageProviderType getType() {
        return StorageProviderType.LOCAL;
    }

    /**
     * Uploads a file to the local file system and returns a relative storage key.
     * The method performs security checks to prevent path traversal and ensures the
     * directory structure exists before writing the file.
     *
     * @param inputStream the data stream of the file
     * @param morphedName the unique filename to be used on disk
     * @param path        the subdirectory path within the storage root
     * @return a relative, forward-slash separated storage key for database storage
     * @throws FileUploadException if the path is invalid or an I/O error occurs
     */
    @Override
    public String upload(InputStream inputStream, String morphedName, String path) {
        try {
            Path targetDirectory = root.resolve(path).normalize();
            Path targetFile = targetDirectory.resolve(morphedName).normalize();

            if (!targetFile.startsWith(root)) {
                throw new FileUploadException("Invalid upload path outside configured storage root",
                    new IllegalArgumentException(path));
            }

            Files.createDirectories(targetDirectory);
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);

            String storageKey = root.relativize(targetFile).toString().replace("\\", "/");
            log.info("File successfully uploaded to: {}. Storage key: {}", targetFile, storageKey);

            return storageKey;
        } catch (IOException e) {
            log.error("Failed to upload file {} to path {}", morphedName, path, e);
            throw new FileUploadException("Could not store file locally", e);
        }
    }

    /**
     * Deletes a file from the local file system using its relative storage key.
     *
     * @param storageKey the relative path of the file as stored in the database
     * @throws InvalidFilePathException if the path is empty or points outside the
     *                                  storage root
     * @throws FileDeleteException      if an I/O error occurs during deletion
     */
    @Override
    public void deletePhysical(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new InvalidFilePathException("Cannot delete local file: blank storage key");
        }

        try {
            Path target = root.resolve(storageKey).normalize();

            if (!target.startsWith(root)) {
                throw new InvalidFilePathException("Access denied: path is outside storage root: " + storageKey);
            }

            boolean deleted = Files.deleteIfExists(target);

            if (deleted) {
                log.info("File deleted successfully: {}", target);
            } else {
                log.warn("File not found for deletion at: {}", target);
            }
        } catch (IOException e) {
            log.error("Could not delete physical file at: {}", storageKey, e);
            throw new FileDeleteException("Could not delete physical file: " + storageKey, e);
        }
    }

    /**
     * Scans the entire local storage root directory and returns a list of relative
     * storage keys for all regular files found.
     *
     * <p>
     * This method performs a recursive walk of the {@code root} directory. Each
     * absolute system path is relativized against the storage root and normalized
     * to use forward-slashes (/) to maintain database consistency across different
     * Operating Systems (Windows/Linux).
     * </p>
     *
     * @return a {@link List} of relative storage keys (e.g., "news/image.jpg")
     * @throws FileListingException if an I/O error occurs while walking the file
     *                              tree or if a path cannot be relativized against
     *                              the root.
     */
    @Override
    public List<String> listAllPhysicalKeys() {
        try (var stream = Files.walk(this.root)) {
            List<String> keys = stream
                .filter(Files::isRegularFile)
                .map(path -> {
                    try {
                        return this.root.relativize(path).toString().replace("\\", "/");
                    } catch (IllegalArgumentException e) {
                        throw new FileListingException("Failed to build storage key for path: " + path, e);
                    }
                })
                .toList();
            log.debug("Found {} physical files in local storage", keys.size());
            return keys;
        } catch (IOException e) {
            throw new FileListingException("Failed to walk local storage directory", e);
        }
    }

    /**
     * Retrieves the last modified timestamp of a file from the local file system.
     *
     * <p>
     * This is used primarily as a safety mechanism during rogue file cleanup to
     * prevent the deletion of files currently being uploaded (race conditions). The
     * method verifies that the requested {@code storageKey} does not attempt to
     * access files outside the configured {@code root} directory.
     * </p>
     *
     * @param storageKey the relative path of the file as stored in the database
     * @return the {@link OffsetDateTime} of the file's last modification in UTC
     * @throws InvalidFilePathException if the path is outside the storage root or
     *                                  malformed
     * @throws FileListingException     if the file metadata cannot be accessed due
     *                                  to I/O errors
     */
    @Override
    public OffsetDateTime getLastModified(String storageKey) {
        try {
            Path target = root.resolve(storageKey).normalize();

            if (!target.startsWith(root)) {
                throw new InvalidFilePathException("Access denied: path is outside storage root: " + storageKey);
            }

            FileTime fileTime = Files.getLastModifiedTime(target);
            return OffsetDateTime.ofInstant(fileTime.toInstant(), ZoneOffset.UTC);
        } catch (IOException e) {
            log.error("Failed to retrieve last modified time for file: {}", storageKey, e);
            throw new FileListingException("Could not read file metadata for: " + storageKey, e);
        }
    }

    /**
     * Constructs a URL for accessing the file based on its storage key. This
     * assumes that the application is configured to serve static files from the
     * "/uploads/**" path, which maps to the local storage root directory.
     *
     * @param storageKey the relative path of the file as stored in the database
     * @return a URL string that can be used to access the file via HTTP
     */
    @Override
    public String getFileUrl(String storageKey) {
        return "/uploads/" + storageKey;
    }
}
