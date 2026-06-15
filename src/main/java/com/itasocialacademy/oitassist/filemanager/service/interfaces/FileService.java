package com.itasocialacademy.oitassist.filemanager.service.interfaces;

import com.itasocialacademy.oitassist.filemanager.dao.enums.FileStatus;
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

    /**
     * Transitions a batch of TEMPORARY files to ATTACHED and establishes their
     * relationship with the specified entity.
     *
     * @param entityId   the ID of the entity to link files to
     * @param entityType the type of the related entity
     * @param fileIds    the IDs of the files to attach; no-op if {@code null} or
     *                   empty
     * @param userId     the ID of the user performing the file linking operation
     */
    void linkFilesToEntity(Long entityId, RelatedEntityType entityType, List<Long> fileIds, Long userId);

    /**
     * Marks a batch of files as SOFT_DELETED. Called when files are removed from
     * content. Validates ownership for each file using the provided userId.
     *
     * @param fileIds the IDs of files to soft-delete
     * @param userId  the ID of the user who triggered detach
     */
    void detachFiles(List<Long> fileIds, Long userId);

    /**
     * Returns all {@link FileStatus#ATTACHED} files for the given entity.
     *
     * @param entityType the type of the related entity
     * @param entityId   the ID of the related entity
     * @return list of file DTOs with resolved URLs
     */
    List<FileResponseDto> getFilesByEntity(RelatedEntityType entityType, Long entityId);
}
