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
 * All these actions are run in a full cycle to ensure consistent storage state.
 */
public interface FileCleanupService {
    /**
     * Orchestrates the complete maintenance cycle. Usually triggered by a scheduled
     * cron job or a manual administrative action.
     */
    void runFullCleanup();
}
