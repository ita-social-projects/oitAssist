package com.itasocialacademy.oitassist.filemanager.service;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileStatus;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import com.itasocialacademy.oitassist.filemanager.dao.model.FileAsset;
import com.itasocialacademy.oitassist.filemanager.dao.repository.FileRepository;
import com.itasocialacademy.oitassist.filemanager.dto.request.FileUploadRequestDto;
import com.itasocialacademy.oitassist.filemanager.dto.response.FileResponseDto;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileAssetNotFoundException;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileUploadException;
import com.itasocialacademy.oitassist.filemanager.mapper.FileMapper;
import com.itasocialacademy.oitassist.filemanager.providers.interfaces.StorageProvider;
import com.itasocialacademy.oitassist.filemanager.providers.resolver.StorageProviderResolver;
import com.itasocialacademy.oitassist.filemanager.service.interfaces.FileService;
import com.itasocialacademy.oitassist.filemanager.validation.FileValidationStrategyResolver;
import com.itasocialacademy.oitassist.filemanager.validation.interfaces.FileValidationStrategy;
import com.itasocialacademy.oitassist.filemanager.validation.model.ValidationResult;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * Role identifier for administrative users. Used for role-based access control
     * checks throughout this service.
     */
    private static final String ROLE_ADMIN = "ADMIN";

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
                "Not authenticated", ErrorCode.ACCESS_DENIED));

        FileValidationStrategy strategy = validationStrategyResolver.resolve(requestDto.getRelatedEntityType());
        ValidationResult result = strategy.validate(files, requestDto);

        if (!result.valid()) {
            throw new ValidationException(
                String.join(", ", result.violations()),
                ErrorCode.FILE_VALIDATION_FAILED);
        }
        return files.stream()
            .map(file -> uploadSingle(file, requestDto, currentUserId))
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
                "Not authenticated", ErrorCode.ACCESS_DENIED));

        FileAsset file = repository.findById(fileId)
            .orElseThrow(() -> new FileAssetNotFoundException("File not found in the database: " + fileId));

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
            .orElseThrow(() -> new FileAssetNotFoundException("File not found in the database: " + fileId));

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
     * Transitions a batch of {@link FileStatus#TEMPORARY} files to
     * {@link FileStatus#ATTACHED} and establishes their relationship with the
     * specified entity.
     *
     * @param entityId   the ID of the entity to link files to
     * @param entityType the type of the related entity
     * @param fileIds    the IDs of the files to attach; no-op if {@code null} or
     *                   empty
     * @param userId     the ID of the user performing the file linking operation;
     *                   must own all files or possess the ADMIN role
     */
    @Override
    @Transactional
    public void linkFilesToEntity(Long entityId, RelatedEntityType entityType, List<Long> fileIds, Long userId) {
        if (fileIds == null || fileIds.isEmpty()) {
            log.debug("No files to link to entity {} with id={}", entityType, entityId);
            return;
        }

        boolean isAdmin = securityFacade.hasRole(ROLE_ADMIN);
        List<FileAsset> files = repository.findAllById(fileIds);

        for (FileAsset file : files) {
            checkOwnerOrAdmin(file.getUserId(), userId, isAdmin);
            if (file.getStatus() == FileStatus.TEMPORARY) {
                file.setStatus(FileStatus.ATTACHED);
                file.setRelatedEntityId(entityId);
                file.setRelatedEntityType(entityType);
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
     * file using the explicitly provided userId, since this runs outside the HTTP
     * Security Context (async event listener).
     *
     * @param fileIds the IDs of files to soft-delete
     * @param userId  the ID of the user who triggered detach (from the event)
     */
    @Override
    @Transactional
    public void detachFiles(List<Long> fileIds, Long userId) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }

        boolean isAdmin = securityFacade.hasRole(ROLE_ADMIN);
        List<FileAsset> files = repository.findAllById(fileIds);

        for (FileAsset file : files) {
            checkOwnerOrAdmin(file.getUserId(), userId, isAdmin);
            file.setStatus(FileStatus.SOFT_DELETED);
            file.setDeletedAt(OffsetDateTime.now());
        }
        repository.saveAll(files);
    }

    /**
     * Returns all {@link FileStatus#ATTACHED} files for the given entity, enriched
     * with their publicly accessible URLs resolved from the storage provider.
     *
     * @param entityType the type of the related entity
     * @param entityId   the ID of the related entity
     * @return list of file DTOs with resolved URLs
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
                StorageProvider provider = providerResolver.resolve(file.getStorageProvider());
                dto.setUrl(provider.getFileUrl(file.getStorageKey()));
                return dto;
            })
            .toList();
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
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String storedFilename = generateStoredFilename(originalFilename);
        String relativePath = buildRelativePath(requestDto);

        StorageProvider provider = providerResolver.resolveDefault();

        String storageKey;
        try (var inputStream = file.getInputStream()) {
            storageKey = provider.upload(inputStream, storedFilename, relativePath);
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

        FileAsset saved = repository.save(fileAsset);
        FileResponseDto dto = fileMapper.toDto(saved);
        dto.setUrl(provider.getFileUrl(storageKey));
        return dto;
    }

    /**
     * Generates a unique filename by prepending a UUID to the original file
     * extension.
     *
     * @param originalFilename the original name of the uploaded file
     * @return a unique filename safe for storage
     */
    private String generateStoredFilename(String originalFilename) {
        return UUID.randomUUID() + extractExtensionWithDot(originalFilename);
    }

    /**
     * Extracts the file extension including the leading dot (e.g., {@code ".pdf"}).
     *
     * @param originalFilename the original filename
     * @return the extension with dot, or an empty string if no extension is present
     */
    private String extractExtensionWithDot(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }

    /**
     * Builds the relative storage directory path derived from the entity type.
     *
     * @param requestDto the upload request containing the entity type
     * @return a lowercase subdirectory name (e.g., {@code "news"}, {@code "task"})
     */
    private String buildRelativePath(FileUploadRequestDto requestDto) {
        return requestDto.getRelatedEntityType().name().toLowerCase();
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
}
