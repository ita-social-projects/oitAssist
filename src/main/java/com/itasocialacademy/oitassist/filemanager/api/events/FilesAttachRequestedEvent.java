package com.itasocialacademy.oitassist.filemanager.api.events;

import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import java.util.List;
import org.springframework.modulith.NamedInterface;

@NamedInterface("FilesAttachRequestedEvent")
public record FilesAttachRequestedEvent(Long entityId, RelatedEntityType entityType, List<Long> fileIds) {
}
