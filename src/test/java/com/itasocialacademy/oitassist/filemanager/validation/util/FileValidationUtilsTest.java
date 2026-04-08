package com.itasocialacademy.oitassist.filemanager.validation.util;

import com.itasocialacademy.oitassist.filemanager.validation.enums.AllowedExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.util.unit.DataSize;
import java.util.Set;

import static com.itasocialacademy.oitassist.filemanager.validation.util.FileValidationUtils.extractExtension;
import static com.itasocialacademy.oitassist.filemanager.validation.util.FileValidationUtils.extractNameWithoutExtension;
import static com.itasocialacademy.oitassist.filemanager.validation.util.FileValidationUtils.formatAllowed;
import static com.itasocialacademy.oitassist.filemanager.validation.util.FileValidationUtils.formatSize;
import static com.itasocialacademy.oitassist.filemanager.validation.util.FileValidationUtils.isExtensionNotAllowed;
import static com.itasocialacademy.oitassist.filemanager.validation.util.FileValidationUtils.isFileSizeExceeded;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileValidationUtilsTest {

    @ParameterizedTest(name = "extractExtension(\"{0}\") -> \"{1}\"")
    @CsvSource({
        "photo.jpg,      jpg",
        "archive.tar.gz, gz",
        "PHOTO.JPG,      jpg",
        "file.,          ''",
        "filename,       ''"
    })
    void extractExtension_ShouldReturnExpectedResult_WhenFilenameGiven(String filename, String expected) {
        assertEquals(expected, extractExtension(filename));
    }

    @Test
    void extractExtension_ShouldReturnEmpty_WhenFilenameIsNull() {
        assertEquals("", extractExtension(null));
    }

    @Test
    void extractExtension_ShouldReturnEmpty_WhenFilenameIsEmpty() {
        assertEquals("", extractExtension(""));
    }

    // --- extractNameWithoutExtension ---

    @ParameterizedTest(name = "extractNameWithoutExtension(\"{0}\") -> \"{1}\"")
    @CsvSource({
        "photo.jpg,      photo",
        "archive.tar.gz, archive.tar",
        "file.,          file",
        "filename,       filename"
    })
    void extractNameWithoutExtension_ShouldReturnExpectedResult_WhenFilenameGiven(String filename, String expected) {
        assertEquals(expected, extractNameWithoutExtension(filename));
    }

    @Test
    void extractNameWithoutExtension_ShouldReturnNull_WhenFilenameIsNull() {
        assertNull(extractNameWithoutExtension(null));
    }

    @Test
    void extractNameWithoutExtension_ShouldReturnEmpty_WhenFilenameIsEmpty() {
        assertEquals("", extractNameWithoutExtension(""));
    }

    // --- isExtensionNotAllowed ---

    @Test
    void isExtensionNotAllowed_ShouldReturnFalse_WhenExtensionIsInAllowedSet() {
        assertFalse(isExtensionNotAllowed("jpg", Set.of(AllowedExtension.JPG)));
    }

    @Test
    void isExtensionNotAllowed_ShouldReturnTrue_WhenExtensionIsNotInAllowedSet() {
        assertTrue(isExtensionNotAllowed("txt", Set.of(AllowedExtension.JPG, AllowedExtension.PNG)));
    }

    @Test
    void isExtensionNotAllowed_ShouldReturnTrue_WhenAllowedSetIsEmpty() {
        assertTrue(isExtensionNotAllowed("jpg", Set.of()));
    }

    // --- formatAllowed ---

    @Test
    void formatAllowed_ShouldReturnEmpty_WhenExtensionSetIsEmpty() {
        assertEquals("", formatAllowed(Set.of()));
    }

    @Test
    void formatAllowed_ShouldReturnRawValue_WhenSingleExtensionProvided() {
        assertEquals("jpg", formatAllowed(Set.of(AllowedExtension.JPG)));
    }

    @Test
    void formatAllowed_ShouldReturnCommaSeparatedRawValues_WhenMultipleExtensionsProvided() {
        String result = formatAllowed(Set.of(AllowedExtension.JPG, AllowedExtension.PNG));

        Set<String> actual = Set.of(result.split(", "));
        assertEquals(Set.of("jpg", "png"), actual);
    }

    // --- isFileSizeExceeded ---

    @Test
    void isFileSizeExceeded_ShouldReturnFalse_WhenFileSizeIsBelowLimit() {
        assertFalse(isFileSizeExceeded(DataSize.ofMegabytes(1).toBytes() - 1, DataSize.ofMegabytes(1)));
    }

    @Test
    void isFileSizeExceeded_ShouldReturnFalse_WhenFileSizeEqualsLimit() {
        long limit = DataSize.ofMegabytes(1).toBytes();

        assertFalse(isFileSizeExceeded(limit, DataSize.ofMegabytes(1)));
    }

    @Test
    void isFileSizeExceeded_ShouldReturnTrue_WhenFileSizeExceedsLimitByOneByte() {
        long overLimit = DataSize.ofMegabytes(1).toBytes() + 1;

        assertTrue(isFileSizeExceeded(overLimit, DataSize.ofMegabytes(1)));
    }

    // --- formatSize ---

    @ParameterizedTest(name = "formatSize({0}MB) -> \"{1}\"")
    @CsvSource({
        "1,  1MB",
        "10, 10MB",
        "50, 50MB"
    })
    void formatSize_ShouldReturnSizeWithMbSuffix_WhenCalled(long megabytes, String expected) {
        assertEquals(expected, formatSize(DataSize.ofMegabytes(megabytes)));
    }
}
