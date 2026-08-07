package com.itasocialacademy.oitassist.logfile.dao;

import com.itasocialacademy.oitassist.logfile.exceptions.LogFileListingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class FileSystemLogFileDaoTest {

    @TempDir
    Path tempDirectory;

    private FileSystemLogFileDao logFileDao;

    @BeforeEach
    void setUp() {
        String configuredLogFile = tempDirectory
            .resolve("app.log")
            .toString();

        logFileDao =
            new FileSystemLogFileDao(configuredLogFile);
    }

    @Test
    void shouldReturnMetadataForRegularFiles()
        throws IOException {

        Instant firstModified =
            Instant.parse("2026-07-20T10:00:00Z");

        Instant secondModified =
            Instant.parse("2026-07-21T12:00:00Z");

        Path activeLogFile = Files.writeString(
            tempDirectory.resolve("app.log"),
            "active log content");

        Path archivedLogFile = Files.writeString(
            tempDirectory.resolve(
                "app.log.2026-07-20.log.gz"),
            "archived log content");

        Files.setLastModifiedTime(
            activeLogFile,
            FileTime.from(firstModified));

        Files.setLastModifiedTime(
            archivedLogFile,
            FileTime.from(secondModified));

        List<LogFileMetadata> result =
            logFileDao.findAll();

        assertThat(result).hasSize(2);

        LogFileMetadata activeMetadata =
            findByFileName(result, "app.log");

        assertThat(activeMetadata.fileName())
            .isEqualTo("app.log");

        assertThat(activeMetadata.size())
            .isEqualTo(Files.size(activeLogFile));

        assertThat(
            activeMetadata
                .lastModified()
                .getEpochSecond())
            .isEqualTo(firstModified.getEpochSecond());

        LogFileMetadata archivedMetadata =
            findByFileName(
                result,
                "app.log.2026-07-20.log.gz");

        assertThat(archivedMetadata.fileName())
            .isEqualTo(
                "app.log.2026-07-20.log.gz");

        assertThat(archivedMetadata.size())
            .isEqualTo(Files.size(archivedLogFile));

        assertThat(
            archivedMetadata
                .lastModified()
                .getEpochSecond())
            .isEqualTo(secondModified.getEpochSecond());
    }

    @Test
    void shouldExcludeDirectories() throws IOException {
        Files.writeString(
            tempDirectory.resolve("app.log"),
            "log content");

        Files.createDirectory(
            tempDirectory.resolve("archive"));

        List<LogFileMetadata> result =
            logFileDao.findAll();

        assertThat(result)
            .extracting(LogFileMetadata::fileName)
            .containsExactly("app.log")
            .doesNotContain("archive");
    }

    @Test
    void shouldReturnEmptyListWhenDirectoryIsEmpty() {
        List<LogFileMetadata> result =
            logFileDao.findAll();

        assertThat(result).isEmpty();
    }

    @Test
    void shouldThrowLogFileListingExceptionWhenDirectoryDoesNotExist() {
        Path missingDirectory =
            tempDirectory.resolve("missing");

        FileSystemLogFileDao dao =
            new FileSystemLogFileDao(
                missingDirectory
                    .resolve("app.log")
                    .toString());

        assertThatThrownBy(dao::findAll)
            .isInstanceOf(
                LogFileListingException.class);
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenConfiguredPathIsBlank() {
        assertThatThrownBy(
            () -> new FileSystemLogFileDao(" "))
            .isInstanceOf(
                IllegalStateException.class)
            .hasMessageContaining(
                "logging.file.name must be configured");
    }

    private static LogFileMetadata findByFileName(
        List<LogFileMetadata> files,
        String fileName) {
        return files.stream()
            .filter(
                file -> file.fileName().equals(fileName))
            .findFirst()
            .orElseThrow();
    }
}