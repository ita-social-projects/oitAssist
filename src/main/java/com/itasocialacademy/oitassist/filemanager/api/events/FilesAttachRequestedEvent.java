package com.itasocialacademy.oitassist.filemanager.api.events;

import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import java.util.List;
import org.springframework.modulith.NamedInterface;

/**
 * Event published when a set of FileStatus TEMPORARY files needs to be attached
 * to a newly created entity.
 *
 * @param entityId   the ID of the newly created entity
 * @param entityType the type of the entity (e.g. {@code NEWS})
 * @param fileIds    the IDs of the temporary files to attach
 * @param userId     the ID of the user requesting the file attachment
 */
@NamedInterface("FilesAttachRequestedEvent")
public record FilesAttachRequestedEvent(Long entityId, RelatedEntityType entityType, List<Long> fileIds, Long userId) {
}
