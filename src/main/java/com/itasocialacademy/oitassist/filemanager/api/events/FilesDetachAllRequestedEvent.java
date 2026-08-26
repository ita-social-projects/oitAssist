package com.itasocialacademy.oitassist.filemanager.api.events;

import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import org.springframework.modulith.NamedInterface;

@NamedInterface("FilesDetachAllRequestedEvent")
public record FilesDetachAllRequestedEvent(RelatedEntityType entityType, Long entityId, Long userId) {
}
