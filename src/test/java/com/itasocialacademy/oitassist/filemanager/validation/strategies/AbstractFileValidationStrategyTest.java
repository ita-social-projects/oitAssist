package com.itasocialacademy.oitassist.filemanager.validation.strategies;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.dto.request.FileUploadRequestDto;
import com.itasocialacademy.oitassist.filemanager.validation.enums.AllowedExtension;
import com.itasocialacademy.oitassist.filemanager.validation.interfaces.FilePolicy;
import com.itasocialacademy.oitassist.filemanager.validation.model.ValidationResult;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class AbstractFileValidationStrategyTest {

    @Mock
    private FilePolicy policy;

    private final AbstractFileValidationStrategy strategy = new TestFileValidationStrategy();

    @BeforeEach
    void setUp() {
        lenient().when(policy.getMaxFileCount()).thenReturn(5);
        lenient().when(policy.getAllowedExtensions()).thenReturn(Set.of(AllowedExtension.JPG));
        lenient().when(policy.getMaxFileSize()).thenReturn(DataSize.ofMegabytes(10));
        lenient().when(policy.getRequiredFileName()).thenReturn(Optional.empty());
    }

    @Test
    void validate_ShouldReturnOk_WhenAllFilesAreValid() {
        // Arrange
        List<MultipartFile> files = List.of(file("photo.jpg", 1024));
        FileUploadRequestDto request = request();

        // Act
        ValidationResult result = strategy.validate(files, request, policy);

        // Assert
        assertAll(
            () -> assertTrue(result.valid()),
            () -> assertTrue(result.violations().isEmpty()));
    }

    @Test
    void validate_ShouldReturnFailure_WhenFileCountExceedsMaximum() {
        // Arrange
        when(policy.getMaxFileCount()).thenReturn(1);
        List<MultipartFile> files = List.of(file("a.jpg", 1024), file("b.jpg", 1024));
        FileUploadRequestDto request = request();

        // Act
        ValidationResult result = strategy.validate(files, request, policy);

        // Assert
        assertAll(
            () -> assertFalse(result.valid()),
            () -> assertTrue(result.violations().stream()
                .anyMatch(v -> v.contains("at most 1 files") && v.contains("got 2"))));
    }

    @Test
    void validate_ShouldReturnViolation_WhenFileExtensionIsNotAllowed() {
        // Arrange
        List<MultipartFile> files = List.of(file("document.txt", 1024));
        FileUploadRequestDto request = request();

        // Act
        ValidationResult result = strategy.validate(files, request, policy);

        // Assert
        assertAll(
            () -> assertFalse(result.valid()),
            () -> assertTrue(result.violations().stream()
                .anyMatch(v -> v.contains("document.txt") && v.contains("unsupported extension"))));
    }

    @Test
    void validate_ShouldReturnViolation_WhenFileSizeExceedsLimit() {
        // Arrange
        when(policy.getMaxFileSize()).thenReturn(DataSize.ofMegabytes(1));
        long twoMegabytes = DataSize.ofMegabytes(2).toBytes();
        List<MultipartFile> files = List.of(file("photo.jpg", (int) twoMegabytes));
        FileUploadRequestDto request = request();

        // Act
        ValidationResult result = strategy.validate(files, request, policy);

        // Assert
        assertAll(
            () -> assertFalse(result.valid()),
            () -> assertTrue(result.violations().stream()
                .anyMatch(v -> v.contains("photo.jpg") && v.contains("exceeds maximum allowed size"))));
    }

    @Test
    void validate_ShouldReturnViolation_WhenFilenameDoesNotMatchRequired() {
        // Arrange
        when(policy.getRequiredFileName()).thenReturn(Optional.of("report"));
        List<MultipartFile> files = List.of(file("photo.jpg", 1024));
        FileUploadRequestDto request = request();

        // Act
        ValidationResult result = strategy.validate(files, request, policy);

        // Assert
        assertAll(
            () -> assertFalse(result.valid()),
            () -> assertTrue(result.violations().stream()
                .anyMatch(v -> v.contains("must be 'report'") && v.contains("got 'photo'"))));
    }

    @Test
    void validate_ShouldAggregateViolations_WhenMultipleRulesAreBroken() {
        // Arrange
        when(policy.getMaxFileSize()).thenReturn(DataSize.ofMegabytes(1));
        long twoMegabytes = DataSize.ofMegabytes(2).toBytes();
        List<MultipartFile> files = List.of(file("document.txt", (int) twoMegabytes));
        FileUploadRequestDto request = request();

        // Act
        ValidationResult result = strategy.validate(files, request, policy);

        // Assert
        assertAll(
            () -> assertFalse(result.valid()),
            () -> assertEquals(2, result.violations().size()),
            () -> assertTrue(result.violations().stream()
                .anyMatch(v -> v.contains("unsupported extension"))),
            () -> assertTrue(result.violations().stream()
                .anyMatch(v -> v.contains("exceeds maximum allowed size"))));
    }

    // --- helpers ---

    private static MockMultipartFile file(String filename, int sizeBytes) {
        return new MockMultipartFile("file", filename, "application/octet-stream", new byte[sizeBytes]);
    }

    private static FileUploadRequestDto request() {
        return FileUploadRequestDto.builder()
            .relatedEntityType(RelatedEntityType.NEWS)
            .relatedEntityId(1L)
            .build();
    }

    // --- inner test implementation ---

    private static class TestFileValidationStrategy extends AbstractFileValidationStrategy {
        @Override
        public boolean supports(RelatedEntityType entityType, FileRole role) {
            return true; // irrelevant to these tests — validate() is exercised directly
        }
    }
}