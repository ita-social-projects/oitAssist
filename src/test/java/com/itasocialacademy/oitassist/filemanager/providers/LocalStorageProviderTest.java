package com.itasocialacademy.oitassist.filemanager.providers;

import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileLocalUploadFailureException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

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
        ReflectionTestUtils.setField(localStorageProvider, "rootPath", tempDir.toString());
    }

    @Test
    void supports_ShouldReturnTrue_WhenTypeIsLocal() {
        assertTrue(localStorageProvider.supports(StorageProviderType.LOCAL));
    }

    @Test
    void supports_ShouldReturnFalse_WhenTypeIsNotLocal() {
        assertFalse(localStorageProvider.supports(StorageProviderType.SHAREPOINT));
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
        String nonExistentPath = tempDir.resolve("non-existent.txt").toString();

        assertDoesNotThrow(() -> localStorageProvider.deletePhysical(nonExistentPath),
            "Should handle non-existent files gracefully without throwing exceptions");
    }

    @Test
    void upload_ShouldSaveFileAndReturnPath_WhenInputIsValid() throws IOException {
        String fileName = "test-image.png";
        String subPath = "uploads/avatars";
        String content = "fake-image-content";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());

        String resultPath = localStorageProvider.upload(inputStream, fileName, subPath);

        Path expectedFile = tempDir.resolve(subPath).resolve(fileName);

        assertThat(resultPath).isEqualTo(expectedFile.toString());
        assertThat(Files.exists(expectedFile)).isTrue();
        assertThat(Files.readString(expectedFile)).isEqualTo(content);
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
                .isInstanceOf(FileLocalUploadFailureException.class)
                .hasMessageContaining("Could not store file locally")
                .hasCauseInstanceOf(IOException.class);
        }
    }
}