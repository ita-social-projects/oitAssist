package com.itasocialacademy.oitassist.filemanager.service;

import static com.itasocialacademy.oitassist.filemanager.util.FilePathUtils.sanitize;
import static com.itasocialacademy.oitassist.filemanager.validation.util.FileValidationUtils.*;
import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.filemanager.api.dto.FileDetailsDTO;
import com.itasocialacademy.oitassist.filemanager.api.dto.StorageContext;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileStatus;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import com.itasocialacademy.oitassist.filemanager.dao.model.FileAsset;
import com.itasocialacademy.oitassist.filemanager.dao.repository.FileRepository;
import com.itasocialacademy.oitassist.filemanager.dao.specification.FileAssetSpecification;
import com.itasocialacademy.oitassist.filemanager.dto.request.FileUploadRequestDto;
import com.itasocialacademy.oitassist.filemanager.dto.response.FileResourceDto;
import com.itasocialacademy.oitassist.filemanager.dto.request.UpdateFileRoleRequestDto;
import com.itasocialacademy.oitassist.filemanager.dto.response.FileResponseDto;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileAssetNotFoundException;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileUploadException;
import com.itasocialacademy.oitassist.filemanager.mapper.FileMapper;
import com.itasocialacademy.oitassist.filemanager.providers.interfaces.StorageProvider;
import com.itasocialacademy.oitassist.filemanager.providers.resolver.StorageProviderResolver;
import com.itasocialacademy.oitassist.filemanager.service.interfaces.FileService;
import com.itasocialacademy.oitassist.filemanager.util.FilePathUtils;
import com.itasocialacademy.oitassist.filemanager.validation.interfaces.FilePolicy;
import com.itasocialacademy.oitassist.filemanager.validation.interfaces.FileValidationStrategy;
import com.itasocialacademy.oitassist.filemanager.validation.model.ValidationResult;
import com.itasocialacademy.oitassist.filemanager.validation.resolvers.FileAccessValidatorResolver;
import com.itasocialacademy.oitassist.filemanager.validation.resolvers.FilePolicyResolver;
import com.itasocialacademy.oitassist.filemanager.access.FileAccessValidator;
import com.itasocialacademy.oitassist.filemanager.validation.resolvers.FileValidationStrategyResolver;
import com.itasocialacademy.oitassist.filemanager.validation.util.FileValidationUtils;
import org.springframework.core.io.Resource;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {
    private final StorageProviderResolver providerResolver;
    private final FileValidationStrategyResolver validationStrategyResolver;
    private final FileRepository repository;
    private final FileMapper fileMapper;
    private final SecurityFacade securityFacade;
    private final FilePolicyResolver filePolicyResolver;
    private final FileAccessValidatorResolver accessValidatorResolver;

    /**
     * Role identifier for administrative users. Used for role-based access control
     * checks throughout this service.
     */
    private static final String ROLE_ADMIN = "ADMIN";

    /**
     * Role identifier for organizational users. Used for role-based access control
     * checks throughout this service.
     */
    private static final String ROLE_ORG = "ORG";

    /**
     * Root directory name used for storing competition-related file hierarchies,
     * including submission uploads and assignment task references.
     */
    public static final String OLYMPIADS_PATH = "olympiads";

    /**
     * Error message indicating that the user is not authenticated. Used when an
     * operation requires authentication, but it is invalid.
     */
    public static final String NOT_AUTHENTICATED = "Not authenticated";

    /**
     * Error message prefix indicating that a requested file could not be found in
     * the database. This message is typically appended with the file identifier to
     * provide context.
     */
    public static final String FILE_NOT_FOUND_IN_THE_DATABASE = "File not found in the database: ";

    /**
     * {@inheritDoc}
     *
     * <p>
     * Resolves the validation strategy for the given entity type, validates all
     * files against the applicable policy, then delegates each file to
     * {@link #uploadSingle}.
     * </p>
     */
    @Override
    @Transactional
    public List<FileResponseDto> upload(List<MultipartFile> files, FileUploadRequestDto requestDto) {
        Long currentUserId = securityFacade.getCurrentUserId()
            .orElseThrow(() -> new AuthorizationException(
                NOT_AUTHENTICATED, ErrorCode.ACCESS_DENIED));

        checkValidation(files, requestDto);

        return files.stream()
            .map(file -> uploadSingle(file, requestDto, currentUserId))
            .toList();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Resolves the validation strategy for the given entity type, validates all
     * files against the applicable policy, then delegates each file to
     * {@link #uploadSingleToFileDetails}.
     * </p>
     */
    @Override
    @Transactional
    public List<FileDetailsDTO> uploadToFileDetails(List<MultipartFile> files, FileUploadRequestDto requestDto) {
        Long currentUserId = securityFacade.getCurrentUserId()
            .orElseThrow(() -> new AuthorizationException(
                NOT_AUTHENTICATED, ErrorCode.ACCESS_DENIED));

        checkValidation(files, requestDto);

        return files.stream()
            .map(file -> uploadSingleToFileDetails(file, requestDto, currentUserId))
            .toList();
    }

    /**
     * Method to perform a status change of the file. It marks the file as
     * SOFT_DELETED, which can be used in further storage cleanup operations either
     * manual, or scheduled. The physical record of the file after method execution
     * remains intact.
     *
     * @param fileId id of the file.
     */
    @Override
    @Transactional
    public void deleteSoft(Long fileId) {
        Long currentUserId = securityFacade.getCurrentUserId()
            .orElseThrow(() -> new AuthorizationException(
                NOT_AUTHENTICATED, ErrorCode.ACCESS_DENIED));

        FileAsset file = repository.findById(fileId)
            .orElseThrow(() -> new FileAssetNotFoundException(FILE_NOT_FOUND_IN_THE_DATABASE + fileId));

        checkOwnerOrAdmin(file.getUserId(), currentUserId);

        file.setStatus(FileStatus.SOFT_DELETED);
        file.setDeletedAt(OffsetDateTime.now());
        repository.save(file);
    }

    /**
     * Method to handle physical deletion of a file. Used for permanent deletion,
     * cleanup scheduling or orphaned files' cleanup.
     *
     * @param fileId id of the file.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteHard(Long fileId) {
        FileAsset file = repository.findById(fileId)
            .orElseThrow(() -> new FileAssetNotFoundException(FILE_NOT_FOUND_IN_THE_DATABASE + fileId));

        validateAdmin();

        StorageProvider provider = providerResolver.resolve(file.getStorageProvider());

        try {
            provider.deletePhysical(file.getStorageKey());

            file.setStatus(FileStatus.HARD_DELETED);
            file.setDeletedAt(OffsetDateTime.now());
            file.setStorageKey("");
        } catch (Exception e) {
            log.error("Physical deletion failed for file {}, but DB record updated", fileId, e);
            file.setStatus(FileStatus.FAILED);
        }

        repository.save(file);
    }

    /**
     * Links previously uploaded TEMPORARY files to a specific entity. If a
     * {@link StorageContext} is provided, the files are physically moved from the
     * temporary directory to their final hierarchical storage path.
     *
     * @param entityId       the ID of the entity to link files to
     * @param entityType     the type of the related entity
     * @param fileIds        the IDs of the files to attach; no-op if {@code null}
     *                       or empty
     * @param userId         the ID of the user performing the file linking
     *                       operation; must own all files or possess the ADMIN role
     * @param storageContext the context used to calculate the final storage path
     */
    @Override
    @Transactional
    public void linkFilesToEntity(Long entityId, RelatedEntityType entityType, List<Long> fileIds, Long userId,
        StorageContext storageContext) {
        if (fileIds == null || fileIds.isEmpty()) {
            log.debug("No files to link to entity {} with id={}", entityType, entityId);
            return;
        }

        boolean isAdmin = securityFacade.hasRole(ROLE_ADMIN);
        List<FileAsset> files = repository.findAllById(fileIds);

        for (FileAsset file : files) {
            checkOwnerOrAdmin(file.getUserId(), userId, isAdmin);
        }

        for (FileAsset file : files) {
            if (file.getStatus() == FileStatus.TEMPORARY) {
                file.setStatus(FileStatus.ATTACHED);
                file.setRelatedEntityId(entityId);
                file.setRelatedEntityType(entityType);

                moveFromTempFolder(entityId, entityType, storageContext, file);

                log.debug("Linked file id={} to {} with id={}", file.getId(), entityType, entityId);
            } else {
                log.warn("Skipped file id={} with status={} (expected TEMPORARY)", file.getId(), file.getStatus());
            }
        }
        repository.saveAll(files);
    }

    /**
     * Marks a batch of files as {@link FileStatus#SOFT_DELETED}. Called via event
     * after a news update removes files from content. Validates ownership for each
     * file using the explicitly provided userId.
     *
     * @param entityType the type of the related entity
     * @param entityId   the ID of the entity to detach files to
     * @param fileIds    the IDs of files to soft-delete
     * @param userId     the ID of the user who triggered detach (from the event)
     */
    @Override
    @Transactional
    public void detachFiles(RelatedEntityType entityType, Long entityId, List<Long> fileIds, Long userId) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }

        List<FileAsset> files = repository.findAllById(fileIds);
        detachFilesHelper(entityType, entityId, userId, files);
    }

    @Override
    @Transactional
    public void detachFilesForMultiOwnerEntity(RelatedEntityType entityType, Long entityId, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        List<FileAsset> files = repository.findAllById(fileIds);

        for (FileAsset file : files) {
            validateEntityBoundary(file, entityType, entityId);
            markAsSoftDeleted(file);
        }
        repository.saveAll(files);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void detachAllFilesByEntityId(RelatedEntityType entityType, Long entityId, Long userId) {
        List<FileAsset> files = repository
            .findByRelatedEntityTypeAndRelatedEntityIdAndStatus(
                entityType, entityId, FileStatus.ATTACHED);
        detachFilesHelper(entityType, entityId, userId, files);
    }

    /**
     * Returns all {@link FileStatus#ATTACHED} files for the given entity, enriched
     * with unified file endpoint URLs ({@code /api/v1/files/{id}}).
     *
     * @param entityType the type of the related entity
     * @param entityId   the ID of the related entity
     * @return list of file DTOs with resolved file URLs
     */
    @Override
    @Transactional(readOnly = true)
    public List<FileResponseDto> getFilesByEntity(RelatedEntityType entityType, Long entityId) {
        List<FileAsset> files = repository
            .findByRelatedEntityTypeAndRelatedEntityIdAndStatus(
                entityType, entityId, FileStatus.ATTACHED);

        return files.stream()
            .map(file -> {
                FileResponseDto dto = fileMapper.toDto(file);
                dto.setUrl(buildFileUrl(file.getId()));
                return dto;
            })
            .toList();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Uses JPA Specifications to filter by entity type, entity ID, ATTACHED status,
     * and the provided set of file roles. Maps results directly to
     * {@link FileDetailsDTO} via the file mapper with unified file URLs.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public List<FileDetailsDTO> getFilesByEntity(RelatedEntityType entityType, Long entityId, Set<FileRole> roles) {
        Specification<FileAsset> spec = Specification
            .where(FileAssetSpecification.hasEntityType(entityType))
            .and(FileAssetSpecification.hasEntityId(entityId))
            .and(FileAssetSpecification.hasStatus(FileStatus.ATTACHED))
            .and(FileAssetSpecification.hasFileRoleIn(roles));

        return repository.findAll(spec).stream()
            .map(file -> fileMapper.toDetails(file, buildFileUrl(file.getId())))
            .toList();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Uses JPA Specifications to filter by entity type, entity IDs, ATTACHED
     * status, and the provided set of file roles. Maps results directly to
     * {@link FileDetailsDTO} via the file mapper and groups them by entity ID with
     * unified file URLs.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<FileDetailsDTO>> getFilesByEntities(RelatedEntityType entityType, List<Long> entityIds,
        Set<FileRole> roles) {
        if (entityIds == null || entityIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Specification<FileAsset> spec = Specification
            .where(FileAssetSpecification.hasEntityType(entityType))
            .and(FileAssetSpecification.hasEntityIdIn(entityIds))
            .and(FileAssetSpecification.hasStatus(FileStatus.ATTACHED))
            .and(FileAssetSpecification.hasFileRoleIn(roles));

        List<FileAsset> files = repository.findAll(spec);

        Map<Long, List<FileDetailsDTO>> resultMap = new HashMap<>();
        for (Long id : entityIds) {
            resultMap.put(id, new ArrayList<>());
        }

        for (FileAsset file : files) {
            FileDetailsDTO dto = fileMapper.toDetails(file, buildFileUrl(file.getId()));
            resultMap.computeIfAbsent(file.getRelatedEntityId(), k -> new ArrayList<>()).add(dto);
        }

        return resultMap;
    }

    /**
     * Updates the role of a file. Restricted to the original uploader or an
     * administrator. This method is typically called via the REST API for generic
     * file role updates.
     *
     * @param fileId  the ID of the file to update
     * @param request the DTO containing the new role
     * @return the updated file metadata
     * @throws AuthorizationException     if the current user is not authenticated
     *                                    or not authorized
     * @throws FileAssetNotFoundException if the file is not found
     */
    @Override
    @Transactional
    public FileResponseDto updateRoleGeneral(Long fileId, UpdateFileRoleRequestDto request) {
        Long currentUserId = securityFacade.getCurrentUserId()
            .orElseThrow(() -> new AuthorizationException(
                NOT_AUTHENTICATED, ErrorCode.ACCESS_DENIED));

        FileAsset file = repository.findById(fileId)
            .orElseThrow(() -> new FileAssetNotFoundException(
                FILE_NOT_FOUND_IN_THE_DATABASE + fileId));

        checkOwnerOrAdmin(file.getUserId(), currentUserId);

        FileAsset saved = validateAndUpdateRole(file, request.getNewRole(),
            file.getRelatedEntityType(), file.getRelatedEntityId());

        FileResponseDto dto = fileMapper.toDto(saved);
        dto.setUrl(buildFileUrl(saved.getId()));
        return dto;
    }

    /**
     * Updates the role of a file attached to an entity that may have multiple
     * owners or complex authorization rules (e.g., Task module where multiple
     * authors exist). Bypasses the strict single-uploader ownership check, assuming
     * the calling module has already verified the user's permissions for the parent
     * entity.
     *
     * @param fileId     the ID of the file to update
     * @param newRole    the new role to assign
     * @param entityType the related entity type to validate against
     * @param entityId   the related entity ID to validate against
     * @throws FileAssetNotFoundException if the file is not found
     */
    @Override
    @Transactional
    public void updateRoleForMultiOwnerEntity(Long fileId, FileRole newRole, RelatedEntityType entityType,
        Long entityId) {
        FileAsset file = repository.findById(fileId)
            .orElseThrow(() -> new FileAssetNotFoundException(
                FILE_NOT_FOUND_IN_THE_DATABASE + fileId));

        validateAndUpdateRole(file, newRole, entityType, entityId);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Resolves the assignment path
     * ({@code olympiads/<competition>/<stage>/<tour>/<task>}) and task pool path
     * ({@code tasks/<task>}), then delegates reference creation to the default
     * provider.
     * </p>
     */
    @Override
    public void createTaskReference(StorageContext ctx) {
        String assignmentPath = String.join("/", OLYMPIADS_PATH,
            sanitize(ctx.competitionTitle()), sanitize(ctx.stageTitle()),
            sanitize(ctx.tourTitle()), sanitize(ctx.taskTitle()));
        String poolPath = "tasks/" + sanitize(ctx.taskTitle());
        providerResolver.resolveDefault().createTaskReference(assignmentPath, poolPath);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Resolves the assignment path
     * ({@code olympiads/<competition>/<stage>/<tour>/<task>}) and delegates
     * reference deletion to the default provider.
     * </p>
     */
    @Override
    public void deleteTaskReference(StorageContext ctx) {
        String assignmentPath = String.join("/", OLYMPIADS_PATH,
            sanitize(ctx.competitionTitle()), sanitize(ctx.stageTitle()),
            sanitize(ctx.tourTitle()), sanitize(ctx.taskTitle()));
        providerResolver.resolveDefault().deleteTaskReference(assignmentPath);
    }

    /**
     * Validates and updates the role of a given file asset. Ensures the file is in
     * an ATTACHED state, belongs to the specified entity, and that the new role is
     * allowed for its extension based on the entity's policy.
     *
     * @param file       the file asset to update
     * @param newRole    the new role to assign
     * @param entityType the expected related entity type
     * @param entityId   the expected related entity ID
     * @return the saved file asset (or the original if the role is already set)
     * @throws ValidationException if the file is not ATTACHED, does not belong to
     *                             the entity, or the extension is disallowed
     */
    private FileAsset validateAndUpdateRole(FileAsset file, FileRole newRole, RelatedEntityType entityType,
        Long entityId) {
        if (!file.getStatus().equals(FileStatus.ATTACHED)) {
            throw new ValidationException(
                "File must be in ATTACHED state to update its role",
                ErrorCode.FILE_VALIDATION_FAILED);
        }

        validateEntityBoundary(file, entityType, entityId);

        if (file.getFileRole().equals(newRole)) {
            return file;
        }

        FilePolicy newPolicy = filePolicyResolver.resolve(entityType, newRole);
        String extension = extractExtension(file.getOriginalFilename());

        if (isExtensionNotAllowed(extension, newPolicy.getAllowedExtensions())) {
            throw new ValidationException(
                "File '%s' has extension '%s' which is not allowed for role %s. Allowed: %s."
                    .formatted(file.getOriginalFilename(), extension, newRole,
                        formatAllowed(newPolicy.getAllowedExtensions())),
                ErrorCode.FILE_VALIDATION_FAILED);
        }

        if (entityType == RelatedEntityType.TASK && file.getStorageKey() != null) {
            String oldKey = file.getStorageKey();
            int lastSlash = oldKey.lastIndexOf('/');
            int secondLastSlash = lastSlash > 0 ? oldKey.lastIndexOf('/', lastSlash - 1) : -1;

            if (secondLastSlash > 0) {
                final String delimiter = "/";
                // e.g. "tasks/My Task/problem" -> "tasks/My Task/solution"
                String newRelativePath =
                    oldKey.substring(0, secondLastSlash) + delimiter + newRole.name().toLowerCase();

                // Ensure no collision if a file with same name already exists in the new role
                // folder
                String safeFilename = generateStoredFilename(file.getOriginalFilename(), newRelativePath);
                String newPath = newRelativePath + delimiter + safeFilename;

                StorageProvider provider = providerResolver.resolve(file.getStorageProvider());
                String newKey = provider.moveFile(oldKey, newPath);

                file.setStorageKey(newKey);
                file.setStoredFilename(safeFilename);
                log.debug("Physically moved file on role change from {} to {}", oldKey, newKey);
            }
        }

        file.setFileRole(newRole);
        FileAsset saved = repository.save(file);
        log.debug("Updated file role for id={} to {} (entity {}:{})",
            file.getId(), newRole, entityType, entityId);

        return saved;
    }

    /**
     * Marks a batch of files as SOFT_DELETED. Called when files are removed from
     * content. Validates ownership for each file using the provided userId.
     *
     * @param entityType the type of the related entity
     * @param entityId   the ID of the entity to detach files from
     * @param userId     the ID of the user who triggered detach
     * @param files      files to detach
     */
    private void detachFilesHelper(RelatedEntityType entityType, Long entityId, Long userId, List<FileAsset> files) {
        boolean isAdmin = securityFacade.hasRole(ROLE_ADMIN);

        for (FileAsset file : files) {
            validateEntityBoundary(file, entityType, entityId);
            checkOwnerOrAdmin(file.getUserId(), userId, isAdmin);
            markAsSoftDeleted(file);
        }
        repository.saveAll(files);
    }

    /**
     * Validates that the given file asset belongs to the specified entity.
     *
     * @param file       the file asset to validate
     * @param entityType the expected related entity type
     * @param entityId   the expected related entity ID
     * @throws ValidationException if the file does not belong to the entity
     */
    private void validateEntityBoundary(FileAsset file, RelatedEntityType entityType, Long entityId) {
        if (!entityType.equals(file.getRelatedEntityType()) || !entityId.equals(file.getRelatedEntityId())) {
            throw new ValidationException(
                "File id=" + file.getId() + " does not belong to " + entityType + " id=" + entityId,
                ErrorCode.FILE_VALIDATION_FAILED);
        }
    }

    /**
     * Updates the file status to SOFT_DELETED and sets the deletion timestamp.
     * Note: This does not persist the changes to the database; the caller must save
     * the entity.
     *
     * @param file the file asset to mark as soft deleted
     */
    private void markAsSoftDeleted(FileAsset file) {
        if (file.getStatus().equals(FileStatus.ATTACHED)) {
            file.setStatus(FileStatus.SOFT_DELETED);
            file.setDeletedAt(OffsetDateTime.now());
        }
    }

    /**
     * Checks if given files are valid according to the validation strategy.
     *
     * @param files      the files to upload
     * @param requestDto upload context metadata (entity type and optional entity
     *                   ID)
     * @throws ValidationException if any file fails the policy validation
     */
    private void checkValidation(List<MultipartFile> files, FileUploadRequestDto requestDto)
        throws ValidationException {
        RelatedEntityType entityType = requestDto.getRelatedEntityType();
        FileRole role = requestDto.getFileRole();

        FileValidationStrategy strategy = validationStrategyResolver.resolve(entityType, role);
        FilePolicy policy = filePolicyResolver.resolve(entityType, role);
        ValidationResult result = strategy.validate(files, requestDto, policy);

        if (!result.valid()) {
            throw new ValidationException(
                String.join(", ", result.violations()),
                ErrorCode.FILE_VALIDATION_FAILED);
        }
    }

    /**
     * Resolves and returns the file resource DTO, handling access control and
     * providing the resource and file metadata for presentation.
     *
     * @param id the ID of the file to retrieve
     * @return FileResourceDto containing the resource, metadata or redirect details
     * @throws FileAssetNotFoundException if the file is not found with the given ID
     * @throws AuthorizationException     if the user does not have permission to
     *                                    view this file
     */
    @Override
    @Transactional(readOnly = true)
    public FileResourceDto getFileResource(Long id) {
        FileAsset file = repository.findById(id)
            .orElseThrow(() -> new FileAssetNotFoundException("File not found with ID: " + id));

        validateFileAccess(file);

        StorageProvider provider = providerResolver.resolve(file.getStorageProvider());
        Resource resource = provider.getResource(file.getStorageKey());

        return new FileResourceDto(
            resource,
            file.getMimeType(),
            resolveDisplayFilename(file),
            file.getSize());
    }

    /**
     * Uploads a single file to the default storage provider and persists its
     * metadata.
     *
     * @param file       the file to upload
     * @param requestDto upload context metadata
     * @param userId     the ID of the uploading user
     * @return the persisted file record as a {@link FileResponseDto}
     * @throws FileUploadException if the file stream cannot be read or the upload
     *                             fails
     */
    private FileResponseDto uploadSingle(MultipartFile file, FileUploadRequestDto requestDto, Long userId) {
        StorageProvider provider = providerResolver.resolveDefault();
        FileAsset saved = uploadFileAndGetFileAsset(file, requestDto, userId, provider);
        FileResponseDto dto = fileMapper.toDto(saved);
        dto.setUrl(buildFileUrl(saved.getId()));
        return dto;
    }

    /**
     * Uploads a single file to the default storage provider and persists its
     * metadata.
     *
     * @param file       the file to upload
     * @param requestDto upload context metadata
     * @param userId     the ID of the uploading user
     * @return the persisted file record as a {@link FileDetailsDTO}
     * @throws FileUploadException if the file stream cannot be read or the upload
     *                             fails
     */
    private FileDetailsDTO uploadSingleToFileDetails(MultipartFile file, FileUploadRequestDto requestDto, Long userId) {
        StorageProvider provider = providerResolver.resolveDefault();
        FileAsset saved = uploadFileAndGetFileAsset(file, requestDto, userId, provider);
        return fileMapper.toDetails(saved, buildFileUrl(saved.getId()));
    }

    /**
     * Uploads a single file to the default storage provider and persists its
     * metadata.
     *
     * @param file       the file to upload
     * @param requestDto upload context metadata
     * @param userId     the ID of the uploading user
     * @param provider   the resolved StorageProvider
     * @return the persisted file record as a {@link FileAsset}
     * @throws FileUploadException if the file stream cannot be read or the upload
     *                             fails
     */
    private FileAsset uploadFileAndGetFileAsset(MultipartFile file, FileUploadRequestDto requestDto, Long userId,
        StorageProvider provider) {
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";

        String relativePath = buildRelativePath(requestDto);

        String physicalName = generateStoredFilename(originalFilename, relativePath);

        String storedFilename = requestDto.getRelatedEntityType() == RelatedEntityType.SUBMISSION
            ? UUID.randomUUID() + FilePathUtils.extractExtensionWithDot(originalFilename)
            : physicalName;

        String storageKey;
        try (var inputStream = file.getInputStream()) {
            storageKey = provider.upload(inputStream, physicalName, relativePath);
        } catch (IOException e) {
            log.error("Failed to upload file: {}", originalFilename, e);
            throw new FileUploadException(originalFilename, e);
        }

        FileAsset fileAsset = buildFileAsset(
            file,
            requestDto,
            originalFilename,
            storedFilename,
            storageKey,
            userId,
            provider.getType());

        return repository.save(fileAsset);
    }

    /**
     * Resolves a unique filename based on the original filename. If a file with the
     * same name already exists in the target folder (as an active storage key), a
     * numeric suffix is appended ({@code name_1.ext}, {@code name_2.ext}, etc.).
     *
     * @param originalFilename the original name of the uploaded file
     * @param relativePath     the target directory path used for collision
     *                         detection
     * @return a sanitized, unique filename safe for storage
     */
    private String generateStoredFilename(String originalFilename, String relativePath) {
        String baseName = FileValidationUtils.extractNameWithoutExtension(originalFilename);
        String ext = FilePathUtils.extractExtensionWithDot(originalFilename);
        if (ext.isEmpty() && originalFilename != null && originalFilename.endsWith(".")) {
            ext = ".";
        }
        String sanitizedBase;
        try {
            sanitizedBase = FilePathUtils.sanitize(baseName);
        } catch (ValidationException _) {
            sanitizedBase = (baseName == null || baseName.isBlank()) ? "file"
                : baseName.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1F]", "_");
            if (sanitizedBase.isBlank()) {
                sanitizedBase = "file";
            }
        }

        String candidate = sanitizedBase + ext;
        int n = 1;
        while (repository.existsByStorageKey(relativePath + "/" + candidate)) {
            candidate = sanitizedBase + "_" + n + ext;
            n++;
        }
        return candidate;
    }

    /**
     * Builds the relative storage directory path for a file upload.
     *
     * <p>
     * When a {@link StorageContext} is present, constructs a human-readable
     * hierarchical path from entity titles (e.g.
     * {@code tasks/PowerPoint/problems/}). When absent, falls back to a flat
     * directory named after the entity type. Temporary files (no entity ID) are
     * placed in {@code _tmp/}.
     * </p>
     *
     * @param requestDto the upload request containing entity type and storage
     *                   context
     * @return the relative directory path for the file
     */
    private String buildRelativePath(FileUploadRequestDto requestDto) {
        StorageContext storageContext = requestDto.getStorageContext();

        // TEMPORARY files (no entityId) go to _tmp/
        if (requestDto.getRelatedEntityId() == null) {
            return "_tmp";
        }

        // If an entityId is provided, a StorageContext MUST be provided.
        if (storageContext == null) {
            throw new ValidationException("StorageContext is required for attached files",
                ErrorCode.FILE_VALIDATION_FAILED);
        }

        return switch (requestDto.getRelatedEntityType()) {
            case NEWS -> {
                if (storageContext.newsTitle() == null) {
                    throw storageContextMismatch("newsTitle", RelatedEntityType.NEWS.name());
                }
                yield "news/" + sanitize(storageContext.newsTitle());
            }
            case TASK -> {
                if (storageContext.taskTitle() == null) {
                    throw storageContextMismatch("taskTitle", RelatedEntityType.TASK.name());
                }
                yield "tasks/" + sanitize(storageContext.taskTitle()) + "/"
                    + requestDto.getFileRole().name().toLowerCase();
            }
            case SUBMISSION -> {
                if (storageContext.competitionTitle() == null || storageContext.stageTitle() == null
                    || storageContext.tourTitle() == null || storageContext.taskTitle() == null
                    || storageContext.userEmail() == null) {
                    throw storageContextMismatch("competitionTitle, stageTitle, "
                        + "tourTitle, taskTitle, userEmail", RelatedEntityType.SUBMISSION.name());
                }
                yield String.join("/",
                    OLYMPIADS_PATH,
                    sanitize(storageContext.competitionTitle()),
                    sanitize(storageContext.stageTitle()),
                    sanitize(storageContext.tourTitle()),
                    sanitize(storageContext.taskTitle()),
                    "submissions",
                    sanitize(storageContext.userEmail()));
            }
            case COMPETITION -> "competitions";
        };
    }

    /**
     * Helper to construct a clear validation error when the provided StorageContext
     * lacks the fields required for the requested entity type.
     */
    private ValidationException storageContextMismatch(String required, String type) {
        return new ValidationException(
            "StorageContext missing required fields [" + required + "] for entity type " + type,
            ErrorCode.FILE_VALIDATION_FAILED);
    }

    /**
     * Constructs a {@link FileAsset} entity from upload context data.
     *
     * @param file             the uploaded file
     * @param requestDto       the upload request metadata
     * @param originalFilename the original client-provided filename
     * @param storedFilename   the unique filename used on disk
     * @param storageKey       the relative storage key returned by the provider
     * @param userId           the ID of the uploading user
     * @param providerType     the storage provider type used for this upload
     * @return a new {@link FileAsset} ready for persistence
     */
    private FileAsset buildFileAsset(
        MultipartFile file,
        FileUploadRequestDto requestDto,
        String originalFilename,
        String storedFilename,
        String storageKey,
        Long userId,
        StorageProviderType providerType) {
        return FileAsset.builder()
            .userId(userId)
            .relatedEntityType(requestDto.getRelatedEntityType())
            .relatedEntityId(requestDto.getRelatedEntityId())
            .status(resolveStatus(requestDto))
            .storageProvider(providerType)
            .originalFilename(originalFilename)
            .storedFilename(storedFilename)
            .storageKey(storageKey)
            .mimeType(file.getContentType())
            .size(file.getSize())
            .fileRole(requestDto.getFileRole())
            .build();
    }

    /**
     * Determines the initial {@link FileStatus} based on whether the upload is
     * linked to a specific entity. Returns {@code TEMPORARY} when no entity ID is
     * present, or {@code ATTACHED} when linked.
     *
     * @param requestDto the upload request metadata
     * @return the appropriate initial file status
     */
    private FileStatus resolveStatus(FileUploadRequestDto requestDto) {
        return requestDto.getRelatedEntityId() == null
            ? FileStatus.TEMPORARY
            : FileStatus.ATTACHED;
    }

    /**
     * Core authorization check — verifies that the given user is either the file
     * owner or an administrator. Used in batch operations where {@code isAdmin} is
     * resolved once outside the loop to avoid redundant Security Context calls.
     *
     * @param fileOwnerId   the ID of the user who owns the file
     * @param currentUserId the ID of the user performing the operation
     * @param isAdmin       whether the current user has the ADMIN role
     * @throws AuthorizationException if the user is neither owner nor admin
     */
    private void checkOwnerOrAdmin(Long fileOwnerId, Long currentUserId, boolean isAdmin) {
        boolean isOwner = fileOwnerId.equals(currentUserId);
        if (!isOwner && !isAdmin) {
            log.warn("Security Breach: User {} attempted to access file owned by {}",
                currentUserId, fileOwnerId);
            throw new AuthorizationException(
                "You do not have permission to modify this file.",
                ErrorCode.ACCESS_DENIED);
        }
    }

    /**
     * Convenience overload for single-file operations where {@code isAdmin} is
     * resolved internally. Used in {@link #deleteSoft} where only one file is
     * processed and a single Security Context call is acceptable.
     *
     * @param fileOwnerId   the ID of the user who owns the file
     * @param currentUserId the ID of the user performing the operation
     * @throws AuthorizationException if the user is neither owner nor admin
     */
    private void checkOwnerOrAdmin(Long fileOwnerId, Long currentUserId) {
        checkOwnerOrAdmin(fileOwnerId, currentUserId, securityFacade.hasRole(ROLE_ADMIN));
    }

    /**
     * Restricts the operation to administrators only. Used exclusively by
     * {@link #deleteHard} which requires elevated privileges regardless of
     * ownership.
     *
     * @throws AuthorizationException if the authenticated user does not have the
     *                                ADMIN role
     */
    private void validateAdmin() {
        if (!securityFacade.hasRole(ROLE_ADMIN)) {
            log.warn("Security Breach: User attempted to access file with insufficient authorities");
            throw new AuthorizationException(
                "You do not have permission to modify this file.",
                ErrorCode.ACCESS_DENIED);
        }
    }

    /**
     * Validates whether the currently authenticated or anonymous user is authorized
     * to access the given file. Temporary files are checked based on ownership or
     * admin roles, while attached files are checked via the entity-specific
     * {@link FileAccessValidator}.
     *
     * @param file the {@link FileAsset} to check access for
     * @throws AuthorizationException if access is denied
     */
    private void validateFileAccess(FileAsset file) {
        Long currentUserId = securityFacade.getCurrentUserId().orElse(null);

        if (file.getStatus() == FileStatus.TEMPORARY) {
            validateTemporaryFileAccess(file, currentUserId);
            return;
        }

        if (file.getStatus() != FileStatus.ATTACHED) {
            throw new AuthorizationException("Access denied to this file", ErrorCode.ACCESS_DENIED);
        }

        FileAccessValidator validator = accessValidatorResolver.resolve(file.getRelatedEntityType());
        if (!validator.canAccess(file.getRelatedEntityId(), currentUserId, securityFacade::hasRole)) {
            log.warn("Security Breach: user {} attempted to access file {} without permission",
                currentUserId, file.getId());
            throw new AuthorizationException("Access denied to this file", ErrorCode.ACCESS_DENIED);
        }
    }

    /**
     * Temporary files aren't linked to any entity yet ({@code relatedEntityId} is
     * {@code null}), so a per-entity {@link FileAccessValidator} has nothing to
     * check against. Access is granted only to the uploader or privileged roles —
     * used e.g. when the editor previews an image right after upload, before the
     * entity post is saved.
     *
     * @param file          the temporary file asset
     * @param currentUserId the ID of the current user, or {@code null} if
     *                      unauthenticated
     * @throws AuthorizationException if the user is not the owner and lacks
     *                                admin/org privileges
     */
    private void validateTemporaryFileAccess(FileAsset file, Long currentUserId) {
        boolean isOwner = currentUserId != null && currentUserId.equals(file.getUserId());
        boolean isAdmin = securityFacade.hasRole(ROLE_ADMIN) || securityFacade.hasRole(ROLE_ORG);
        if (!isOwner && !isAdmin) {
            log.warn("Security Breach: user {} attempted to access temporary file {} they don't own",
                currentUserId, file.getId());
            throw new AuthorizationException("Access denied to this file", ErrorCode.ACCESS_DENIED);
        }
    }

    /**
     * Resolves the filename to expose in the Content-Disposition header. The
     * original, user-provided filename is shown only to the file's owner; everyone
     * else (regardless of role or entity type) receives the opaque
     * {@code storedFilename}, so identity is never leaked via HTTP headers to
     * reviewers/admins browsing someone else's submission.
     *
     * @param file the file asset being retrieved
     * @return the filename to use for Content-Disposition
     */
    private String resolveDisplayFilename(FileAsset file) {
        Long currentUserId = securityFacade.getCurrentUserId().orElse(null);
        boolean isOwner = currentUserId != null && currentUserId.equals(file.getUserId());
        return isOwner ? file.getOriginalFilename() : file.getStoredFilename();
    }

    /**
     * Builds the URL through which the frontend must always fetch a file's bytes.
     * This is identical for every storage provider — access control happens in
     * {@link #getFileResource}, not at the physical storage layer — so the URL is
     * computed here once instead of duplicated per provider.
     *
     * @param id the database ID of the file asset
     * @return the unified relative file URL (e.g., {@code "/api/v1/files/123"})
     */
    private String buildFileUrl(Long id) {
        return "/api/v1/files/" + id;
    }

    /**
     * Physically moves a file from the temporary upload directory ({@code _tmp/})
     * to its final hierarchical destination based on the entity's storage context.
     *
     * <p>
     * This method is triggered when a file is formally linked to an entity (e.g.,
     * when a News article is saved and its inline images are permanently attached).
     * It updates the file's {@code storageKey} in the database to reflect the new
     * location.
     * </p>
     *
     * @param entityId       the ID of the entity the file is being attached to
     * @param entityType     the type of the entity
     * @param storageContext the hierarchical naming context (e.g., titles) used to
     *                       build the path
     * @param file           the file asset being moved
     */
    private void moveFromTempFolder(Long entityId, RelatedEntityType entityType, StorageContext storageContext,
        FileAsset file) {
        if (storageContext != null && file.getStorageKey() != null && file.getStorageKey().startsWith("_tmp/")) {
            String oldKey = file.getStorageKey();

            FileUploadRequestDto pathDto = FileUploadRequestDto.builder()
                .relatedEntityType(entityType)
                .relatedEntityId(entityId)
                .fileRole(file.getFileRole())
                .storageContext(storageContext)
                .build();

            String newPath = buildRelativePath(pathDto);

            String originalName = (file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank())
                ? file.getOriginalFilename()
                : file.getStoredFilename();

            String safeFilename = generateStoredFilename(originalName, newPath);

            StorageProvider provider = providerResolver.resolve(
                file.getStorageProvider());

            String newKey = provider.moveFile(
                oldKey, newPath + "/" + safeFilename);

            file.setStorageKey(newKey);
            file.setStoredFilename(safeFilename);

            log.debug("Moved file {} to {}", oldKey, newKey);
        }
    }
}
