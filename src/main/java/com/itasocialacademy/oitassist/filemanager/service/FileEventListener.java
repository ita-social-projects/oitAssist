package com.itasocialacademy.oitassist.filemanager.service;

import com.itasocialacademy.oitassist.filemanager.api.events.FilesAttachRequestedEvent;
import com.itasocialacademy.oitassist.filemanager.api.events.FilesDetachRequestedEvent;
import com.itasocialacademy.oitassist.filemanager.service.interfaces.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Event listener for file attachment events within the application module.
 * <p>
 * Handles {@link FilesAttachRequestedEvent} by delegating file linking
 * operations to the {@link FileService}. This listener enables asynchronous,
 * decoupled communication between modules when files need to be attached to
 * entities.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileEventListener {
    private final FileService fileService;

    /**
     * Processes a file attachment request event.
     * <p>
     * Links the specified files to the given entity by delegating to the file
     * service.
     * </p>
     *
     * @param event the event containing entity type, entity ID, and file IDs to
     *              link
     */
    @ApplicationModuleListener
    public void onFilesAttachRequested(FilesAttachRequestedEvent event) {
        log.debug("Received FilesAttachRequestedEvent for entity type={} id={}, fileIds={}",
            event.entityType(), event.entityId(), event.fileIds());

        fileService.linkFilesToEntity(event.entityId(), event.entityType(), event.fileIds(), event.userId());
    }

    /**
     * Processes a file detachment request event.
     * <p>
     * Unlinks the specified files from their associated entities by delegating to
     * the file service.
     * </p>
     *
     * @param event the event containing file IDs to unlink and the user performing
     *              the detachment
     */
    @ApplicationModuleListener
    public void onFileDetachRequested(FilesDetachRequestedEvent event) {
        log.debug("Received FilesDetachRequestedEvent for fileIds={}", event.fileIds());

        fileService.detachFiles(event.fileIds(), event.userId());
    }
}
