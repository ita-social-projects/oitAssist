package com.itasocialacademy.oitassist.filemanager.api.events;

import com.itasocialacademy.oitassist.filemanager.api.dto.StorageContext;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import java.util.List;
import org.springframework.modulith.NamedInterface;

/**
 * Event published when files uploaded as TEMPORARY need to be permanently
 * attached to an entity. The file manager listens for this event to move the
 * physical files to their final destination and update their database records.
 *
 * @param entityId       the ID of the entity the files are being attached to
 * @param entityType     the type of the entity
 * @param fileIds        the list of file IDs to attach
 * @param userId         the ID of the user requesting the attachment
 * @param storageContext the hierarchical context used to determine the final
 *                       storage folder
 */
@NamedInterface("FilesAttachRequestedEvent")
public record FilesAttachRequestedEvent(Long entityId, RelatedEntityType entityType, List<Long> fileIds, Long userId,
    StorageContext storageContext) {
}
