package com.itasocialacademy.oitassist.filemanager.api;

import com.itasocialacademy.oitassist.filemanager.api.dto.FileDetailsDTO;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;

/**
 * Facade exposing file queries to other modules. Returns {@link FileDetailsDTO}
 * to keep the internal persistence model private.
 */
public interface FileManagerFacade {
    /**
     * Returns all ATTACHED files for the given entity, filtered by the specified
     * file roles.
     *
     * @param entityType the type of the related entity
     * @param entityId   the ID of the related entity
     * @param roles      the set of file roles to include in the result
     * @return list of file DTOs with resolved download URLs
     */
    List<FileDetailsDTO> getFilesByEntity(
        RelatedEntityType entityType, Long entityId, Set<FileRole> roles);

    /**
     * Returns all ATTACHED files for the given entities, filtered by the specified
     * file roles.
     *
     * @param entityType the type of the related entity
     * @param entityIds  the IDs of the related entities
     * @param roles      the set of file roles to include in the result
     * @return map of entity ID to list of file DTOs with resolved download URLs
     */
    Map<Long, List<FileDetailsDTO>> getFilesByEntities(
        RelatedEntityType entityType, List<Long> entityIds, Set<FileRole> roles);

    /**
     * Uploads the given files with specific parameters.
     *
     * @param files      files to upload
     * @param entityType the type of the related entity
     * @param entityId   the ID of the related entity
     * @param role       the role of the given files
     * @return list of file DTOs with resolved download URLs
     */
    List<FileDetailsDTO> uploadFiles(
        List<MultipartFile> files, RelatedEntityType entityType, Long entityId, FileRole role);

    /**
     * Detaches all files by given entity type and id by marking them as
     * SOFT_DELETED.
     *
     * @param entityType the type of the related entity
     * @param entityId   the ID of the related entity
     * @param userId     the ID of user performing the detachment
     */
    void detachAllFilesByEntity(RelatedEntityType entityType, Long entityId, Long userId);

    /**
     * Marks attached files as SOFT_DELETED, detaching them from the entity. No-op
     * if fileIds is null or empty.
     *
     * @param entityType the type of the related entity
     * @param entityId   the ID of the entity
     * @param fileIds    the IDs of the files to soft-delete
     * @param userId     the ID of the user performing the operation
     */
    void detachFiles(RelatedEntityType entityType, Long entityId, List<Long> fileIds, Long userId);

    /**
     * Updates the role of an attached file. Only the file owner or admin can
     * update. File must be in ATTACHED state.
     *
     * @param fileId  the ID of the file to update
     * @param newRole the new role to assign
     */
    void updateFileRole(Long fileId, FileRole newRole);
}
