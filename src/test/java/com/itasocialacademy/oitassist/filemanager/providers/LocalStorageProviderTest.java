package com.itasocialacademy.oitassist.filemanager.providers;

import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileDeleteException;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileListingException;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileUploadException;
import com.itasocialacademy.oitassist.filemanager.exceptions.InvalidFilePathException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
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
        String relativeKey = "test-file.txt";
        Path fileToDelete = tempDir.resolve(relativeKey);
        Files.createFile(fileToDelete);

        assertTrue(Files.exists(fileToDelete), "File should exist before deletion");

        localStorageProvider.deletePhysical(relativeKey);

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
            .isInstanceOf(InvalidFilePathException.class)
            .hasMessageContaining("Access denied: path is outside storage root");
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

    //
    // listAllPhysicalKeys() SECTION TESTS
    //

    @Test
    void listAllPhysicalKeys_ShouldReturnAllFilesRecursivelyWithForwardSlashes() throws IOException {
        Files.createFile(tempDir.resolve("file1.txt"));
        Path newsDir = Files.createDirectories(tempDir.resolve("news"));
        Files.createFile(newsDir.resolve("image.png"));
        Path docsDir = Files.createDirectories(tempDir.resolve("task"));
        Files.createFile(docsDir.resolve("resume.pdf"));

        List<String> keys = localStorageProvider.listAllPhysicalKeys();

        assertThat(keys)
            .hasSize(3)
            .containsExactlyInAnyOrder(
                "file1.txt",
                "news/image.png",
                "task/resume.pdf")
            .allSatisfy(key -> assertThat(key)
                .as("Key '%s' should not contain backslashes", key)
                .doesNotContain("\\"));
    }

    @Test
    void listAllPhysicalKeys_ShouldThrowFileListingException_WhenRootDoesNotExist() {
        LocalStorageProvider badProvider = new LocalStorageProvider(tempDir.resolve("ghost").toString());

        assertThatThrownBy(badProvider::listAllPhysicalKeys)
            .isInstanceOf(FileListingException.class)
            .hasMessageContaining("Failed to walk local storage directory");
    }

    //
    // getLastModified() SECTION TESTS
    //

    @Test
    void getLastModified_ShouldReturnCorrectTimeInUtc() throws IOException {
        String fileName = "time-test.txt";
        Path file = tempDir.resolve(fileName);
        Files.createFile(file);

        OffsetDateTime result = localStorageProvider.getLastModified(fileName);

        assertThat(result).isNotNull();
        assertThat(result.getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(result.toInstant()).isBeforeOrEqualTo(OffsetDateTime.now().toInstant());
    }

    @Test
    void getLastModified_ShouldThrowException_WhenPathIsOutsideRoot() {
        String maliciousKey = "../outside.txt";

        assertThatThrownBy(() -> localStorageProvider.getLastModified(maliciousKey))
            .isInstanceOf(InvalidFilePathException.class)
            .hasMessageContaining("Access denied: path is outside storage root");
    }

    @Test
    void getLastModified_ShouldThrowFileListingException_WhenFileDoesNotExist() {
        assertThatThrownBy(() -> localStorageProvider.getLastModified("non-existent.txt"))
            .isInstanceOf(FileListingException.class)
            .hasMessageContaining("Could not read file metadata");
    }

    //
    // moveFile() SECTION TESTS
    //

    @Test
    void moveFile_ShouldMoveFileSuccessfully() throws IOException {
        Path sourceDir = tempDir.resolve("tasks/task1/problem");
        Files.createDirectories(sourceDir);
        Path source = sourceDir.resolve("doc.txt");
        Files.writeString(source, "hello world");

        String moved = localStorageProvider.moveFile(
            "tasks/task1/problem/doc.txt", "tasks/task1/solution/doc.txt");

        assertThat(moved).isEqualTo("tasks/task1/solution/doc.txt");
        assertThat(Files.exists(source)).isFalse();
        Path target = tempDir.resolve("tasks/task1/solution/doc.txt");
        assertThat(Files.exists(target)).isTrue();
        assertThat(Files.readString(target)).isEqualTo("hello world");
    }

    @Test
    void moveFile_ShouldThrowInvalidFilePathException_WhenKeyIsBlank() {
        assertThrows(InvalidFilePathException.class,
            () -> localStorageProvider.moveFile(null, "target.txt"));
        assertThrows(InvalidFilePathException.class,
            () -> localStorageProvider.moveFile("source.txt", "   "));
    }

    @Test
    void moveFile_ShouldThrowInvalidFilePathException_WhenPathIsOutsideRoot() {
        assertThatThrownBy(() -> localStorageProvider.moveFile("source.txt", "../outside.txt"))
            .isInstanceOf(InvalidFilePathException.class)
            .hasMessageContaining("Access denied: path is outside storage root");
    }

    //
    // createTaskReference() & deleteTaskReference() SECTION TESTS
    //

    @Test
    void createTaskReference_ShouldThrowInvalidFilePathException_WhenPathsAreBlank() {
        assertThrows(InvalidFilePathException.class,
            () -> localStorageProvider.createTaskReference("", "tasks/task1"));
        assertThrows(InvalidFilePathException.class,
            () -> localStorageProvider.createTaskReference("olympiads/task1", null));
    }

    @Test
    void createTaskReference_ShouldThrowInvalidFilePathException_WhenPathIsOutsideRoot() {
        assertThatThrownBy(() -> localStorageProvider.createTaskReference("../outside", "tasks/task1"))
            .isInstanceOf(InvalidFilePathException.class)
            .hasMessageContaining("Access denied: path is outside storage root");
    }

    @Test
    void createTaskReference_andDeleteTaskReference_ShouldManageSymlinkLifecycle() throws IOException {
        Path taskDir = tempDir.resolve("tasks/task1");
        Files.createDirectories(taskDir);

        localStorageProvider.createTaskReference("olympiads/comp/stage/tour/task1", "tasks/task1");

        Path link = tempDir.resolve("olympiads/comp/stage/tour/task1/task");
        if (Files.isSymbolicLink(link)) {
            localStorageProvider.deleteTaskReference("olympiads/comp/stage/tour/task1");
            assertThat(Files.exists(link)).isFalse();
        }
    }

    @Test
    void createTaskReference_ShouldCreateTargetDirectoryAndBeIdempotent() {
        Path target = tempDir.resolve("tasks/non_existent_target");
        assertThat(Files.exists(target)).isFalse();

        localStorageProvider.createTaskReference("olympiads/comp/stage/tour/task1", "tasks/non_existent_target");
        assertThat(Files.isDirectory(target)).isTrue();

        // Calling again should not throw FileAlreadyExistsException
        localStorageProvider.createTaskReference("olympiads/comp/stage/tour/task1", "tasks/non_existent_target");
        Path link = tempDir.resolve("olympiads/comp/stage/tour/task1/task");
        if (Files.isSymbolicLink(link)) {
            assertThat(Files.exists(link)).isTrue();
        }
    }

    @Test
    void deleteTaskReference_ShouldThrowInvalidFilePathException_WhenPathIsBlankOrOutsideRoot() {
        assertThrows(InvalidFilePathException.class,
            () -> localStorageProvider.deleteTaskReference("   "));
        assertThrows(InvalidFilePathException.class,
            () -> localStorageProvider.deleteTaskReference(null));
        assertThatThrownBy(() -> localStorageProvider.deleteTaskReference("../outside"))
            .isInstanceOf(InvalidFilePathException.class)
            .hasMessageContaining("Access denied: path is outside storage root");
    }
}