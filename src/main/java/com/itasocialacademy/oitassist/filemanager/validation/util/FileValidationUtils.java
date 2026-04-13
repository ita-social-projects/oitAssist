package com.itasocialacademy.oitassist.filemanager.validation.util;

import com.itasocialacademy.oitassist.filemanager.validation.enums.AllowedExtension;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.unit.DataSize;

public class FileValidationUtils {
    private FileValidationUtils() {
    }

    /**
     * Extracts the file extension (without the dot) from the given filename.
     *
     * @param filename the original filename
     * @return the lowercase extension, or an empty string if none is present
     */
    public static String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * Checks whether the given extension is not present in the set of allowed
     * extensions.
     *
     * @param ext     the file extension to check (without dot, lowercase)
     * @param allowed the set of permitted extensions
     * @return {@code true} if the extension is not allowed
     */
    public static boolean isExtensionNotAllowed(String ext, Set<AllowedExtension> allowed) {
        return allowed.stream().noneMatch(e -> e.getRawValue().equals(ext));
    }

    /**
     * Formats the set of allowed extensions as a human-readable comma-separated
     * string.
     *
     * @param allowed the set of permitted extensions
     * @return a comma-separated list of extension values (e.g.,
     *         {@code "jpg, png, gif"})
     */
    public static String formatAllowed(Set<AllowedExtension> allowed) {
        return allowed.stream()
            .map(AllowedExtension::getRawValue)
            .collect(Collectors.joining(", "));
    }

    /**
     * Checks whether the file size exceeds the configured maximum.
     *
     * @param fileSize the actual file size in bytes
     * @param maxSize  the maximum allowed size
     * @return {@code true} if the file size exceeds the limit
     */
    public static boolean isFileSizeExceeded(long fileSize, DataSize maxSize) {
        return fileSize > maxSize.toBytes();
    }

    /**
     * Formats a {@link DataSize} as a megabyte string (e.g., {@code "10MB"}).
     *
     * @param size the size to format
     * @return the size expressed in megabytes followed by "MB"
     */
    public static String formatSize(DataSize size) {
        return size.toMegabytes() + "MB";
    }

    /**
     * Extracts the filename without its extension.
     *
     * @param filename the original filename
     * @return the portion of the name before the last dot, or the full filename if
     *         no extension is present
     */
    public static String extractNameWithoutExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return filename;
        }
        return filename.substring(0, filename.lastIndexOf('.'));
    }
}
