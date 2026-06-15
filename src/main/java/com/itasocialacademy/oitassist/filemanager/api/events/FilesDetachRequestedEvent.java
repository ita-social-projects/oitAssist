package com.itasocialacademy.oitassist.filemanager.api.events;

import java.util.List;
import org.springframework.modulith.NamedInterface;

@NamedInterface("FilesDetachRequestedEvent")
public record FilesDetachRequestedEvent(List<Long> fileIds, Long userId) {
}
