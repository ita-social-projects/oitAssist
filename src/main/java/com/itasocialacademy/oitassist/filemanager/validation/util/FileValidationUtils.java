package com.itasocialacademy.oitassist.filemanager.validation.util;

import com.itasocialacademy.oitassist.filemanager.validation.enums.AllowedExtension;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.unit.DataSize;

public class FileValidationUtils {
    private FileValidationUtils() {
    }

    public static String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    public static boolean isExtensionNotAllowed(String ext, Set<AllowedExtension> allowed) {
        return allowed.stream().noneMatch(e -> e.getRawValue().equals(ext));
    }

    public static String formatAllowed(Set<AllowedExtension> allowed) {
        return allowed.stream()
            .map(AllowedExtension::getRawValue)
            .collect(Collectors.joining(", "));
    }

    public static boolean isFileSizeExceeded(long fileSize, DataSize maxSize) {
        return fileSize > maxSize.toBytes();
    }

    public static String formatSize(DataSize size) {
        return size.toMegabytes() + "MB";
    }

    public static String extractNameWithoutExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return filename;
        }
        return filename.substring(0, filename.lastIndexOf('.'));
    }
}
