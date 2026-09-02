package com.itasocialacademy.oitassist.logfile.dao;

import com.itasocialacademy.oitassist.logfile.dao.model.LogFileMetadata;
import com.itasocialacademy.oitassist.logfile.exceptions.InvalidLogFileNameException;
import com.itasocialacademy.oitassist.logfile.exceptions.LogFileDownloadException;
import com.itasocialacademy.oitassist.logfile.exceptions.LogFileListingException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class FileSystemLogFileDao implements LogFileDao {
    private final Path logDirectory;

    public FileSystemLogFileDao(
        @Value("${logging.file.name}") String configuredLogFile) {
        this.logDirectory =
            resolveLogDirectory(configuredLogFile);
    }

    @Override
    public List<LogFileMetadata> findAll() {
        return findFiles(path -> true);
    }

    @Override
    public List<LogFileMetadata> findByNameContainingIgnoreCase(String name) {
        String normalizedName = name.toLowerCase(Locale.ROOT);
        return findFiles(path -> path.getFileName()
            .toString()
            .toLowerCase(Locale.ROOT)
            .contains(normalizedName));
    }

    @Override
    public Optional<Path> downloadFile(String fileName) {
        validateLogDirectory();

        Path filePath = resolveDownloadPath(fileName);

        try {
            BasicFileAttributes attributes =
                Files.readAttributes(
                    filePath,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS);

            if (attributes.isSymbolicLink() || !attributes.isRegularFile()) {
                return Optional.empty();
            }
            return Optional.of(filePath);
        } catch (NoSuchFileException exception) {
            log.debug("Log file not found for download: {}", fileName);

            return Optional.empty();
        } catch (IOException | SecurityException exception) {
            log.error("Failed to access log file for download: {}", filePath, exception);

            throw new LogFileDownloadException();
        }
    }

    private Path resolveDownloadPath(String fileName) {
        try {
            Path filePath = logDirectory
                .resolve(fileName)
                .normalize();

            if (!filePath.startsWith(logDirectory)
                || !logDirectory.equals(filePath.getParent())) {
                throw new InvalidLogFileNameException();
            }

            return filePath;
        } catch (InvalidPathException exception) {
            throw new InvalidLogFileNameException();
        }
    }

    private List<LogFileMetadata> findFiles(Predicate<Path> filter) {
        validateLogDirectory();

        try (Stream<Path> paths = Files.list(logDirectory)) {
            return paths
                .filter(filter)
                .map(this::readMetadata)
                .flatMap(Optional::stream)
                .toList();
        } catch (UncheckedIOException exception) {
            log.error(
                "Failed to read metadata from log directory: {}",
                logDirectory,
                exception.getCause());

            throw new LogFileListingException();
        } catch (IOException | SecurityException exception) {
            log.error("Failed to access log directory: {}", logDirectory, exception);
            throw new LogFileListingException();
        }
    }

    private void validateLogDirectory() {
        if (!Files.exists(
            logDirectory,
            LinkOption.NOFOLLOW_LINKS)) {
            log.error(
                "Configured log directory does not exist: {}",
                logDirectory);
            throw new LogFileListingException();
        }

        if (!Files.isDirectory(
            logDirectory,
            LinkOption.NOFOLLOW_LINKS)) {
            log.error(
                "Configured log path is not a directory: {}",
                logDirectory);

            throw new LogFileListingException();
        }
        if (!Files.isReadable(logDirectory)) {
            log.error("Configured log directory is not readable: {}", logDirectory);
            throw new LogFileListingException();
        }
    }

    private static Path resolveLogDirectory(String configuredLogFile) {
        if (configuredLogFile == null || configuredLogFile.isBlank()) {
            throw new IllegalStateException("Property logging.file.name must be configured");
        }
        try {
            Path logFilePath = Path.of(configuredLogFile)
                .toAbsolutePath()
                .normalize();

            Path parentDirectory = logFilePath.getParent();
            if (parentDirectory == null) {
                throw new IllegalStateException("Unable to determine the log directory");
            }
            return parentDirectory;
        } catch (InvalidPathException exception) {
            throw new IllegalStateException("Property logging.file.name contains an invalid path", exception);
        }
    }

    private Optional<LogFileMetadata> readMetadata(Path path) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(
                path,
                BasicFileAttributes.class,
                LinkOption.NOFOLLOW_LINKS);

            if (!attributes.isRegularFile()) {
                return Optional.empty();
            }
            return Optional.of(new LogFileMetadata(
                path.getFileName().toString(),
                attributes.size(),
                attributes
                    .lastModifiedTime()
                    .toInstant()));
        } catch (NoSuchFileException exception) {
            log.debug("Log file disappeared during directory scan: {}", path.getFileName());
            return Optional.empty();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read log file attributes",
                exception);
        }
    }
}
