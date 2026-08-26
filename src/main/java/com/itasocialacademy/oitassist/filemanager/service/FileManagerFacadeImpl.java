package com.itasocialacademy.oitassist.filemanager.service;

import com.itasocialacademy.oitassist.filemanager.api.FileManagerFacade;
import com.itasocialacademy.oitassist.filemanager.api.dto.FileDetailsDTO;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.dto.request.FileUploadRequestDto;
import com.itasocialacademy.oitassist.filemanager.service.interfaces.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class FileManagerFacadeImpl implements FileManagerFacade {
    private final FileService fileService;

    @Override
    public List<FileDetailsDTO> getFilesByEntity(RelatedEntityType entityType, Long entityId, Set<FileRole> roles) {
        return fileService.getFilesByEntity(entityType, entityId, roles);
    }

    @Override
    public Map<Long, List<FileDetailsDTO>> getFilesByEntities(RelatedEntityType entityType, List<Long> entityIds,
                                                              Set<FileRole> roles) {
        return fileService.getFilesByEntities(entityType, entityIds, roles);
    }

    @Override
    public List<FileDetailsDTO> uploadFiles(List<MultipartFile> files, RelatedEntityType entityType, Long entityId,
                                            FileRole role) {
        return fileService.uploadToFileDetails(files, FileUploadRequestDto.builder()
            .fileRole(role)
            .relatedEntityType(entityType)
            .relatedEntityId(entityId)
            .build());
    }

    @Override
    public void detachAllFilesByEntity(RelatedEntityType entityType, Long entityId, Long userId) {
        fileService.detachAllFilesByEntityId(entityType, entityId, userId);
    }
}
