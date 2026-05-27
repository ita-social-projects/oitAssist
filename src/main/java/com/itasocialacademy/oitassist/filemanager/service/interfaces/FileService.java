package com.itasocialacademy.oitassist.filemanager.service.interfaces;

import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.dto.request.FileUploadRequestDto;
import com.itasocialacademy.oitassist.filemanager.dto.response.FileResponseDto;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;

public interface FileService {
    /**
     * Validates and uploads a batch of files, linking them to the specified entity.
     *
     * @param files      the files to upload
     * @param requestDto upload context metadata (entity type and optional entity
     *                   ID)
     * @return a list of {@link FileResponseDto} representing the persisted file
     *         records
     * @throws ValidationException if any file fails the policy validation
     */
    List<FileResponseDto> upload(List<MultipartFile> files, FileUploadRequestDto requestDto);

    /**
     * Method to mark a file SOFT_DELETED, but keep a physical file intact.
     *
     * @param fileId id of the file record in the db.
     */
    void deleteSoft(Long fileId);

    /**
     * Method to mark a file HARD_DELETED, and call StorageProvider to physically
     * delete the file.
     *
     * @param fileId id of the file record in the db.
     */
    void deleteHard(Long fileId);

    void linkFilesToEntity(Long entityId, RelatedEntityType entityType, List<Long> fileIds);
}
