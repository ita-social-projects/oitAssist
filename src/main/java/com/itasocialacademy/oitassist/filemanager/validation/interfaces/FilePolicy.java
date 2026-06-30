package com.itasocialacademy.oitassist.filemanager.validation.interfaces;

import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.validation.enums.AllowedExtension;
import java.util.Optional;
import java.util.Set;
import org.springframework.util.unit.DataSize;

public interface FilePolicy {
    /**
     * Indicates whether this policy's rules apply to the given combination of
     * entity type and file role.
     *
     * @param entityType the related entity type of the upload
     * @param role       the role the uploaded file plays for that entity
     * @return {@code true} if this policy applies
     */
    boolean supports(RelatedEntityType entityType, FileRole role);

    /**
     * Returns the set of permitted file extensions for this policy.
     *
     * @return allowed extensions
     */
    Set<AllowedExtension> getAllowedExtensions();

    /**
     * Returns the maximum number of files allowed per upload request.
     *
     * @return max file count
     */
    int getMaxFileCount();

    /**
     * Returns the maximum allowed size per individual file.
     *
     * @return max file size as a {@link DataSize}
     */
    DataSize getMaxFileSize();

    /**
     * Returns the required filename (without extension) if the policy mandates a
     * specific name. Returns empty if any filename is permitted.
     *
     * @return an {@link Optional} containing the required name, or empty if
     *         unrestricted
     */
    default Optional<String> getRequiredFileName() {
        return Optional.empty();
    }
}
