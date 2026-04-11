package com.itasocialacademy.oitassist.filemanager.validation;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.validation.interfaces.FileValidationStrategy;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FileValidationStrategyResolver {
    private final List<FileValidationStrategy> strategies;

    /**
     * Resolves the {@link FileValidationStrategy} registered for the given entity
     * type.
     *
     * @param type the entity type for which a validation strategy is required
     * @return the matching {@link FileValidationStrategy}
     * @throws com.itasocialacademy.oitassist.core.exceptions.ValidationException if
     *                                                                            no
     *                                                                            strategy
     *                                                                            is
     *                                                                            registered
     *                                                                            for
     *                                                                            the
     *                                                                            given
     *                                                                            type
     */
    public FileValidationStrategy resolve(RelatedEntityType type) {
        return strategies.stream()
            .filter(s -> s.supports() == type)
            .findFirst()
            .orElseThrow(() -> new ValidationException(
                "No file validation strategy registered for: " + type,
                ErrorCode.FILE_VALIDATION_FAILED));
    }
}
