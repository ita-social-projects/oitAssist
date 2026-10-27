package com.itasocialacademy.oitassist.filemanager.util;

import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilePathUtilsTest {

    @Test
    void sanitize_validInput_shouldReturnSameString() {
        assertThat(FilePathUtils.sanitize("Normal Task Title")).isEqualTo("Normal Task Title");
    }

    @Test
    void sanitize_illegalChars_shouldReplaceWithUnderscore() {
        assertThat(FilePathUtils.sanitize("Task: How / Why *?")).isEqualTo("Task_ How _ Why __");
        assertThat(FilePathUtils.sanitize("a<b|c>d\"e\\f")).isEqualTo("a_b_c_d_e_f");
    }

    @Test
    void sanitize_leadingAndTrailingDotsAndSpaces_shouldTrim() {
        assertThat(FilePathUtils.sanitize("  ...Task Title...   ")).isEqualTo("Task Title");
    }

    @Test
    void sanitize_innerDots_shouldCollapseConsecutiveDots() {
        assertThat(FilePathUtils.sanitize("Wait... what")).isEqualTo("Wait. what");
        assertThat(FilePathUtils.sanitize("Round 1..2")).isEqualTo("Round 1.2");
    }

    @Test
    void sanitize_longTitle_shouldTruncateTo60Characters() {
        String longTitle = "a".repeat(80);
        String result = FilePathUtils.sanitize(longTitle);
        assertThat(result).hasSize(60).isEqualTo("a".repeat(60));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void sanitize_blankInput_shouldThrowValidationException(String input) {
        assertThatThrownBy(() -> FilePathUtils.sanitize(input))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("must not be blank");
    }

    @ParameterizedTest
    @ValueSource(strings = {"...", "   ...   ", " . . "})
    void sanitize_onlyDotsAndSpaces_shouldThrowValidationException(String input) {
        assertThatThrownBy(() -> FilePathUtils.sanitize(input))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    void extractExtensionWithDot_normalFilename_shouldReturnExtension() {
        assertThat(FilePathUtils.extractExtensionWithDot("photo.jpg")).isEqualTo(".jpg");
        assertThat(FilePathUtils.extractExtensionWithDot("archive.tar.gz")).isEqualTo(".gz");
    }

    @Test
    void extractExtensionWithDot_noExtension_shouldReturnEmpty() {
        assertThat(FilePathUtils.extractExtensionWithDot("file_without_extension")).isEmpty();
        assertThat(FilePathUtils.extractExtensionWithDot(null)).isEmpty();
        assertThat(FilePathUtils.extractExtensionWithDot(".")).isEmpty();
    }
}
