package com.itasocialacademy.oitassist.filemanager.service;

import com.itasocialacademy.oitassist.filemanager.api.FileManagerFacade;
import com.itasocialacademy.oitassist.filemanager.api.dto.FileDetailsDTO;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.dto.request.FileUploadRequestDto;
import com.itasocialacademy.oitassist.filemanager.dto.request.UpdateFileRoleRequestDto;
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

    @Override
    public void detachFiles(RelatedEntityType entityType, Long entityId, List<Long> fileIds, Long userId) {
        fileService.detachFiles(entityType, entityId, fileIds, userId);
    }

    @Override
    public void detachFilesForMultiOwnerEntity(RelatedEntityType entityType, Long entityId, List<Long> fileIds) {
        fileService.detachFilesForMultiOwnerEntity(entityType, entityId, fileIds);
    }

    @Override
    public void updateFileRole(Long fileId, FileRole newRole) {
        fileService.updateRoleGeneral(fileId, new UpdateFileRoleRequestDto(newRole));
    }

    @Override
    public void updateRoleForMultiOwnerEntity(Long fileId, FileRole newRole, RelatedEntityType entityType,
        Long entityId) {
        fileService.updateRoleForMultiOwnerEntity(fileId, newRole, entityType, entityId);
    }
}
