package com.itasocialacademy.oitassist.filemanager.util;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;

public final class FilePathUtils {
    private static final String ILLEGAL_CHARS = "[\\\\/:*?\"<>|\\x00-\\x1F]";

    private FilePathUtils() {
    }

    /**
     * Sanitizes a title for use as a filesystem folder name. Replaces characters
     * illegal on Windows (\ / : * ? " < > |), control characters, and trims
     * leading/trailing dots and spaces.
     */
    public static String sanitize(String title) {
        if (title == null || title.isBlank()) {
            throw new ValidationException(
                "Title must not be blank", ErrorCode.FILE_VALIDATION_FAILED);
        }
        String safe = title.replaceAll(ILLEGAL_CHARS, "_");
        safe = safe.replaceAll("^[.\\s]+", "");
        safe = safe.replaceAll("[.\\s]+$", "");
        if (safe.isBlank()) {
            throw new ValidationException(
                "Title contains only illegal characters: " + title,
                ErrorCode.FILE_VALIDATION_FAILED);
        }
        return safe.length() > 60 ? safe.substring(0, 60).trim() : safe;
    }

    /**
     * Extracts extension with dot from a filename. Returns "" if none.
     */
    public static String extractExtensionWithDot(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot) : "";
    }
}
