package com.itasocialacademy.oitassist.logfile.dao;

import com.itasocialacademy.oitassist.logfile.dao.model.LogFileMetadata;
import com.itasocialacademy.oitassist.logfile.exceptions.InvalidLogFileNameException;
import com.itasocialacademy.oitassist.logfile.exceptions.LogFileDownloadException;
import com.itasocialacademy.oitassist.logfile.exceptions.LogFileListingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

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

    @Test
    void shouldFindLogFilesByPartialName() throws IOException {
        Files.writeString(
            tempDirectory.resolve("app.log"),
            "active log");

        Files.writeString(
            tempDirectory.resolve("app.log.2026-07-20.gz"),
            "archived log");

        Files.writeString(
            tempDirectory.resolve("server.log"),
            "server log");

        List<LogFileMetadata> result =
            logFileDao.findByNameContainingIgnoreCase("app");

        assertThat(result)
            .extracting(LogFileMetadata::fileName)
            .containsExactlyInAnyOrder(
                "app.log",
                "app.log.2026-07-20.gz")
            .doesNotContain("server.log");
    }

    @Test
    void shouldFindLogFilesIgnoringCase() throws IOException {
        Files.writeString(
            tempDirectory.resolve("Application.log"),
            "log content");

        List<LogFileMetadata> result =
            logFileDao.findByNameContainingIgnoreCase("APPLICATION");

        assertThat(result)
            .extracting(LogFileMetadata::fileName)
            .containsExactly("Application.log");
    }

    @Test
    void shouldReturnEmptyListWhenNoFileNameMatches()
        throws IOException {

        Files.writeString(
            tempDirectory.resolve("app.log"),
            "log content");

        Files.writeString(
            tempDirectory.resolve("server.log"),
            "server log");

        List<LogFileMetadata> result =
            logFileDao.findByNameContainingIgnoreCase("unknown");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldExcludeDirectoriesMatchingSearchName()
        throws IOException {

        Files.writeString(
            tempDirectory.resolve("app.log"),
            "log content");

        Files.createDirectory(
            tempDirectory.resolve("app-archive"));

        List<LogFileMetadata> result =
            logFileDao.findByNameContainingIgnoreCase("app");

        assertThat(result)
            .extracting(LogFileMetadata::fileName)
            .containsExactly("app.log")
            .doesNotContain("app-archive");
    }

    @Test
    void shouldThrowLogFileListingExceptionWhenDirectoryListingFails(){

        Path logDirectory = tempDirectory;
        String configuredLogFile =
            logDirectory.resolve("app.log").toString();

        FileSystemLogFileDao dao =
            new FileSystemLogFileDao(configuredLogFile);

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.exists(
                logDirectory,
                java.nio.file.LinkOption.NOFOLLOW_LINKS))
                .thenReturn(true);

            filesMock.when(() -> Files.isDirectory(
                logDirectory,
                java.nio.file.LinkOption.NOFOLLOW_LINKS))
                .thenReturn(true);

            filesMock.when(() -> Files.isReadable(logDirectory))
                .thenReturn(true);

            filesMock.when(() -> Files.list(logDirectory))
                .thenThrow(new IOException("Directory listing failed"));

            assertThatThrownBy(dao::findAll)
                .isInstanceOf(LogFileListingException.class);
        }
    }

    @Test
    void shouldThrowLogFileListingExceptionWhenReadingMetadataFails(){

        Path logDirectory = tempDirectory;
        Path logFile = logDirectory.resolve("app.log");

        FileSystemLogFileDao dao =
            new FileSystemLogFileDao(
                logFile.toString());

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.exists(
                logDirectory,
                java.nio.file.LinkOption.NOFOLLOW_LINKS))
                .thenReturn(true);

            filesMock.when(() -> Files.isDirectory(
                logDirectory,
                java.nio.file.LinkOption.NOFOLLOW_LINKS))
                .thenReturn(true);

            filesMock.when(() -> Files.isReadable(logDirectory))
                .thenReturn(true);

            filesMock.when(() -> Files.list(logDirectory))
                .thenReturn(Stream.of(logFile));

            filesMock.when(() -> Files.readAttributes(
                logFile,
                BasicFileAttributes.class,
                java.nio.file.LinkOption.NOFOLLOW_LINKS))
                .thenThrow(new IOException(
                    "Failed to read attributes"));

            assertThatThrownBy(dao::findAll)
                .isInstanceOf(LogFileListingException.class);
        }
    }

    @Test
    void shouldSkipFileWhenItDisappearsDuringDirectoryScan(){

        Path logDirectory = tempDirectory;
        Path logFile = logDirectory.resolve("app.log");

        FileSystemLogFileDao dao =
            new FileSystemLogFileDao(
                logFile.toString());

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.exists(
                logDirectory,
                java.nio.file.LinkOption.NOFOLLOW_LINKS))
                .thenReturn(true);

            filesMock.when(() -> Files.isDirectory(
                logDirectory,
                java.nio.file.LinkOption.NOFOLLOW_LINKS))
                .thenReturn(true);

            filesMock.when(() -> Files.isReadable(logDirectory))
                .thenReturn(true);

            filesMock.when(() -> Files.list(logDirectory))
                .thenReturn(Stream.of(logFile));

            filesMock.when(() -> Files.readAttributes(
                logFile,
                BasicFileAttributes.class,
                java.nio.file.LinkOption.NOFOLLOW_LINKS))
                .thenThrow(
                    new NoSuchFileException(
                        logFile.toString()));

            List<LogFileMetadata> result =
                dao.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Test
    void shouldThrowLogFileListingExceptionWhenLogPathIsNotDirectory()
        throws IOException {

        Path notDirectory = Files.writeString(
            tempDirectory.resolve("not-directory"),
            "content");

        FileSystemLogFileDao dao =
            new FileSystemLogFileDao(
                notDirectory
                    .resolve("app.log")
                    .toString());

        assertThatThrownBy(dao::findAll)
            .isInstanceOf(LogFileListingException.class);
    }

    @Test
    void shouldThrowLogFileListingExceptionWhenDirectoryIsNotReadable() {

        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {

            filesMock.when(() -> Files.exists(
                tempDirectory,
                LinkOption.NOFOLLOW_LINKS))
                .thenReturn(true);

            filesMock.when(() -> Files.isDirectory(
                tempDirectory,
                LinkOption.NOFOLLOW_LINKS))
                .thenReturn(true);

            filesMock.when(() -> Files.isReadable(tempDirectory))
                .thenReturn(false);

            assertThatThrownBy(logFileDao::findAll)
                .isInstanceOf(LogFileListingException.class);
        }
    }

    @Test
    void shouldReturnLogFilePathForDownload() throws IOException {
        Path logFile =
            Files.writeString(
                tempDirectory.resolve("app.log"),
                "test log content");

        Optional<Path> result =
            logFileDao.downloadFile("app.log");

        assertThat(result)
            .contains(
                logFile.toAbsolutePath().normalize());
    }

    @Test
    void shouldReturnEmptyWhenDownloadFileDoesNotExist() {
        Optional<Path> result =
            logFileDao.downloadFile("missing.log");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenDownloadPathIsDirectory()
        throws IOException {

        Files.createDirectory(
            tempDirectory.resolve("archive"));

        Optional<Path> result =
            logFileDao.downloadFile("archive");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenDownloadFileIsSymbolicLink(){

        Path filePath =
            tempDirectory
                .resolve("linked.log")
                .toAbsolutePath()
                .normalize();

        BasicFileAttributes attributes =
            mock(BasicFileAttributes.class);

        when(attributes.isSymbolicLink())
            .thenReturn(true);

        try (MockedStatic<Files> filesMock =
            mockStatic(Files.class)) {

            filesMock.when(
                () -> Files.exists(
                    tempDirectory,
                    LinkOption.NOFOLLOW_LINKS))
                .thenReturn(true);

            filesMock.when(
                () -> Files.isDirectory(
                    tempDirectory,
                    LinkOption.NOFOLLOW_LINKS))
                .thenReturn(true);

            filesMock.when(
                () -> Files.isReadable(tempDirectory))
                .thenReturn(true);

            filesMock.when(
                () -> Files.readAttributes(
                    filePath,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS))
                .thenReturn(attributes);

            Optional<Path> result =
                logFileDao.downloadFile("linked.log");

            assertThat(result).isEmpty();
        }
    }

    @Test
    void shouldRejectDownloadPathOutsideLogDirectory() {
        assertThatThrownBy(
            () -> logFileDao.downloadFile("../secret.log"))
            .isInstanceOf(
                InvalidLogFileNameException.class);
    }

    @Test
    void shouldRejectDownloadFileFromNestedDirectory()
        throws IOException {

        Path archiveDirectory =
            Files.createDirectory(
                tempDirectory.resolve("archive"));

        Files.writeString(
            archiveDirectory.resolve("app.log"),
            "archived log");

        assertThatThrownBy(
            () -> logFileDao.downloadFile(
                "archive/app.log"))
            .isInstanceOf(
                InvalidLogFileNameException.class);
    }

    @Test
    void shouldThrowLogFileDownloadExceptionWhenReadingFileAttributesFails() {
        Path filePath =
            tempDirectory
                .resolve("app.log")
                .toAbsolutePath()
                .normalize();

        IOException cause =
            new IOException(
                "Unable to read file attributes");

        try (MockedStatic<Files> filesMock =
            mockStatic(Files.class)) {

            filesMock.when(
                () -> Files.exists(
                    tempDirectory,
                    LinkOption.NOFOLLOW_LINKS))
                .thenReturn(true);

            filesMock.when(
                () -> Files.isDirectory(
                    tempDirectory,
                    LinkOption.NOFOLLOW_LINKS))
                .thenReturn(true);

            filesMock.when(
                () -> Files.isReadable(
                    tempDirectory))
                .thenReturn(true);

            filesMock.when(
                () -> Files.readAttributes(
                    filePath,
                    BasicFileAttributes.class,
                    LinkOption.NOFOLLOW_LINKS))
                .thenThrow(cause);

            assertThatThrownBy(
                () -> logFileDao.downloadFile("app.log"))
                .isInstanceOf(
                    LogFileDownloadException.class)
                .hasCause(cause);
        }
    }

    @Test
    void shouldThrowInvalidLogFileNameExceptionForInvalidPath() {
        assertThatThrownBy(
            () -> logFileDao.downloadFile("\u0000"))
            .isInstanceOf(
                InvalidLogFileNameException.class);
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