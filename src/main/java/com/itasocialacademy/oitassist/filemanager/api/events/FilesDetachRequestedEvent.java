package com.itasocialacademy.oitassist.filemanager.api.events;

import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import java.util.List;
import org.springframework.modulith.NamedInterface;

@NamedInterface("FilesDetachRequestedEvent")
public record FilesDetachRequestedEvent(RelatedEntityType entityType, Long entityId, List<Long> fileIds, Long userId) {
}
