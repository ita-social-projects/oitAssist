package com.itasocialacademy.oitassist.filemanager.validation.resolvers;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.filemanager.access.FileAccessValidator;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Resolver component responsible for selecting the appropriate {@link FileAccessValidator}
 * strategy based on the target entity type ({@link RelatedEntityType}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileAccessValidatorResolver {
    private final List<FileAccessValidator> validators;

    /**
     * Resolves the {@link FileAccessValidator} registered for the given entity
     * type.
     *
     * @param type the related entity type of the file being accessed
     * @return the matching validator
     * @throws AuthorizationException if no validator is registered for this type —
     *                                fail-closed: an unconfigured entity type must
     *                                never silently grant access.
     */
    public FileAccessValidator resolve(RelatedEntityType type) {
        return validators.stream()
            .filter(validator -> validator.getEntityType() == type)
            .findFirst()
            .orElseThrow(() -> {
                log.warn("No FileAccessValidator registered for entity type: {}", type);
                return new AuthorizationException(
                    "Access denied: no validator configured", ErrorCode.ACCESS_DENIED);
            });
    }
}
