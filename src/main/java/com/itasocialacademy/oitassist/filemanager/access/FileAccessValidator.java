package com.itasocialacademy.oitassist.filemanager.access;

import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import java.util.function.Predicate;
import org.springframework.modulith.NamedInterface;

/**
 * Strategy interface for determining access rights to files dynamically based
 * on the entity type they belong to.
 */
@NamedInterface("FileAccessValidator")
public interface FileAccessValidator {
    /**
     * Returns the related entity type this validator supports.
     *
     * @return the related entity type this validator supports
     */
    RelatedEntityType getEntityType();

    /**
     * Checks if the user is authorized to access the file belonging to this entity.
     *
     * @param relatedEntityId the ID of the entity the file is attached to
     * @param currentUserId   the ID of the current user, or {@code null} if
     *                        unauthenticated
     * @param hasRole         predicate to check whether the current user holds a
     *                        given role (e.g. {@code hasRole.test("ADMIN")}); each
     *                        validator decides which roles are privileged for its
     *                        own entity type
     * @return true if access is granted, false otherwise
     */
    boolean canAccess(Long relatedEntityId, Long currentUserId, Predicate<String> hasRole);
}
