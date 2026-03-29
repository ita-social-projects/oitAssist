package com.itasocialacademy.oitassist.filemanager.providers;

import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

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
}