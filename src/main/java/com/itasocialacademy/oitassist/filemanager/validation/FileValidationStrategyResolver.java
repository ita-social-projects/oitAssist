package com.itasocialacademy.oitassist.filemanager.validation;

import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.validation.interfaces.FileValidationStrategy;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileValidationStrategyResolver {
    private final List<FileValidationStrategy> strategies;

    public FileValidationStrategy resolve(RelatedEntityType type) {
        return strategies.stream()
            .filter(s -> s.supports() == type)
            .findFirst()
            .orElseThrow(() -> new UnsupportedOperationException(
                "No file validation strategy registered for: " + type));
    }
}
