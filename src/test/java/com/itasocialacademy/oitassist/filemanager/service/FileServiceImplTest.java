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
import com.itasocialacademy.oitassist.filemanager.dto.request.UpdateFileRoleRequestDto;
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
import java.util.Set;

import com.itasocialacademy.oitassist.filemanager.access.FileAccessValidator;
import com.itasocialacademy.oitassist.filemanager.api.dto.FileDetailsDTO;
import com.itasocialacademy.oitassist.filemanager.dto.response.FileDownloadDto;
import com.itasocialacademy.oitassist.filemanager.validation.resolvers.FileAccessValidatorResolver;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import com.itasocialacademy.oitassist.filemanager.validation.enums.AllowedExtension;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    private static final String ROLE_ADMIN = "ADMIN";
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

    @Mock
    private FileAccessValidatorResolver accessValidatorResolver;

    @Mock
    private FileAccessValidator accessValidator;

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
            .thenThrow(new ValidationException("No file policy registered for: TASK/PROBLEM",
                ErrorCode.FILE_VALIDATION_FAILED));

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

    // --- getFilesByEntity(entityType, entityId, roles) Tests ---

    @Test
    void getFilesByEntity_withRoles_ShouldReturnFilteredFilesWithUrls() {
        FileAsset problemFile = new FileAsset();
        problemFile.setId(1L);
        problemFile.setStorageProvider(StorageProviderType.LOCAL);
        problemFile.setStorageKey("task/problem.pdf");
        problemFile.setOriginalFilename("problem.pdf");
        problemFile.setMimeType("application/pdf");
        problemFile.setSize(2048L);
        problemFile.setFileRole(FileRole.PROBLEM);

        Set<FileRole> roles = Set.of(FileRole.PROBLEM, FileRole.REFERENCE);
        FileDetailsDTO expectedDto = new FileDetailsDTO(1L, "problem.pdf", "application/pdf", 2048L, "PROBLEM",
            "/api/v1/files/download/1");

        when(fileRepository.findAll(any(Specification.class))).thenReturn(List.of(problemFile));
        when(fileMapper.toDetails(problemFile, "/api/v1/files/download/1")).thenReturn(expectedDto);

        List<FileDetailsDTO> result = fileService.getFilesByEntity(RelatedEntityType.TASK, 10L, roles);

        assertEquals(1, result.size());
        assertEquals(expectedDto, result.getFirst());
        verify(fileRepository).findAll(any(Specification.class));
        verify(fileMapper).toDetails(problemFile, "/api/v1/files/download/1");
    }

    @Test
    void getFilesByEntity_withRoles_ShouldReturnEmptyList_WhenNoFilesMatch() {
        Set<FileRole> roles = Set.of(FileRole.SOLUTION);

        when(fileRepository.findAll(any(Specification.class))).thenReturn(List.of());

        List<FileDetailsDTO> result = fileService.getFilesByEntity(RelatedEntityType.TASK, 10L, roles);

        assertTrue(result.isEmpty());
        verify(fileRepository).findAll(any(Specification.class));
        verifyNoInteractions(providerResolver, fileMapper);
    }

    @Test
    void getFilesByEntity_withRoles_ShouldReturnMultipleFiles_WhenMultipleRolesMatch() {
        FileAsset problemFile = new FileAsset();
        problemFile.setId(1L);
        problemFile.setStorageProvider(StorageProviderType.LOCAL);
        problemFile.setStorageKey("task/problem.pdf");
        problemFile.setFileRole(FileRole.PROBLEM);

        FileAsset referenceFile = new FileAsset();
        referenceFile.setId(2L);
        referenceFile.setStorageProvider(StorageProviderType.LOCAL);
        referenceFile.setStorageKey("task/reference.docx");
        referenceFile.setFileRole(FileRole.REFERENCE);

        Set<FileRole> roles = Set.of(FileRole.PROBLEM, FileRole.REFERENCE);
        FileDetailsDTO problemDto = new FileDetailsDTO(1L, "problem.pdf", "application/pdf", 1024L, "PROBLEM",
            "/api/v1/files/download/1");
        FileDetailsDTO referenceDto = new FileDetailsDTO(2L, "reference.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 2048L, "REFERENCE",
            "/api/v1/files/download/2");

        when(fileRepository.findAll(any(Specification.class))).thenReturn(List.of(problemFile, referenceFile));
        when(fileMapper.toDetails(problemFile, "/api/v1/files/download/1")).thenReturn(problemDto);
        when(fileMapper.toDetails(referenceFile, "/api/v1/files/download/2")).thenReturn(referenceDto);

        List<FileDetailsDTO> result = fileService.getFilesByEntity(RelatedEntityType.TASK, 10L, roles);

        assertEquals(2, result.size());
        assertEquals(problemDto, result.get(0));
        assertEquals(referenceDto, result.get(1));
        verify(fileMapper, times(2)).toDetails(any(), anyString());
    }

    @Test
    void getFilesByEntity_withRoles_ShouldFilterBySingleRole() {
        FileAsset solutionFile = new FileAsset();
        solutionFile.setId(3L);
        solutionFile.setStorageProvider(StorageProviderType.LOCAL);
        solutionFile.setStorageKey("task/solution.zip");
        solutionFile.setFileRole(FileRole.SOLUTION);

        Set<FileRole> roles = Set.of(FileRole.SOLUTION);
        FileDetailsDTO solutionDto = new FileDetailsDTO(3L, "solution.zip", "application/zip", 4096L, "SOLUTION",
            "/api/v1/files/download/3");

        when(fileRepository.findAll(any(Specification.class))).thenReturn(List.of(solutionFile));
        when(fileMapper.toDetails(solutionFile, "/api/v1/files/download/3")).thenReturn(solutionDto);

        List<FileDetailsDTO> result = fileService.getFilesByEntity(RelatedEntityType.TASK, 10L, roles);

        assertEquals(1, result.size());
        assertEquals("SOLUTION", result.getFirst().fileRole());
        verify(fileRepository).findAll(any(Specification.class));
    }

    // --- uploadToFileDetails Tests ---

    @Test
    void uploadToFileDetails_ShouldReturnEmptyList_WhenNoFilesProvided() {
        FileUploadRequestDto request =
            uploadRequest(RelatedEntityType.NEWS, 1L);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(validationStrategyResolver.resolve(any(), any()))
            .thenReturn(validationStrategy);
        when(filePolicyResolver.resolve(any(), any()))
            .thenReturn(filePolicy);
        when(validationStrategy.validate(any(), any(), any()))
            .thenReturn(ValidationResult.ok());

        List<FileDetailsDTO> result =
            fileService.uploadToFileDetails(List.of(), request);

        assertTrue(result.isEmpty());

        verify(validationStrategyResolver)
            .resolve(RelatedEntityType.NEWS, FileRole.GENERIC);
        verify(filePolicyResolver)
            .resolve(RelatedEntityType.NEWS, FileRole.GENERIC);

        verifyNoInteractions(providerResolver, fileRepository, fileMapper);
    }

    @Test
    void uploadToFileDetails_ShouldThrowAuthorizationException_WhenUserNotAuthenticated() {
        MultipartFile file = new MockMultipartFile(
            "file",
            "test.jpg",
            "image/jpeg",
            new byte[10]);

        List<MultipartFile> files = List.of(file);

        FileUploadRequestDto request =
            uploadRequest(RelatedEntityType.NEWS, 1L);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.empty());

        assertThrows(
            AuthorizationException.class,
            () -> fileService.uploadToFileDetails(files, request));

        verifyNoInteractions(
            validationStrategyResolver,
            filePolicyResolver,
            providerResolver,
            fileRepository,
            fileMapper);
    }

    @Test
    void uploadToFileDetails_ShouldThrowValidationException_WhenValidationFails() {
        MultipartFile file = new MockMultipartFile(
            "file",
            "photo.jpg",
            "image/jpeg",
            new byte[512]);

        List<MultipartFile> files = List.of(file);

        FileUploadRequestDto request =
            uploadRequest(RelatedEntityType.NEWS, 1L);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(validationStrategyResolver.resolve(any(), any()))
            .thenReturn(validationStrategy);
        when(filePolicyResolver.resolve(any(), any()))
            .thenReturn(filePolicy);
        when(validationStrategy.validate(any(), any(), any()))
            .thenReturn(ValidationResult.fail("File size exceeded"));

        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> fileService.uploadToFileDetails(files, request));

        assertTrue(exception.getMessage().contains("File size exceeded"));

        verifyNoInteractions(providerResolver);
        verify(fileRepository, never()).save(any());
        verify(fileMapper, never()).toDetails(any(), any());
    }

    @Test
    void uploadToFileDetails_ShouldReturnMappedDtos_WhenFilesAreValid() {
        MultipartFile file = new MockMultipartFile(
            "file",
            "photo.jpg",
            "image/jpeg",
            new byte[512]);

        FileUploadRequestDto request =
            uploadRequest(RelatedEntityType.NEWS, 1L);

        FileAsset savedAsset = new FileAsset();
        savedAsset.setId(10L);
        savedAsset.setStorageKey("news/stored.jpg");

        ArgumentCaptor<FileAsset> captor =
            ArgumentCaptor.forClass(FileAsset.class);

        FileDetailsDTO expectedDto = new FileDetailsDTO(
            10L,
            "photo.jpg",
            "image/jpeg",
            512L,
            FileRole.GENERIC.name(),
            "/api/v1/files/download/10");

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(validationStrategyResolver.resolve(any(), any()))
            .thenReturn(validationStrategy);
        when(filePolicyResolver.resolve(any(), any()))
            .thenReturn(filePolicy);
        when(validationStrategy.validate(any(), any(), any()))
            .thenReturn(ValidationResult.ok());

        when(providerResolver.resolveDefault()).thenReturn(storageProvider);
        when(storageProvider.upload(any(), anyString(), anyString()))
            .thenReturn("news/stored.jpg");
        when(storageProvider.getType())
            .thenReturn(StorageProviderType.LOCAL);
        when(fileRepository.save(any())).thenReturn(savedAsset);

        when(fileMapper.toDetails(
            savedAsset,
            "/api/v1/files/download/10")).thenReturn(expectedDto);

        List<FileDetailsDTO> result =
            fileService.uploadToFileDetails(List.of(file), request);

        assertEquals(List.of(expectedDto), result);

        verify(fileRepository).save(captor.capture());

        FileAsset saved = captor.getValue();

        assertAll(
            () -> assertEquals("photo.jpg", saved.getOriginalFilename()),
            () -> assertEquals("image/jpeg", saved.getMimeType()),
            () -> assertEquals(512L, saved.getSize()),
            () -> assertEquals(RelatedEntityType.NEWS, saved.getRelatedEntityType()),
            () -> assertEquals(1L, saved.getRelatedEntityId()),
            () -> assertEquals(userId, saved.getUserId()),
            () -> assertEquals(FileStatus.ATTACHED, saved.getStatus()));

        verify(providerResolver).resolveDefault();
        verify(fileMapper).toDetails(
            savedAsset,
            "/api/v1/files/download/10");

        assertEquals(List.of(expectedDto), result);

    }

    @Test
    void uploadToFileDetails_ShouldProcessEachFileIndependently_WhenMultipleFilesProvided() {
        MultipartFile file1 = new MockMultipartFile(
            "file",
            "a.jpg",
            "image/jpeg",
            new byte[256]);

        MultipartFile file2 = new MockMultipartFile(
            "file",
            "b.jpg",
            "image/jpeg",
            new byte[512]);

        FileUploadRequestDto request =
            uploadRequest(RelatedEntityType.NEWS, 1L);

        FileAsset asset1 = new FileAsset();
        asset1.setId(1L);
        asset1.setStorageKey("news/a.jpg");

        FileAsset asset2 = new FileAsset();
        asset2.setId(2L);
        asset2.setStorageKey("news/b.jpg");

        FileDetailsDTO dto1 = new FileDetailsDTO(
            1L,
            "a.jpg",
            "image/jpeg",
            256L,
            FileRole.GENERIC.name(),
            "/api/v1/files/download/1");

        FileDetailsDTO dto2 = new FileDetailsDTO(
            2L,
            "b.jpg",
            "image/jpeg",
            512L,
            FileRole.GENERIC.name(),
            "/api/v1/files/download/2");

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(validationStrategyResolver.resolve(any(), any()))
            .thenReturn(validationStrategy);
        when(filePolicyResolver.resolve(any(), any()))
            .thenReturn(filePolicy);
        when(validationStrategy.validate(any(), any(), any()))
            .thenReturn(ValidationResult.ok());

        when(providerResolver.resolveDefault()).thenReturn(storageProvider);

        when(storageProvider.upload(any(), anyString(), anyString()))
            .thenReturn("news/a.jpg")
            .thenReturn("news/b.jpg");

        when(storageProvider.getType())
            .thenReturn(StorageProviderType.LOCAL);

        when(fileRepository.save(any()))
            .thenReturn(asset1)
            .thenReturn(asset2);

        when(fileMapper.toDetails(
            asset1,
            "/api/v1/files/download/1")).thenReturn(dto1);

        when(fileMapper.toDetails(
            asset2,
            "/api/v1/files/download/2")).thenReturn(dto2);

        List<FileDetailsDTO> result =
            fileService.uploadToFileDetails(
                List.of(file1, file2),
                request);

        assertEquals(List.of(dto1, dto2), result);

        verify(providerResolver, times(2)).resolveDefault();
        verify(fileRepository, times(2)).save(any());
        verify(fileMapper, times(2)).toDetails(any(), anyString());
    }

    @Test
    void uploadToFileDetails_ShouldPassGeneratedFileUrlToMapper() {
        MultipartFile file = new MockMultipartFile(
            "file",
            "photo.jpg",
            "image/jpeg",
            new byte[100]);

        FileUploadRequestDto request =
            uploadRequest(RelatedEntityType.NEWS, 1L);

        FileAsset savedAsset = new FileAsset();
        savedAsset.setId(10L);
        savedAsset.setStorageKey("news/photo.jpg");

        FileDetailsDTO expectedDto = new FileDetailsDTO(
            10L,
            "photo.jpg",
            "image/jpeg",
            512L,
            FileRole.GENERIC.name(),
            "/api/v1/files/download/10");

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(validationStrategyResolver.resolve(any(), any()))
            .thenReturn(validationStrategy);
        when(filePolicyResolver.resolve(any(), any()))
            .thenReturn(filePolicy);
        when(validationStrategy.validate(any(), any(), any()))
            .thenReturn(ValidationResult.ok());

        when(providerResolver.resolveDefault()).thenReturn(storageProvider);
        when(storageProvider.upload(any(), anyString(), anyString()))
            .thenReturn("news/photo.jpg");
        when(storageProvider.getType())
            .thenReturn(StorageProviderType.LOCAL);

        when(fileRepository.save(any()))
            .thenReturn(savedAsset);

        when(fileMapper.toDetails(
            savedAsset,
            "/api/v1/files/download/10")).thenReturn(expectedDto);

        FileDetailsDTO result =
            fileService.uploadToFileDetails(List.of(file), request)
                .getFirst();

        assertSame(expectedDto, result);

        verify(fileMapper)
            .toDetails(
                savedAsset,
                "/api/v1/files/download/10");
    }

    // --- detachFiles Tests ---

    @Test
    void detachFiles_ShouldDoNothing_WhenFileIdsAreNull() {
        fileService.detachFiles(
            RelatedEntityType.NEWS,
            1L,
            null,
            userId);

        verifyNoInteractions(fileRepository, securityFacade);
    }

    @Test
    void detachFiles_ShouldDoNothing_WhenFileIdsAreEmpty() {
        fileService.detachFiles(
            RelatedEntityType.NEWS,
            1L,
            List.of(),
            userId);

        verifyNoInteractions(fileRepository, securityFacade);
    }

    @Test
    void detachFiles_ShouldSoftDeleteFiles_WhenUserIsOwner() {
        FileAsset file1 = createFile(
            1L,
            userId,
            RelatedEntityType.NEWS,
            10L);
        FileAsset file2 = createFile(
            2L,
            userId,
            RelatedEntityType.NEWS,
            10L);

        List<Long> fileIds = List.of(1L, 2L);

        when(fileRepository.findAllById(fileIds))
            .thenReturn(List.of(file1, file2));

        when(securityFacade.hasRole(ROLE_ADMIN))
            .thenReturn(false);

        fileService.detachFiles(
            RelatedEntityType.NEWS,
            10L,
            fileIds,
            userId);

        assertAll(
            () -> assertEquals(FileStatus.SOFT_DELETED, file1.getStatus()),
            () -> assertEquals(FileStatus.SOFT_DELETED, file2.getStatus()),
            () -> assertNotNull(file1.getDeletedAt()),
            () -> assertNotNull(file2.getDeletedAt()));

        verify(fileRepository).findAllById(fileIds);
        verify(fileRepository).saveAll(List.of(file1, file2));
    }

    @Test
    void detachFiles_ShouldAllowAdminToDetachAnotherUsersFiles() {
        FileAsset file = createFile(
            fileId,
            999L,
            RelatedEntityType.NEWS,
            10L);

        when(fileRepository.findAllById(List.of(fileId)))
            .thenReturn(List.of(file));

        when(securityFacade.hasRole(ROLE_ADMIN))
            .thenReturn(true);

        fileService.detachFiles(
            RelatedEntityType.NEWS,
            10L,
            List.of(fileId),
            userId);

        assertEquals(FileStatus.SOFT_DELETED, file.getStatus());
        assertNotNull(file.getDeletedAt());

        verify(fileRepository).saveAll(List.of(file));
    }

    @Test
    void detachFiles_ShouldThrowValidationException_WhenFileBelongsToAnotherEntity() {
        FileAsset file = createFile(
            fileId,
            userId,
            RelatedEntityType.TASK,
            999L);

        List<Long> fileIds = List.of(fileId);

        when(fileRepository.findAllById(fileIds))
            .thenReturn(List.of(file));

        when(securityFacade.hasRole(ROLE_ADMIN))
            .thenReturn(false);

        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> fileService.detachFiles(
                RelatedEntityType.NEWS,
                10L,
                fileIds,
                userId));

        assertTrue(exception.getMessage().contains("does not belong"));
        assertEquals(FileStatus.ATTACHED, file.getStatus());
        assertNull(file.getDeletedAt());

        verify(fileRepository, never()).saveAll(any());
    }

    @Test
    void detachFiles_ShouldThrowAuthorizationException_WhenUserIsNotOwnerAndNotAdmin() {
        FileAsset file = createFile(
            fileId,
            999L,
            RelatedEntityType.NEWS,
            10L);

        List<Long> fileIds = List.of(fileId);

        when(fileRepository.findAllById(fileIds))
            .thenReturn(List.of(file));

        when(securityFacade.hasRole(ROLE_ADMIN))
            .thenReturn(false);

        assertThrows(
            AuthorizationException.class,
            () -> fileService.detachFiles(
                RelatedEntityType.NEWS,
                10L,
                fileIds,
                userId));

        assertEquals(FileStatus.ATTACHED, file.getStatus());
        assertNull(file.getDeletedAt());

        verify(fileRepository, never()).saveAll(any());
    }

    // --- detachAllFilesByEntityId Tests ---

    @Test
    void detachAllFilesByEntityId_ShouldDetachAllAttachedFiles() {
        FileAsset file1 = createFile(
            1L,
            userId,
            RelatedEntityType.NEWS,
            10L);
        FileAsset file2 = createFile(
            2L,
            userId,
            RelatedEntityType.NEWS,
            10L);

        List<FileAsset> files = List.of(file1, file2);

        when(fileRepository
            .findByRelatedEntityTypeAndRelatedEntityIdAndStatus(
                RelatedEntityType.NEWS,
                10L,
                FileStatus.ATTACHED))
            .thenReturn(files);

        when(securityFacade.hasRole(ROLE_ADMIN))
            .thenReturn(false);

        fileService.detachAllFilesByEntityId(
            RelatedEntityType.NEWS,
            10L,
            userId);

        assertAll(
            () -> assertEquals(FileStatus.SOFT_DELETED, file1.getStatus()),
            () -> assertEquals(FileStatus.SOFT_DELETED, file2.getStatus()),
            () -> assertNotNull(file1.getDeletedAt()),
            () -> assertNotNull(file2.getDeletedAt()));

        verify(fileRepository)
            .findByRelatedEntityTypeAndRelatedEntityIdAndStatus(
                RelatedEntityType.NEWS,
                10L,
                FileStatus.ATTACHED);

        verify(fileRepository).saveAll(files);
    }

    @Test
    void detachAllFilesByEntityId_ShouldAllowAdminToDetachAnotherUsersFiles() {
        FileAsset file = createFile(
            fileId,
            999L,
            RelatedEntityType.NEWS,
            10L);

        when(fileRepository
            .findByRelatedEntityTypeAndRelatedEntityIdAndStatus(
                RelatedEntityType.NEWS,
                10L,
                FileStatus.ATTACHED))
            .thenReturn(List.of(file));

        when(securityFacade.hasRole(ROLE_ADMIN))
            .thenReturn(true);

        fileService.detachAllFilesByEntityId(
            RelatedEntityType.NEWS,
            10L,
            userId);

        assertEquals(FileStatus.SOFT_DELETED, file.getStatus());
        assertNotNull(file.getDeletedAt());

        verify(fileRepository).saveAll(List.of(file));
    }

    @Test
    void detachAllFilesByEntityId_ShouldDoNothing_WhenNoAttachedFilesFound() {
        when(fileRepository
            .findByRelatedEntityTypeAndRelatedEntityIdAndStatus(
                RelatedEntityType.NEWS,
                10L,
                FileStatus.ATTACHED))
            .thenReturn(List.of());

        when(securityFacade.hasRole(ROLE_ADMIN))
            .thenReturn(false);

        fileService.detachAllFilesByEntityId(
            RelatedEntityType.NEWS,
            10L,
            userId);

        verify(fileRepository)
            .findByRelatedEntityTypeAndRelatedEntityIdAndStatus(
                RelatedEntityType.NEWS,
                10L,
                FileStatus.ATTACHED);

        verify(fileRepository).saveAll(List.of());
    }

    @Test
    void detachAllFilesByEntityId_ShouldThrowAuthorizationException_WhenUserIsNotOwner() {
        FileAsset file = createFile(
            fileId,
            999L,
            RelatedEntityType.NEWS,
            10L);

        when(fileRepository
            .findByRelatedEntityTypeAndRelatedEntityIdAndStatus(
                RelatedEntityType.NEWS,
                10L,
                FileStatus.ATTACHED))
            .thenReturn(List.of(file));

        when(securityFacade.hasRole(ROLE_ADMIN))
            .thenReturn(false);

        assertThrows(
            AuthorizationException.class,
            () -> fileService.detachAllFilesByEntityId(
                RelatedEntityType.NEWS,
                10L,
                userId));

        assertEquals(FileStatus.ATTACHED, file.getStatus());
        assertNull(file.getDeletedAt());

        verify(fileRepository, never()).saveAll(any());
    }

    // --- Update Role Tests ---

    @Test
    void updateRole_ShouldUpdateRoleAndReturnDto_WhenValidRequest() {
        UpdateFileRoleRequestDto requestDto = new UpdateFileRoleRequestDto(FileRole.PROBLEM);

        FileAsset file = new FileAsset();
        file.setId(fileId);
        file.setUserId(userId);
        file.setStatus(FileStatus.ATTACHED);
        file.setRelatedEntityType(RelatedEntityType.TASK);
        file.setFileRole(FileRole.REFERENCE);
        file.setStorageProvider(StorageProviderType.LOCAL);
        file.setStorageKey("task/problem.docx");
        file.setOriginalFilename("problem.docx");

        FileResponseDto expectedDto = FileResponseDto.builder().id(fileId).build();

        FilePolicy newRolePolicy = mock(FilePolicy.class);
        when(newRolePolicy.getAllowedExtensions()).thenReturn(Set.of(AllowedExtension.DOCX));

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(securityFacade.hasRole("ADMIN")).thenReturn(false);
        when(filePolicyResolver.resolve(RelatedEntityType.TASK, FileRole.PROBLEM)).thenReturn(newRolePolicy);
        when(fileRepository.save(any(FileAsset.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fileMapper.toDto(any(FileAsset.class))).thenReturn(expectedDto);
        when(providerResolver.resolve(StorageProviderType.LOCAL)).thenReturn(storageProvider);

        FileResponseDto result = fileService.updateRole(fileId, requestDto);

        assertNotNull(result);
        assertEquals(expectedDto, result);
        assertEquals(FileRole.PROBLEM, file.getFileRole());

        verify(fileRepository).save(file);
        verify(filePolicyResolver).resolve(RelatedEntityType.TASK, FileRole.PROBLEM);
        verify(newRolePolicy).getAllowedExtensions();
    }

    @Test
    void updateRole_ShouldThrowValidationException_WhenFileNotAttached() {
        UpdateFileRoleRequestDto requestDto = new UpdateFileRoleRequestDto(FileRole.PROBLEM);

        FileAsset file = new FileAsset();
        file.setId(fileId);
        file.setUserId(userId);
        file.setStatus(FileStatus.TEMPORARY);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(securityFacade.hasRole("ADMIN")).thenReturn(false);

        ValidationException exception =
            assertThrows(ValidationException.class, () -> fileService.updateRole(fileId, requestDto));
        assertTrue(exception.getMessage().contains("ATTACHED state"));

        verifyNoInteractions(filePolicyResolver, fileMapper);
        verify(fileRepository, never()).save(any());
    }

    @Test
    void updateRole_ShouldThrowAuthorizationException_WhenUserNotOwnerOrAdmin() {
        Long ownerId = 99L;
        UpdateFileRoleRequestDto requestDto = new UpdateFileRoleRequestDto(FileRole.PROBLEM);

        FileAsset file = new FileAsset();
        file.setId(fileId);
        file.setUserId(ownerId);

        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(securityFacade.hasRole("ADMIN")).thenReturn(false);

        assertThrows(AuthorizationException.class, () -> fileService.updateRole(fileId, requestDto));

        verifyNoInteractions(filePolicyResolver, fileMapper);
        verify(fileRepository, never()).save(any());
    }

    // --- downloadFile(Long id) Tests ---

    @Test
    void downloadFile_ShouldReturnDto_WhenAttachedFileAndAccessGranted() {
        existingFile.setStatus(FileStatus.ATTACHED);
        existingFile.setRelatedEntityType(RelatedEntityType.NEWS);
        existingFile.setRelatedEntityId(100L);
        existingFile.setStorageKey("news/100/article.pdf");
        existingFile.setMimeType("application/pdf");
        existingFile.setOriginalFilename("article.pdf");
        existingFile.setSize(1024L);

        Resource mockResource = new ByteArrayResource("content".getBytes());

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(existingFile));
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(accessValidatorResolver.resolve(RelatedEntityType.NEWS)).thenReturn(accessValidator);
        when(accessValidator.canAccess(eq(100L), eq(userId), any())).thenReturn(true);
        when(providerResolver.resolve(StorageProviderType.LOCAL)).thenReturn(storageProvider);
        when(storageProvider.getResource("news/100/article.pdf")).thenReturn(mockResource);

        FileDownloadDto result = fileService.downloadFile(fileId);

        assertNotNull(result);
        assertEquals(mockResource, result.resource());
        assertEquals("application/pdf", result.mimeType());
        assertEquals("article.pdf", result.originalFilename());
        assertEquals(1024L, result.contentLength());
    }

    @Test
    void downloadFile_ShouldThrowNotFound_WhenFileDoesNotExist() {
        when(fileRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(FileAssetNotFoundException.class, () -> fileService.downloadFile(nonExistentId));
    }

    @Test
    void downloadFile_ShouldThrowAuthorizationException_WhenAccessDenied() {
        existingFile.setStatus(FileStatus.ATTACHED);
        existingFile.setRelatedEntityType(RelatedEntityType.NEWS);
        existingFile.setRelatedEntityId(100L);

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(existingFile));
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(accessValidatorResolver.resolve(RelatedEntityType.NEWS)).thenReturn(accessValidator);
        when(accessValidator.canAccess(eq(100L), eq(userId), any())).thenReturn(false);

        assertThrows(AuthorizationException.class, () -> fileService.downloadFile(fileId));
    }

    @Test
    void downloadFile_ShouldSucceedForTemporaryFile_WhenUserIsOwner() {
        existingFile.setStatus(FileStatus.TEMPORARY);
        existingFile.setUserId(userId);
        existingFile.setStorageKey("temp/preview.png");
        existingFile.setMimeType("image/png");
        existingFile.setOriginalFilename("preview.png");
        existingFile.setSize(512L);

        Resource mockResource = new ByteArrayResource("image".getBytes());

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(existingFile));
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
        when(providerResolver.resolve(StorageProviderType.LOCAL)).thenReturn(storageProvider);
        when(storageProvider.getResource("temp/preview.png")).thenReturn(mockResource);

        FileDownloadDto result = fileService.downloadFile(fileId);

        assertNotNull(result);
        assertEquals(mockResource, result.resource());
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

    private FileAsset createFile(
        Long id,
        Long ownerId,
        RelatedEntityType entityType,
        Long entityId) {
        FileAsset file = new FileAsset();
        file.setId(id);
        file.setUserId(ownerId);
        file.setRelatedEntityType(entityType);
        file.setRelatedEntityId(entityId);
        file.setStatus(FileStatus.ATTACHED);
        return file;
    }
}