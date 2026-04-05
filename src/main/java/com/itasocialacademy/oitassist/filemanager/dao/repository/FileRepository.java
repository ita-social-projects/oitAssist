package com.itasocialacademy.oitassist.filemanager.dao.repository;

import com.itasocialacademy.oitassist.core.rest.repository.EntityRepository;
import com.itasocialacademy.oitassist.filemanager.dao.model.FileAsset;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends EntityRepository<FileAsset, Long>, JpaSpecificationExecutor<FileAsset> {
    /** Find all id's of files eligible for cleanup. */
    @Query(value = """
            SELECT f.id FROM FileAsset f
            WHERE (f.status = 'TEMPORARY' AND f.createdAt < :orphanThreshold)
               OR (f.status = 'SOFT_DELETED' AND f.deletedAt < :expiredThreshold)
               OR (f.status = 'FAILED')
        """)
    List<Long> findIdsEligibleForCleanup(
        @Param("orphanThreshold") OffsetDateTime orphanThreshold,
        @Param("expiredThreshold") OffsetDateTime expiredThreshold);

    /** Find all active storage keys. */
    @Query("SELECT f.storageKey FROM FileAsset f WHERE f.status != 'HARD_DELETED'")
    List<String> findAllActiveStorageKeys();

    /** Find all files that claim to be attached to something. */
    @Query("SELECT f FROM FileAsset f WHERE f.status = 'ATTACHED'")
    List<FileAsset> findAllAttachedFiles();
}