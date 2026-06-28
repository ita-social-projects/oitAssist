package com.itasocialacademy.oitassist.filemanager.validation;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
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
     * type and file role.
     *
     * @param type the entity type for which a validation strategy is required
     * @param role the role the uploaded file plays for that entity
     * @return the matching {@link FileValidationStrategy}
     * @throws com.itasocialacademy.oitassist.core.exceptions.ValidationException if
     *                                                                            no
     *                                                                            strategy
     *                                                                            is
     *                                                                            registered
     *                                                                            for
     *                                                                            the
     *                                                                            given
     *                                                                            combination
     */
    public FileValidationStrategy resolve(RelatedEntityType type, FileRole role) {
        return strategies.stream()
            .filter(s -> s.supports(type, role))
            .findFirst()
            .orElseThrow(() -> new ValidationException(
                "No file validation strategy registered for: " + type,
                ErrorCode.FILE_VALIDATION_FAILED));
    }
}
