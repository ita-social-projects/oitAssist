package com.itasocialacademy.oitassist.filemanager.dao.specification;

import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileStatus;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.dao.model.FileAsset;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import java.util.Collection;
import java.util.Set;

/**
 * Reusable JPA Specifications for querying {@link FileAsset} with dynamic
 * criteria. Designed for composition via
 * {@code Specification.where(...).and(...)}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileAssetSpecification {
    public static final String RELATED_ENTITY_TYPE = "relatedEntityType";
    public static final String RELATED_ENTITY_ID = "relatedEntityId";
    public static final String STATUS = "status";
    public static final String FILE_ROLE = "fileRole";

    public static Specification<FileAsset> hasEntityType(RelatedEntityType entityType) {
        return (root, query, cb) -> cb.equal(root.get(RELATED_ENTITY_TYPE), entityType);
    }

    public static Specification<FileAsset> hasEntityId(Long entityId) {
        return (root, query, cb) -> cb.equal(root.get(RELATED_ENTITY_ID), entityId);
    }

    public static Specification<FileAsset> hasEntityIdIn(Collection<Long> entityIds) {
        return (root, query, cb) -> root.get(RELATED_ENTITY_ID).in(entityIds);
    }

    public static Specification<FileAsset> hasStatus(FileStatus status) {
        return (root, query, cb) -> cb.equal(root.get(STATUS), status);
    }

    public static Specification<FileAsset> hasFileRoleIn(Set<FileRole> roles) {
        return (root, query, cb) -> root.get(FILE_ROLE).in(roles);
    }
}
