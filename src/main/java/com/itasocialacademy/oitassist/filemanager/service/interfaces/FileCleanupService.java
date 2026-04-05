package com.itasocialacademy.oitassist.filemanager.service.interfaces;

/**
 * Service responsible for the garbage collection of the file system. This
 * service ensures data integrity between the database and physical storage by:
 * <ul>
 * <li>Purging temporary and soft-deleted files based on configurable grace
 * periods.</li>
 * <li>Identifying "rogue" files on disk that have no corresponding database
 * record.</li>
 * <li>Detecting "dangling" references where the parent entity (e.g., News) has
 * been deleted.</li>
 * </ul>
 */
public interface FileCleanupService {
    /**
     * Orchestrates the complete maintenance cycle. Usually triggered by a scheduled
     * cron job or a manual administrative action.
     */
    void runFullCleanup();

    /**
     * Identifies and permanently deletes files from storage and the database if
     * they are marked as TEMPORARY or SOFT_DELETED and have exceeded their
     * respective grace periods.
     */
    void purgeExpiredAndOrphanedFiles();

    /**
     * Performs a disk-to-database reconciliation. Scans physical storage for files
     * that are not tracked in the database and deletes them if they exceed a safety
     * age threshold.
     */
    void cleanupRoguePhysicalFiles();

    /**
     * Scans for files with an ATTACHED status whose parent entities no longer exist
     * in the database. These files are moved to SOFT_DELETED status to be
     * eventually purged.
     */
    void handleDanglingAttachedFiles();
}
