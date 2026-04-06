package com.itasocialacademy.oitassist.filemanager.providers;

import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileDeleteException;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileUploadException;
import com.itasocialacademy.oitassist.filemanager.exceptions.InvalidFilePathException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

class LocalStorageProviderTest {

    private LocalStorageProvider localStorageProvider;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        localStorageProvider = new LocalStorageProvider(tempDir.toString());
    }

    //
    // getType() SECTION TESTS
    //

    @Test
    void getType_ShouldReturnLocal() {
        assertThat(localStorageProvider.getType()).isEqualTo(StorageProviderType.LOCAL);
    }

    //
    // supports() SECTION TESTS
    //

    @Test
    void supports_ShouldReturnTrue_WhenTypeIsLocal() {
        assertTrue(localStorageProvider.supports(StorageProviderType.LOCAL));
    }

    @Test
    void supports_ShouldReturnFalse_WhenTypeIsNotLocal() {
        assertFalse(localStorageProvider.supports(StorageProviderType.SHAREPOINT));
    }

    //
    // deletePhysical() SECTION TESTS
    //

    @Test
    void deletePhysical_ShouldDeleteFileUsingRelativeStorageKey() throws IOException {
        Path subDir = tempDir.resolve("news");
        Files.createDirectories(subDir);
        Path file = subDir.resolve("article.jpg");
        Files.createFile(file);

        localStorageProvider.deletePhysical("news/article.jpg");

        assertFalse(Files.exists(file), "File should be deleted using relative storage key");
    }

    @Test
    void deletePhysical_ShouldHandleRelativePath() throws IOException {
        Path subDir = tempDir.resolve("news");
        Files.createDirectories(subDir);
        Path file = subDir.resolve("article.jpg");
        Files.createFile(file);

        localStorageProvider.deletePhysical("news/article.jpg");

        assertFalse(Files.exists(file), "Should delete file using relative path resolution");
    }

    @Test
    void deletePhysical_ShouldThrowException_WhenPathIsOutsideRoot() {
        String maliciousKey = "../secret.txt";

        assertThatThrownBy(() -> localStorageProvider.deletePhysical(maliciousKey))
            .isInstanceOf(InvalidFilePathException.class)
            .hasMessageContaining("Access denied: path is outside storage root");
    }

    @Test
    void deletePhysical_ShouldThrowException_WhenKeyIsBlank() {
        assertThrows(InvalidFilePathException.class, () -> localStorageProvider.deletePhysical("   "));
        assertThrows(InvalidFilePathException.class, () -> localStorageProvider.deletePhysical(null));
    }

    @Test
    void deletePhysical_ShouldThrowFileDeleteException_WhenFileSystemFails() throws IOException {
        Path file = tempDir.resolve("protected.txt");
        Files.createFile(file);

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.deleteIfExists(any(Path.class)))
                .thenThrow(new IOException("Lock error"));

            assertThatThrownBy(() -> localStorageProvider.deletePhysical("protected.txt"))
                .isInstanceOf(FileDeleteException.class)
                .hasMessageContaining("Could not delete physical file");
        }
    }

    @Test
    void deletePhysical_ShouldDeleteExistingFile() throws IOException {
        Path fileToDelete = tempDir.resolve("test-file.txt");
        Files.createFile(fileToDelete);
        String absolutePath = fileToDelete.toAbsolutePath().toString();

        assertTrue(Files.exists(fileToDelete), "File should exist before deletion");

        localStorageProvider.deletePhysical(absolutePath);

        assertFalse(Files.exists(fileToDelete), "File should be physically deleted");
    }

    @Test
    void deletePhysical_ShouldNotThrowException_WhenFileDoesNotExist() {
        assertDoesNotThrow(() -> localStorageProvider.deletePhysical("non-existent/file.txt"),
            "Should handle non-existent files gracefully");
    }

    //
    // upload() SECTION TESTS
    //

    @Test
    void upload_ShouldSaveFileAndReturnRelativePathWithForwardSlashes() throws IOException {
        String fileName = "test-image.png";
        String subPath = "uploads/avatars"; // Using forward slashes in input
        String content = "fake-image-content";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());

        String resultKey = localStorageProvider.upload(inputStream, fileName, subPath);

        // Assert: The returned key must be relative and use forward slashes
        assertThat(resultKey).isEqualTo("uploads/avatars/test-image.png");

        // Verify physical file existence
        Path expectedFile = tempDir.resolve("uploads/avatars").resolve(fileName);
        assertThat(Files.exists(expectedFile)).isTrue();
        assertThat(Files.readString(expectedFile)).isEqualTo(content);
    }

    @Test
    void upload_ShouldThrowException_WhenPathAttemptsTraversal() {
        InputStream content = new ByteArrayInputStream("data".getBytes());

        assertThatThrownBy(() -> localStorageProvider.upload(content, "hack.exe", "../../etc"))
            .isInstanceOf(FileUploadException.class)
            .hasMessageContaining("Invalid upload path outside configured storage root");
    }

    @Test
    void upload_ShouldOverwrite_WhenFileAlreadyExists() throws IOException {
        String fileName = "overwrite.txt";
        String subPath = "docs";
        Path dir = tempDir.resolve(subPath);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(fileName), "old content");

        InputStream newContent = new ByteArrayInputStream("new content".getBytes());

        localStorageProvider.upload(newContent, fileName, subPath);

        assertThat(Files.readString(dir.resolve(fileName))).isEqualTo("new content");
    }

    @Test
    void upload_ShouldThrowException_WhenIOExceptionOccurs() {
        InputStream inputStream = new ByteArrayInputStream("data".getBytes());

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createDirectories(any(Path.class)))
                .thenThrow(new IOException("Disk Full"));

            assertThatThrownBy(() -> localStorageProvider.upload(inputStream, "fail.txt", "any/path"))
                .isInstanceOf(FileUploadException.class)
                .hasMessageContaining("Could not store file locally")
                .hasCauseInstanceOf(IOException.class);
        }
    }
}