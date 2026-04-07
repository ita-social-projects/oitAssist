package com.itasocialacademy.oitassist.filemanager.validation.interfaces;

import com.itasocialacademy.oitassist.filemanager.validation.enums.AllowedExtension;
import java.util.Optional;
import java.util.Set;
import org.springframework.util.unit.DataSize;

public interface FilePolicy {
    Set<AllowedExtension> getAllowedExtensions();

    int getMaxFileCount();

    DataSize getMaxFileSize();

    default Optional<String> getRequiredFileName() {
        return Optional.empty();
    }
}
