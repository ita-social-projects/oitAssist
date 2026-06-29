package com.itasocialacademy.oitassist.filemanager.service;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileRole;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import com.itasocialacademy.oitassist.filemanager.dao.model.FileAsset;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileStatus;
import com.itasocialacademy.oitassist.filemanager.dao.repository.FileRepository;
import com.itasocialacademy.oitassist.filemanager.dto.request.FileUploadRequestDto;
import com.itasocialacademy.oitassist.filemanager.dto.response.FileResponseDto;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileAssetNotFoundException;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileUploadException;
import com.itasocialacademy.oitassist.filemanager.exceptions.UnsupportedStorageException;
import com.itasocialacademy.oitassist.filemanager.mapper.FileMapper;
import com.itasocialacademy.oitassist.filemanager.providers.interfaces.StorageProvider;
import com.itasocialacademy.oitassist.filemanager.providers.resolver.StorageProviderResolver;
import com.itasocialacademy.oitassist.filemanager.validation.interfaces.FilePolicy;
import com.itasocialacademy.oitassist.filemanager.validation.resolvers.FilePolicyResolver;
import com.itasocialacademy.oitassist.filemanager.validation.resolvers.FileValidationStrategyResolver;
import com.itasocialacademy.oitassist.filemanager.validation.interfaces.FileValidationStrategy;
import com.itasocialacademy.oitassist.filemanager.validation.model.ValidationResult;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private StorageProviderResolver providerResolver;

    @Mock
    private StorageProvider storageProvider;

    @Mock
    private FileValidationStrategyResolver validationStrategyResolver;

    @Mock
    private FileValidationStrategy validationStrategy;

    @Mock
    private FileMapper fileMapper;

    @Mock
    private SecurityFacade securityFacade;

    @InjectMocks
    private FileServiceImpl fileService;

    @Mock
    private FilePolicyResolver filePolicyResolver;

    @Mock
    private FilePolicy filePolicy;

    private Long fileId;
    private Long nonExistentId;
    private Long userId;
    private FileAsset existingFile;

    @BeforeEach
    void setUp() {
        fileId = 1L;
        userId = 42L;
        existingFile = new FileAsset();
        existingFile.setId(fileId);
        existingFile.setStatus(FileStatus.ATTACHED);
        existingFile.setStorageProvider(StorageProviderType.LOCAL);
        existingFile.setUserId(userId);

        nonExistentId = 999L;
    }

    // --- Upload Tests ---

    @Test
    void upload_ShouldReturnEmptyList_WhenNoFilesProvided() {
        FileUploadRequestDto request = uploadRequest(RelatedEntityType.NEWS, 1L);
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(validationStrategyResolver.resolve(any(), any())).thenReturn(validationStrategy);
        when(filePolicyResolver.resolve(any(), any())).thenReturn(filePolicy);
        when(validationStrategy.validate(any(), any(), any())).thenReturn(ValidationResult.ok());

        List<FileResponseDto> result = fileService.upload(List.of(), request);

        assertTrue(result.isEmpty());
        verifyNoInteractions(providerResolver, fileRepository);
    }

    @Test
    void upload_ShouldHandleFilesWithoutExtension() {
        MultipartFile file = new MockMultipartFile("file", "filename_without_extension", "text/plain", new byte[10]);
        FileUploadRequestDto request = uploadRequest(RelatedEntityType.NEWS, 1L);
        ArgumentCaptor<FileAsset> captor = ArgumentCaptor.forClass(FileAsset.class);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(validationStrategyResolver.resolve(any(), any())).thenReturn(validationStrategy);
        when(filePolicyResolver.resolve(any(), any())).thenReturn(filePolicy);
        when(validationStrategy.validate(any(), any(), any())).thenReturn(ValidationResult.ok());
        when(providerResolver.resolveDefault()).thenReturn(storageProvider);
        when(storageProvider.upload(any(), anyString(), anyString())).thenReturn("news/stored");
        when(storageProvider.getType()).thenReturn(StorageProviderType.LOCAL);
        when(fileRepository.save(any())).thenReturn(new FileAsset());
        when(fileMapper.toDto(any())).thenReturn(new FileResponseDto());

        fileService.upload(List.of(file), request);

        verify(fileRepository).save(captor.capture());
        FileAsset saved = captor.getValue();
        assertFalse(saved.getStoredFilename().contains("."),
            "Stored filename should not have a dot if original didn't");
    }

    @Test
    void upload_ShouldHandleNullOriginalFilename() {
        MultipartFile file = new MockMultipartFile("file", null, "text/plain", new byte[10]);
        FileUploadRequestDto request = uploadRequest(RelatedEntityType.NEWS, 1L);
        ArgumentCaptor<FileAsset> captor = ArgumentCaptor.forClass(FileAsset.class);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(validationStrategyResolver.resolve(any(), any())).thenReturn(validationStrategy);
        when(filePolicyResolver.resolve(any(), any())).thenReturn(filePolicy);
        when(validationStrategy.validate(any(), any(), any())).thenReturn(ValidationResult.ok());
        when(providerResolver.resolveDefault()).thenReturn(storageProvider);
        when(storageProvider.upload(any(), anyString(), anyString())).thenReturn("news/stored");
        when(storageProvider.getType()).thenReturn(StorageProviderType.LOCAL);
        when(fileRepository.save(any())).thenReturn(new FileAsset());
        when(fileMapper.toDto(any())).thenReturn(new FileResponseDto());

        fileService.upload(List.of(file), request);

        verify(fileRepository).save(captor.capture());
        FileAsset saved = captor.getValue();
        assertEquals("", saved.getOriginalFilename());
        assertFalse(saved.getStoredFilename().contains("."));
    }

    @Test
    void upload_ShouldHandleFilesWithMultipleDots() {
        MultipartFile file = new MockMultipartFile("file", "archive.tar.gz", "application/gzip", new byte[10]);
        FileUploadRequestDto request = uploadRequest(RelatedEntityType.NEWS, 1L);
        ArgumentCaptor<FileAsset> captor = ArgumentCaptor.forClass(FileAsset.class);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(validationStrategyResolver.resolve(any(), any())).thenReturn(validationStrategy);
        when(filePolicyResolver.resolve(any(), any())).thenReturn(filePolicy);
        when(validationStrategy.validate(any(), any(), any())).thenReturn(ValidationResult.ok());
        when(providerResolver.resolveDefault()).thenReturn(storageProvider);
        when(storageProvider.upload(any(), anyString(), anyString())).thenReturn("news/stored.gz");
        when(storageProvider.getType()).thenReturn(StorageProviderType.LOCAL);
        when(fileRepository.save(any())).thenReturn(new FileAsset());
        when(fileMapper.toDto(any())).thenReturn(new FileResponseDto());

        fileService.upload(List.of(file), request);

        verify(fileRepository).save(captor.capture());
        FileAsset saved = captor.getValue();
        assertTrue(saved.getStoredFilename().endsWith(".gz"));
        assertFalse(saved.getStoredFilename().endsWith(".tar.gz"), "Should only extract the last extension");
    }

    @Test
    void upload_ShouldHandleFilenameEndingWithDot() {
        MultipartFile file = new MockMultipartFile("file", "filename.", "text/plain", new byte[10]);
        FileUploadRequestDto request = uploadRequest(RelatedEntityType.NEWS, 1L);
        ArgumentCaptor<FileAsset> captor = ArgumentCaptor.forClass(FileAsset.class);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(validationStrategyResolver.resolve(any(), any())).thenReturn(validationStrategy);
        when(filePolicyResolver.resolve(any(), any())).thenReturn(filePolicy);
        when(validationStrategy.validate(any(), any(), any())).thenReturn(ValidationResult.ok());
        when(providerResolver.resolveDefault()).thenReturn(storageProvider);
        when(storageProvider.upload(any(), anyString(), anyString())).thenReturn("news/stored.");
        when(storageProvider.getType()).thenReturn(StorageProviderType.LOCAL);
        when(fileRepository.save(any())).thenReturn(new FileAsset());
        when(fileMapper.toDto(any())).thenReturn(new FileResponseDto());

        fileService.upload(List.of(file), request);

        verify(fileRepository).save(captor.capture());
        FileAsset saved = captor.getValue();
        assertTrue(saved.getStoredFilename().endsWith("."));
    }

    @Test
    void upload_ShouldHandleFilenameBeingJustADot() {
        MultipartFile file = new MockMultipartFile("file", ".", "text/plain", new byte[10]);
        FileUploadRequestDto request = uploadRequest(RelatedEntityType.NEWS, 1L);
        ArgumentCaptor<FileAsset> captor = ArgumentCaptor.forClass(FileAsset.class);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(validationStrategyResolver.resolve(any(), any())).thenReturn(validationStrategy);
        when(filePolicyResolver.resolve(any(), any())).thenReturn(filePolicy);
        when(validationStrategy.validate(any(), any(), any())).thenReturn(ValidationResult.ok());
        when(providerResolver.resolveDefault()).thenReturn(storageProvider);
        when(storageProvider.upload(any(), anyString(), anyString())).thenReturn("news/stored.");
        when(storageProvider.getType()).thenReturn(StorageProviderType.LOCAL);
        when(fileRepository.save(any())).thenReturn(new FileAsset());
        when(fileMapper.toDto(any())).thenReturn(new FileResponseDto());

        fileService.upload(List.of(file), request);

        verify(fileRepository).save(captor.capture());
        FileAsset saved = captor.getValue();
        assertTrue(saved.getStoredFilename().endsWith("."));
    }

    @Test
    void upload_ShouldReturnMappedDtos_WhenFilesAreValid() {
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[512]);
        FileUploadRequestDto request = uploadRequest(RelatedEntityType.NEWS, 1L);
        FileAsset savedAsset = new FileAsset();
        FileResponseDto expectedDto = FileResponseDto.builder().id(10L).build();

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(validationStrategyResolver.resolve(any(), any())).thenReturn(validationStrategy);
        when(filePolicyResolver.resolve(any(), any())).thenReturn(filePolicy);
        when(validationStrategy.validate(any(), any(), any())).thenReturn(ValidationResult.ok());
        when(providerResolver.resolveDefault()).thenReturn(storageProvider);
        when(storageProvider.upload(any(), anyString(), anyString())).thenReturn("news/stored.jpg");
        when(storageProvider.getType()).thenReturn(StorageProviderType.LOCAL);
        when(fileRepository.save(any())).thenReturn(savedAsset);
        when(fileMapper.toDto(savedAsset)).thenReturn(expectedDto);

        List<FileResponseDto> result = fileService.upload(List.of(file), request);

        assertEquals(List.of(expectedDto), result);
        verify(validationStrategyResolver).resolve(RelatedEntityType.NEWS, FileRole.GENERIC);
        verify(filePolicyResolver).resolve(RelatedEntityType.NEWS, FileRole.GENERIC);
        verify(providerResolver).resolveDefault();
        verify(fileRepository).save(any());
        verify(fileMapper).toDto(savedAsset);
    }

    @Test
    void upload_ShouldSaveFileAssetWithCorrectMetadata_WhenUploading() {
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[512]);
        FileUploadRequestDto request = uploadRequest(RelatedEntityType.NEWS, 5L);
        ArgumentCaptor<FileAsset> captor = ArgumentCaptor.forClass(FileAsset.class);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(validationStrategyResolver.resolve(any(), any())).thenReturn(validationStrategy);
        when(filePolicyResolver.resolve(any(), any())).thenReturn(filePolicy);
        when(validationStrategy.validate(any(), any(), any())).thenReturn(ValidationResult.ok());
        when(providerResolver.resolveDefault()).thenReturn(storageProvider);
        when(storageProvider.upload(any(), anyString(), anyString())).thenReturn("news/stored.jpg");
        when(storageProvider.getType()).thenReturn(StorageProviderType.LOCAL);
        when(fileRepository.save(any())).thenReturn(new FileAsset());
        when(fileMapper.toDto(any())).thenReturn(new FileResponseDto());

        fileService.upload(List.of(file), request);

        verify(fileRepository).save(captor.capture());
        FileAsset saved = captor.getValue();
        assertAll(
            () -> assertEquals("photo.jpg", saved.getOriginalFilename()),
            () -> assertTrue(saved.getStoredFilename().endsWith(".jpg")),
            () -> assertEquals("news/stored.jpg", saved.getStorageKey()),
            () -> assertEquals(StorageProviderType.LOCAL, saved.getStorageProvider()),
            () -> assertEquals(RelatedEntityType.NEWS, saved.getRelatedEntityType()),
            () -> assertEquals(5L, saved.getRelatedEntityId()),
            () -> assertEquals(userId, saved.getUserId()),
            () -> assertEquals("image/jpeg", saved.getMimeType()),
            () -> assertEquals(512L, saved.getSize()));
    }

    @Test
    void upload_ShouldPassRelatedEntityTypeNameAsRelativePath_WhenUploading() {
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[512]);
        FileUploadRequestDto request = uploadRequest(RelatedEntityType.TASK, 1L);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(validationStrategyResolver.resolve(any(), any())).thenReturn(validationStrategy);
        when(filePolicyResolver.resolve(any(), any())).thenReturn(filePolicy);
        when(validationStrategy.validate(any(), any(), any())).thenReturn(ValidationResult.ok());
        when(providerResolver.resolveDefault()).thenReturn(storageProvider);
        when(storageProvider.upload(any(), anyString(), anyString())).thenReturn("task/stored.jpg");
        when(storageProvider.getType()).thenReturn(StorageProviderType.LOCAL);
        when(fileRepository.save(any())).thenReturn(new FileAsset());
        when(fileMapper.toDto(any())).thenReturn(new FileResponseDto());

        fileService.upload(List.of(file), request);

        verify(storageProvider).upload(any(), anyString(), eq("task"));
    }

    @Test
    void upload_ShouldSetStatusAttached_WhenRelatedEntityIdIsProvided() {
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[512]);
        FileUploadRequestDto request = uploadRequest(RelatedEntityType.NEWS, 1L);
        ArgumentCaptor<FileAsset> captor = ArgumentCaptor.forClass(FileAsset.class);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(validationStrategyResolver.resolve(any(), any())).thenReturn(validationStrategy);
        when(filePolicyResolver.resolve(any(), any())).thenReturn(filePolicy);
        when(validationStrategy.validate(any(), any(), any())).thenReturn(ValidationResult.ok());
        when(providerResolver.resolveDefault()).thenReturn(storageProvider);
        when(storageProvider.upload(any(), anyString(), anyString())).thenReturn("news/stored.jpg");
        when(storageProvider.getType()).thenReturn(StorageProviderType.LOCAL);
        when(fileRepository.save(any())).thenReturn(new FileAsset());
        when(fileMapper.toDto(any())).thenReturn(new FileResponseDto());

        fileService.upload(List.of(file), request);

        verify(fileRepository).save(captor.capture());
        assertEquals(FileStatus.ATTACHED, captor.getValue().getStatus());
    }

    @Test
    void upload_ShouldSetStatusTemporary_WhenRelatedEntityIdIsNull() {
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[512]);
        FileUploadRequestDto request = uploadRequest(RelatedEntityType.NEWS, null);
        ArgumentCaptor<FileAsset> captor = ArgumentCaptor.forClass(FileAsset.class);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(validationStrategyResolver.resolve(any(), any())).thenReturn(validationStrategy);
        when(filePolicyResolver.resolve(any(), any())).thenReturn(filePolicy);
        when(validationStrategy.validate(any(), any(), any())).thenReturn(ValidationResult.ok());
        when(providerResolver.resolveDefault()).thenReturn(storageProvider);
        when(storageProvider.upload(any(), anyString(), anyString())).thenReturn("news/stored.jpg");
        when(storageProvider.getType()).thenReturn(StorageProviderType.LOCAL);
        when(fileRepository.save(any())).thenReturn(new FileAsset());
        when(fileMapper.toDto(any())).thenReturn(new FileResponseDto());

        fileService.upload(List.of(file), request);

        verify(fileRepository).save(captor.capture());
        assertEquals(FileStatus.TEMPORARY, captor.getValue().getStatus());
    }

    @Test
    void upload_ShouldThrowValidationException_WhenValidationFails() {
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[512]);
        FileUploadRequestDto request = uploadRequest(RelatedEntityType.NEWS, 1L);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(validationStrategyResolver.resolve(any(), any())).thenReturn(validationStrategy);
        when(filePolicyResolver.resolve(any(), any())).thenReturn(filePolicy);
        when(validationStrategy.validate(any(), any(), any())).thenReturn(
            ValidationResult.fail("File size exceeded"));

        List<MultipartFile> files = List.of(file);
        ValidationException exception = assertThrows(
            ValidationException.class, () -> fileService.upload(files, request));

        assertTrue(exception.getMessage().contains("File size exceeded"));
        verifyNoInteractions(providerResolver);
        verify(fileRepository, never()).save(any());
    }

    @Test
    void upload_ShouldThrowFileUploadException_WhenInputStreamThrowsIOException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        FileUploadRequestDto request = uploadRequest(RelatedEntityType.NEWS, 1L);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(file.getOriginalFilename()).thenReturn("photo.jpg");
        when(file.getInputStream()).thenThrow(new IOException("disk error"));
        when(validationStrategyResolver.resolve(any(), any())).thenReturn(validationStrategy);
        when(filePolicyResolver.resolve(any(), any())).thenReturn(filePolicy);
        when(validationStrategy.validate(any(), any(), any())).thenReturn(ValidationResult.ok());
        when(providerResolver.resolveDefault()).thenReturn(storageProvider);

        List<MultipartFile> files = List.of(file);
        FileUploadException exception = assertThrows(
            FileUploadException.class, () -> fileService.upload(files, request));

        assertInstanceOf(IOException.class, exception.getCause());
        verify(fileRepository, never()).save(any());
    }

    @Test
    void upload_ShouldProcessEachFileIndependently_WhenMultipleFilesProvided() {
        MultipartFile file1 = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[256]);
        MultipartFile file2 = new MockMultipartFile("file", "b.jpg", "image/jpeg", new byte[512]);
        FileUploadRequestDto request = uploadRequest(RelatedEntityType.NEWS, 1L);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(validationStrategyResolver.resolve(any(), any())).thenReturn(validationStrategy);
        when(filePolicyResolver.resolve(any(), any())).thenReturn(filePolicy);
        when(validationStrategy.validate(any(), any(), any())).thenReturn(ValidationResult.ok());
        when(providerResolver.resolveDefault()).thenReturn(storageProvider);
        when(storageProvider.upload(any(), anyString(), anyString()))
            .thenReturn("news/a.jpg")
            .thenReturn("news/b.jpg");
        when(storageProvider.getType()).thenReturn(StorageProviderType.LOCAL);
        when(fileRepository.save(any())).thenReturn(new FileAsset());
        when(fileMapper.toDto(any()))
            .thenReturn(FileResponseDto.builder().id(1L).build())
            .thenReturn(FileResponseDto.builder().id(2L).build());

        List<FileResponseDto> result = fileService.upload(List.of(file1, file2), request);

        assertEquals(2, result.size());
        verify(storageProvider, times(2)).upload(any(), anyString(), anyString());
        verify(fileRepository, times(2)).save(any());
        verify(fileMapper, times(2)).toDto(any());
    }

    @Test
    void upload_ShouldSucceed_WhenUserIsAuthenticated() {
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[10]);
        FileUploadRequestDto request = uploadRequest(RelatedEntityType.NEWS, 1L);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(validationStrategyResolver.resolve(any(), any())).thenReturn(validationStrategy);
        when(filePolicyResolver.resolve(any(), any())).thenReturn(filePolicy);
        when(validationStrategy.validate(any(), any(), any())).thenReturn(ValidationResult.ok());
        when(providerResolver.resolveDefault()).thenReturn(storageProvider);
        when(storageProvider.upload(any(), anyString(), anyString())).thenReturn("path/key");
        when(storageProvider.getType()).thenReturn(StorageProviderType.LOCAL);
        when(fileRepository.save(any())).thenReturn(existingFile);
        when(fileMapper.toDto(any())).thenReturn(new FileResponseDto());

        assertDoesNotThrow(() -> fileService.upload(List.of(file), request));
        verify(securityFacade).getCurrentUserId();
    }

    @Test
    void upload_ShouldThrowAuthorizationException_WhenUserNotAuthenticated() {
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[10]);
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.empty());

        List<MultipartFile> files = List.of(file);
        FileUploadRequestDto request = uploadRequest(RelatedEntityType.NEWS, 1L);
        assertThrows(AuthorizationException.class, () -> fileService.upload(files, request));

        verifyNoInteractions(fileRepository, storageProvider);
    }

    @Test
    void upload_ShouldDefaultToGenericRole_WhenFileRoleIsNotProvided() {
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[512]);
        FileUploadRequestDto request = FileUploadRequestDto.builder()
            .relatedEntityType(RelatedEntityType.NEWS)
            .relatedEntityId(1L)
            .build();

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(validationStrategyResolver.resolve(any(), any())).thenReturn(validationStrategy);
        when(filePolicyResolver.resolve(any(), any())).thenReturn(filePolicy);
        when(validationStrategy.validate(any(), any(), any())).thenReturn(ValidationResult.ok());
        when(providerResolver.resolveDefault()).thenReturn(storageProvider);
        when(storageProvider.upload(any(), anyString(), anyString())).thenReturn("news/stored.jpg");
        when(storageProvider.getType()).thenReturn(StorageProviderType.LOCAL);
        when(fileRepository.save(any())).thenReturn(new FileAsset());
        when(fileMapper.toDto(any())).thenReturn(new FileResponseDto());

        fileService.upload(List.of(file), request);

        verify(validationStrategyResolver).resolve(RelatedEntityType.NEWS, FileRole.GENERIC);
        verify(filePolicyResolver).resolve(RelatedEntityType.NEWS, FileRole.GENERIC);
    }

    @Test
    void upload_ShouldResolveStrategyAndPolicyByRole_WhenTaskRoleProvided() {
        MultipartFile file = new MockMultipartFile("file", "doc.docx", "application/vnd.openxmlformats", new byte[10]);
        FileRole role = FileRole.REFERENCE;
        FileUploadRequestDto request = uploadRequest(RelatedEntityType.TASK, 1L, role);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(validationStrategyResolver.resolve(RelatedEntityType.TASK, role)).thenReturn(validationStrategy);
        when(filePolicyResolver.resolve(RelatedEntityType.TASK, role)).thenReturn(filePolicy);
        when(validationStrategy.validate(any(), any(), any())).thenReturn(ValidationResult.ok());
        when(providerResolver.resolveDefault()).thenReturn(storageProvider);
        when(storageProvider.upload(any(), anyString(), anyString())).thenReturn("task/stored.docx");
        when(storageProvider.getType()).thenReturn(StorageProviderType.LOCAL);
        when(fileRepository.save(any())).thenReturn(new FileAsset());
        when(fileMapper.toDto(any())).thenReturn(new FileResponseDto());

        fileService.upload(List.of(file), request);

        verify(validationStrategyResolver).resolve(RelatedEntityType.TASK, role);
        verify(filePolicyResolver).resolve(RelatedEntityType.TASK, role);
        verify(validationStrategy).validate(any(), eq(request), eq(filePolicy));
    }

    @Test
    void upload_ShouldThrowValidationException_WhenNoPolicyRegisteredForCombination() {
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[512]);
        FileUploadRequestDto request = uploadRequest(RelatedEntityType.TASK, 1L, FileRole.PROBLEM);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(validationStrategyResolver.resolve(any(), any())).thenReturn(validationStrategy);
        when(filePolicyResolver.resolve(any(), any()))
            .thenThrow(new ValidationException("No file policy registered for: TASK/PROBLEM", ErrorCode.FILE_VALIDATION_FAILED));

        List<MultipartFile> files = List.of(file);
        assertThrows(ValidationException.class, () -> fileService.upload(files, request));

        verifyNoInteractions(providerResolver);
        verify(fileRepository, never()).save(any());
    }

    // --- Soft Delete Tests ---

    @Test
    void deleteSoft_ShouldUpdateStatusAndTimestamp_WhenFileExists() {
        String originalKey = "news/photo.jpg";
        existingFile.setStorageKey(originalKey);
        existingFile.setUserId(userId);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(existingFile));
        when(securityFacade.hasRole("ADMIN")).thenReturn(false);

        fileService.deleteSoft(fileId);

        ArgumentCaptor<FileAsset> fileCaptor = ArgumentCaptor.forClass(FileAsset.class);
        verify(fileRepository).save(fileCaptor.capture());

        FileAsset savedFile = fileCaptor.getValue();
        assertEquals(FileStatus.SOFT_DELETED, savedFile.getStatus());
        assertNotNull(savedFile.getDeletedAt());
        assertEquals(originalKey, savedFile.getStorageKey(), "Storage key must remain intact on soft delete");
    }

    @Test
    void deleteSoft_ShouldThrowException_WhenFileNotFound() {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(fileRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(FileAssetNotFoundException.class, () -> fileService.deleteSoft(nonExistentId));
        verify(fileRepository, never()).save(any());
    }

    @Test
    void deleteSoft_ShouldSucceed_WhenUserIsOwner() {
        existingFile.setUserId(userId);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(existingFile));
        when(securityFacade.hasRole("ADMIN")).thenReturn(false);

        fileService.deleteSoft(fileId);

        verify(fileRepository).save(any());
        assertEquals(FileStatus.SOFT_DELETED, existingFile.getStatus());
    }

    @Test
    void deleteSoft_ShouldSucceed_WhenUserIsNotOwnerButIsAdmin() {
        Long otherUserId = 99L;
        existingFile.setUserId(otherUserId);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(existingFile));
        when(securityFacade.hasRole("ADMIN")).thenReturn(true);

        fileService.deleteSoft(fileId);

        verify(fileRepository).save(any());
        assertEquals(FileStatus.SOFT_DELETED, existingFile.getStatus());
    }

    @Test
    void deleteSoft_ShouldThrowAuthorizationException_WhenUserIsNeitherOwnerNorAdmin() {
        Long otherUserId = 99L;
        existingFile.setUserId(otherUserId);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(existingFile));
        when(securityFacade.hasRole("ADMIN")).thenReturn(false);

        assertThrows(AuthorizationException.class, () -> fileService.deleteSoft(fileId));
        verify(fileRepository, never()).save(any());
    }

    // --- Hard Delete Tests ---

    @Test
    void deleteHard_ShouldTriggerPhysicalDeletionAndClearPath() {
        String testPath = "/tmp/storage/test.txt";
        existingFile.setStorageKey(testPath);

        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(existingFile));
        when(providerResolver.resolve(StorageProviderType.LOCAL)).thenReturn(storageProvider);

        fileService.deleteHard(fileId);

        verify(storageProvider).deletePhysical(testPath);

        ArgumentCaptor<FileAsset> captor = ArgumentCaptor.forClass(FileAsset.class);
        verify(fileRepository).save(captor.capture());

        FileAsset result = captor.getValue();
        assertAll(
            () -> assertEquals(FileStatus.HARD_DELETED, result.getStatus()),
            () -> assertEquals("", result.getStorageKey()),
            () -> assertNotNull(result.getDeletedAt(), "deletedAt should be set on successful hard delete"));
    }

    @Test
    void deleteHard_ShouldThrowUnsupportedStorageException_WhenResolverFails() {
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(existingFile));
        when(providerResolver.resolve(any())).thenThrow(UnsupportedStorageException.class);
        when(securityFacade.hasRole("ADMIN")).thenReturn(true);

        assertThrows(UnsupportedStorageException.class, () -> fileService.deleteHard(fileId));

        verify(fileRepository, never()).save(any());
    }

    @Test
    void deleteHard_ShouldPerformDeletion_WhenUserIsAdmin() {
        existingFile.setStorageKey("old/key");
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(existingFile));
        when(providerResolver.resolve(any())).thenReturn(storageProvider);
        when(securityFacade.hasRole("ADMIN")).thenReturn(true);

        fileService.deleteHard(fileId);

        verify(storageProvider).deletePhysical("old/key");
        verify(fileRepository).save(any());
    }

    @Test
    void deleteHard_ShouldSetStatusToFailed_WhenPhysicalDeletionThrowsException() {
        String testPath = "path/to/fail";
        existingFile.setStorageKey(testPath);
        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(providerResolver.resolve(any())).thenReturn(storageProvider);
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(existingFile));

        doThrow(new RuntimeException("IO Error")).when(storageProvider).deletePhysical(anyString());

        fileService.deleteHard(fileId);

        ArgumentCaptor<FileAsset> captor = ArgumentCaptor.forClass(FileAsset.class);
        verify(fileRepository).save(captor.capture());

        FileAsset result = captor.getValue();
        assertAll(
            () -> assertEquals(FileStatus.FAILED, result.getStatus()),
            () -> assertEquals(testPath, result.getStorageKey(),
                "Storage key should NOT be cleared if physical deletion fails"),
            () -> assertNull(result.getDeletedAt(), "deletedAt should NOT be set if physical deletion fails"));
    }

    @Test
    void deleteHard_ShouldThrowFileAssetNotFoundException_WhenFileMissingInDb() {
        when(fileRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(FileAssetNotFoundException.class, () -> fileService.deleteHard(nonExistentId));

        verifyNoInteractions(providerResolver, storageProvider);
        verify(fileRepository, never()).save(any());
    }

    @Test
    void deleteHard_ShouldThrowException_WhenUserIsNotAdmin() {
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(existingFile));
        when(securityFacade.hasRole("ADMIN")).thenReturn(false);

        assertThrows(AuthorizationException.class, () -> fileService.deleteHard(fileId));

        verifyNoInteractions(providerResolver, storageProvider);
    }

    // --- Helpers ---

    private static FileUploadRequestDto uploadRequest(RelatedEntityType type, Long relatedEntityId) {
        return uploadRequest(type, relatedEntityId, FileRole.GENERIC);
    }

    private static FileUploadRequestDto uploadRequest(RelatedEntityType type, Long relatedEntityId, FileRole role) {
        return FileUploadRequestDto.builder()
            .relatedEntityType(type)
            .relatedEntityId(relatedEntityId)
            .fileRole(role)
            .build();
    }
}