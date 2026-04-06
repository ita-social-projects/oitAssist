package com.itasocialacademy.oitassist.filemanager.service;

import com.itasocialacademy.oitassist.filemanager.config.FileCleanupConfig;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileStatus;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.dao.model.FileAsset;
import com.itasocialacademy.oitassist.filemanager.dao.repository.FileRepository;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileListingException;
import com.itasocialacademy.oitassist.filemanager.exceptions.UnsupportedStorageException;
import com.itasocialacademy.oitassist.filemanager.providers.interfaces.StorageProvider;
import com.itasocialacademy.oitassist.filemanager.providers.resolver.StorageProviderResolver;
import com.itasocialacademy.oitassist.filemanager.service.interfaces.FileCleanupService;
import com.itasocialacademy.oitassist.filemanager.service.interfaces.FileService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link FileCleanupService} providing transactional
 * maintenance logic. This implementation uses a "fail-safe" approach: errors
 * occurring during the deletion of a single file are logged but do not
 * terminate the entire batch process. High-level orchestration is executed
 * within a single transaction to ensure status updates (like dangling reference
 * marking) are atomic.
 *
 * @see FileService#deleteHard(Long)
 * @see FileCleanupConfig
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FileCleanupServiceImpl implements FileCleanupService {
    @PersistenceContext
    private EntityManager entityManager;
    private final FileService fileService;
    private final FileRepository repository;
    private final StorageProviderResolver providerResolver;
    private final FileCleanupConfig cleanupConfig;

    /**
     * Executes the full cleanup suite. Order of execution: 1. Dangling references
     * are marked as SOFT_DELETED. 2. Force flush to ensure DB updates are before
     * file operations. 3. DB-identified expired/orphaned files are hard-deleted. 4.
     * Rogue physical files are reconciled and removed.
     */
    @Override
    @Transactional
    public void runFullCleanup() {
        log.info("Files cleanup cycle starting...");

        this.handleDanglingAttachedFiles();

        entityManager.flush();

        this.purgeExpiredAndOrphanedFiles();
        this.cleanupRoguePhysicalFiles();

        log.info("Files cleanup cycle complete.");
    }

    /**
     * Queries the database for IDs eligible for purging and delegates hard-deletion
     * to the {@link FileService}. Identifies and permanently deletes files from
     * storage and the database if they are marked as TEMPORARY or SOFT_DELETED and
     * have exceeded their respective grace periods.
     */
    private void purgeExpiredAndOrphanedFiles() {
        log.info("Files cleanup: Purging expired and orphaned files...");
        OffsetDateTime orphanThreshold = OffsetDateTime.now().minusHours(cleanupConfig.getOrphanHours());
        OffsetDateTime expiredThreshold = OffsetDateTime.now().minusHours(cleanupConfig.getExpiredHours());

        List<Long> fileIds = repository.findIdsEligibleForCleanup(orphanThreshold, expiredThreshold);

        if (fileIds.isEmpty()) {
            log.info("Files cleanup: No orphaned or expired files eligible for purging.");
            return;
        }

        int successCount = deleteExpiredAndOrphaned(fileIds);

        log.info("Files cleanup complete: {}/{} files successfully purged.", successCount, fileIds.size());
    }

    /**
     * Compares the physical file inventory from the {@link StorageProvider} against
     * active database records. Performs a disk-to-database reconciliation.
     *
     * @throws FileListingException if the physical storage cannot be reliably
     *                              scanned.
     */
    private void cleanupRoguePhysicalFiles() {
        log.info("Files cleanup: Cleaning up rogue physical files...");
        try {
            StorageProvider provider = providerResolver.resolveDefault();
            List<String> physicalKeys = provider.listAllPhysicalKeys();

            Set<String> activeKeys = new HashSet<>(repository.findAllActiveStorageKeys());

            int rogueCount = deleteRogueFiles(physicalKeys, activeKeys, provider);

            if (rogueCount > 0) {
                log.info("Files cleanup: Purged {} rogue files with no DB record.", rogueCount);
            } else {
                log.info("Files cleanup: No rogue files found.");
            }
        } catch (FileListingException | UnsupportedStorageException e) {
            log.error("Files cleanup: Storage inventory for rogue files could not be built reliably.", e);
        }
    }

    /**
     * Identifies files marked as {@code ATTACHED} whose parent entities (e.g.,
     * News, Task) have been deleted from the database. These files are moved to
     * SOFT_DELETED status to be eventually purged.
     *
     * <p>
     * This method uses a batching strategy: it groups files by their
     * {@link RelatedEntityType}, collects all unique parent IDs for that type, and
     * performs a single existence check per group. Files found to be "dangling" are
     * moved to {@code SOFT_DELETED} status.
     * </p>
     */
    private void handleDanglingAttachedFiles() {
        log.info("Files cleanup: Checking for dangling file references...");

        List<FileAsset> attachedFiles = repository.findAllAttachedFiles();

        Map<RelatedEntityType, List<FileAsset>> groupedFiles = attachedFiles.stream()
            .collect(Collectors.groupingBy(FileAsset::getRelatedEntityType));

        int danglingCount = processDanglingGroups(groupedFiles);

        if (danglingCount > 0) {
            log.info("Files cleanup: Cleaned up {} dangling references - moved to SOFT_DELETED.", danglingCount);
        } else {
            log.info("Files cleanup: No dangling references found.");
        }
    }

    /**
     * Iterates through grouped file assets to verify parental existence in the
     * database. Order of execution: 1. Collect unique IDs for this entity type
     * (e.g., News IDs). 2. Query DB to see which of these IDs still exist. 3. Mark
     * files as SOFT_DELETED if their parent is missing.
     *
     * @param groupedFiles a map of files grouped by their associated entity type
     * @return the total number of files identified as dangling and marked for
     *         deletion
     */
    private int processDanglingGroups(Map<RelatedEntityType, List<FileAsset>> groupedFiles) {
        int totalDangling = 0;

        for (Map.Entry<RelatedEntityType, List<FileAsset>> entry : groupedFiles.entrySet()) {
            RelatedEntityType type = entry.getKey();
            List<FileAsset> files = entry.getValue();

            final Set<Long> parentIds = collectIdsForType(files);

            Set<Long> existingParentIds = checkExistence(type, parentIds);

            totalDangling = markFilesAsSoftDeleted(files, existingParentIds, totalDangling);
        }
        return totalDangling;
    }

    /**
     * Compares a list of files against a set of known existing parent IDs and
     * updates the status of orphaned files.
     *
     * @param files             the list of files to validate
     * @param existingParentIds a set of IDs that still exist in the database for
     *                          this type
     * @param totalDangling     the current running count of dangling files
     * @return the updated count of dangling files after processing this list
     */
    private int markFilesAsSoftDeleted(List<FileAsset> files, Set<Long> existingParentIds, int totalDangling) {
        for (FileAsset file : files) {
            Long parentId = file.getRelatedEntityId();

            if (parentId != null && !existingParentIds.contains(parentId)) {
                file.setStatus(FileStatus.SOFT_DELETED);
                file.setDeletedAt(OffsetDateTime.now());
                totalDangling++;
            }
        }
        return totalDangling;
    }

    /**
     * Extracts unique, non-null related entity IDs from a list of file assets.
     *
     * @param files the list of file assets
     * @return a set of unique IDs associated with these files
     */
    private @NonNull Set<Long> collectIdsForType(List<FileAsset> files) {
        return files.stream()
            .map(FileAsset::getRelatedEntityId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    /**
     * Performs permanent deletion of files identified by the database cleanup
     * query.
     *
     * <p>
     * This method wraps each individual deletion in a try-catch block to ensure
     * that a failure (e.g., a file-lock or database constraint) on one file does
     * not terminate the entire cleanup process.
     * </p>
     *
     * @param fileIds list of database IDs for files eligible for hard deletion
     * @return the count of successfully deleted files
     */
    private int deleteExpiredAndOrphaned(List<Long> fileIds) {
        int count = 0;
        for (Long id : fileIds) {
            try {
                fileService.deleteHard(id);
                count++;
            } catch (Exception e) { // Add relevant exceptions that may appear during deletion
                log.error("Failed to hard delete file ID: {} during cleanup", id, e);
            }
        }
        return count;
    }

    /**
     * Reconciles physical storage with the database by deleting files found on disk
     * that have no corresponding database record.
     *
     * <p>
     * <strong>Safety Mechanism:</strong> To prevent deleting files that are
     * currently being uploaded (where the file exists but the DB transaction hasn't
     * committed), this only deletes files older than the configured
     * {@code rogueGraceHours}.
     * </p>
     *
     * @param physicalKeys list of all relative paths found in the storage provider
     * @param activeKeys   set of all storage keys currently tracked in the database
     * @param provider     the storage provider to execute deletions against
     * @return the number of rogue files successfully purged
     */
    private int deleteRogueFiles(List<String> physicalKeys, Set<String> activeKeys, StorageProvider provider) {
        int rogueCount = 0;
        OffsetDateTime safetyThreshold = OffsetDateTime.now().minusHours(cleanupConfig.getRogueGraceHours());

        for (String key : physicalKeys) {
            if (!activeKeys.contains(key)) {
                try {
                    OffsetDateTime lastModified = provider.getLastModified(key);
                    if (lastModified == null) {
                        log.warn("Files cleanup: Skipping rogue file {}: could not determine last modified time", key);
                        continue;
                    }
                    if (lastModified.isBefore(safetyThreshold)) {
                        provider.deletePhysical(key);
                        rogueCount++;
                    } else {
                        log.debug("Files cleanup: Skipping rogue file {}: too new (Last modified: {})", key,
                            lastModified);
                    }
                } catch (Exception e) {
                    log.error("Files cleanup: Failed to process or delete rogue file: {}", key, e);
                }
            }
        }
        return rogueCount;
    }

    /**
     * Performs a dynamic JPQL query to check which of the provided IDs still exist
     * in the target entity's table.
     *
     * @param type the type of entity to check (used to resolve the JPA entity name)
     * @param ids  the set of IDs to verify
     * @return a set containing only the IDs that were found to exist in the
     *         database
     */
    private Set<Long> checkExistence(RelatedEntityType type, Set<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptySet();
        }

        String entityName = getEntityName(type);

        String queryString = String.format("SELECT e.id FROM %s e WHERE e.id IN :ids", entityName);

        List<Long> result = entityManager.createQuery(queryString, Long.class)
            .setParameter("ids", ids)
            .getResultList();

        return new HashSet<>(result);
    }

    /**
     * Maps a {@link RelatedEntityType} to its corresponding JPA Entity name. This
     * mapping is used for dynamic JPQL queries during the dangling reference check.
     * The returned string must exactly match the name defined in the
     * {@code @Entity} annotation of the target class.
     *
     * <p>
     * <strong>Maintenance Note:</strong> This method must be kept in sync with the
     * {@link RelatedEntityType} enum. When adding new supported entities to the
     * system (e.g., {@code PROJECT}, {@code USER}), you must update this switch
     * case to prevent {@link IllegalArgumentException} during the cleanup cycle.
     * </p>
     *
     * @param type the enum constant representing the type of the parent entity
     * @return the String name of the JPA entity (e.g., "News")
     * @throws IllegalArgumentException if the provided type is not yet mapped to an
     *                                  entity name
     */
    private String getEntityName(RelatedEntityType type) {
        return switch (type) {
            case NEWS -> "News";
            case TASK -> "Task";
            case SUBMISSION -> "Submission";
            default -> throw new IllegalArgumentException("Unknown entity type: " + type);
        };
    }
}