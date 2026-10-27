package com.itasocialacademy.oitassist.filemanager.api;

import com.itasocialacademy.oitassist.filemanager.api.dto.FileDetailsDTO;
import com.itasocialacademy.oitassist.filemanager.api.dto.StorageContext;
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
     * Uploads the given files with specific parameters and hierarchical storage
     * path context. Files are stored in a directory structure derived from the
     * entity titles in the storage context rather than in a flat folder by entity
     * type.
     *
     * @param files          files to upload
     * @param entityType     the type of the related entity
     * @param entityId       the ID of the related entity
     * @param role           the role of the given files
     * @param storageContext title hierarchy used to build the storage path
     *                       (nullable — falls back to flat path)
     * @return list of file DTOs with resolved download URLs
     */
    List<FileDetailsDTO> uploadFiles(
        List<MultipartFile> files,
        RelatedEntityType entityType,
        Long entityId,
        FileRole role,
        StorageContext storageContext);

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
     * Marks a batch of files as SOFT_DELETED that must belong to the specified
     * entity. Validates that the files are attached to the given entity type and ID
     * before detaching.
     *
     * <p>
     * <strong>Does NOT perform per-file ownership checks.</strong> The calling
     * module must verify that the current user is authorized to modify the entity
     * (e.g. task ownership check) before invoking this method.
     * </p>
     *
     * @param entityType the type of the related entity
     * @param entityId   the ID of the entity
     * @param fileIds    the IDs of the files to soft-delete
     */
    void detachFilesForMultiOwnerEntity(RelatedEntityType entityType, Long entityId, List<Long> fileIds);

    /**
     * Updates the role of an attached file. Only the file owner or admin can
     * update. File must be in ATTACHED state.
     *
     * @param fileId  the ID of the file to update
     * @param newRole the new role to assign
     */
    void updateFileRole(Long fileId, FileRole newRole);

    /**
     * Updates the role of a file that must belong to the specified entity.
     * Validates that the file is ATTACHED to the given entity type and ID before
     * updating.
     *
     * <p>
     * <strong>Does NOT perform per-file ownership checks.</strong> The calling
     * module must verify that the current user is authorized to modify the entity
     * (e.g. task ownership check) before invoking this method.
     * </p>
     *
     * @param fileId     the ID of the file to update
     * @param newRole    the new role to assign
     * @param entityType the expected related entity type
     * @param entityId   the expected related entity ID
     */
    void updateRoleForMultiOwnerEntity(Long fileId, FileRole newRole, RelatedEntityType entityType, Long entityId);

    /**
     * Creates a reference (symlink or {@code .url} file) in the assignment folder
     * pointing to the corresponding task folder in the task pool.
     *
     * @param context the storage context containing the hierarchy titles
     */
    void createTaskReference(StorageContext context);

    /**
     * Deletes the task reference from the assignment folder.
     *
     * @param context the storage context containing the hierarchy titles
     */
    void deleteTaskReference(StorageContext context);
}
