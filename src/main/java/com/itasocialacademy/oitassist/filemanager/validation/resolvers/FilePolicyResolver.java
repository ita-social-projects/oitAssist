package com.itasocialacademy.oitassist.filemanager.validation.resolvers;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.validation.interfaces.FilePolicy;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FilePolicyResolver {
    private final List<FilePolicy> filePolicies;

    /**
     * Resolves the {@link FilePolicy} registered for the given entity type and file
     * role.
     *
     * @param entityType the entity type for which a policy is required
     * @param role       the role the uploaded file plays for that entity
     * @return the matching {@link FilePolicy}
     * @throws com.itasocialacademy.oitassist.core.exceptions.ValidationException if
     *                                                                            no
     *                                                                            policy
     *                                                                            is
     *                                                                            registered
     *                                                                            for
     *                                                                            the
     *                                                                            given
     *                                                                            combination
     */
    public FilePolicy resolve(RelatedEntityType entityType, FileRole role) {
        return filePolicies.stream()
            .filter(p -> p.supports(entityType, role))
            .findFirst()
            .orElseThrow(() -> new ValidationException(
                "No file policy registered for: %s/%s".formatted(entityType, role),
                ErrorCode.FILE_VALIDATION_FAILED));
    }
}
