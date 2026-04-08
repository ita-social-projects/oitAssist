package com.itasocialacademy.oitassist.filemanager.service;

import com.itasocialacademy.oitassist.core.exceptions.ValidationException;
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
import com.itasocialacademy.oitassist.filemanager.validation.FileValidationStrategyResolver;
import com.itasocialacademy.oitassist.filemanager.validation.interfaces.FileValidationStrategy;
import com.itasocialacademy.oitassist.filemanager.validation.model.ValidationResult;
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

    @InjectMocks
    private FileServiceImpl fileService;

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
        // authorities should be tested once we add security service

        nonExistentId = 999L;
    }

    // --- Upload Tests ---

    @Test
    void upload_ShouldReturnMappedDtos_WhenFilesAreValid() {
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[512]);
        FileUploadRequestDto request = uploadRequest(RelatedEntityType.NEWS, 1L);
        FileAsset savedAsset = new FileAsset();
        FileResponseDto expectedDto = FileResponseDto.builder().id(10L).build();

        when(validationStrategyResolver.resolve(RelatedEntityType.NEWS)).thenReturn(validationStrategy);
        when(validationStrategy.validate(any(), any())).thenReturn(ValidationResult.ok());
        when(providerResolver.resolveDefault()).thenReturn(storageProvider);
        when(storageProvider.upload(any(), anyString(), anyString())).thenReturn("news/stored.jpg");
        when(storageProvider.getType()).thenReturn(StorageProviderType.LOCAL);
        when(fileRepository.save(any())).thenReturn(savedAsset);
        when(fileMapper.toDto(savedAsset)).thenReturn(expectedDto);

        List<FileResponseDto> result = fileService.upload(List.of(file), request, userId);

        assertEquals(List.of(expectedDto), result);
        verify(validationStrategyResolver).resolve(RelatedEntityType.NEWS);
        verify(providerResolver).resolveDefault();
        verify(fileRepository).save(any());
        verify(fileMapper).toDto(savedAsset);
    }

    @Test
    void upload_ShouldSaveFileAssetWithCorrectMetadata_WhenUploading() {
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[512]);
        FileUploadRequestDto request = uploadRequest(RelatedEntityType.NEWS, 5L);
        ArgumentCaptor<FileAsset> captor = ArgumentCaptor.forClass(FileAsset.class);

        when(validationStrategyResolver.resolve(any())).thenReturn(validationStrategy);
        when(validationStrategy.validate(any(), any())).thenReturn(ValidationResult.ok());
        when(providerResolver.resolveDefault()).thenReturn(storageProvider);
        when(storageProvider.upload(any(), anyString(), anyString())).thenReturn("news/stored.jpg");
        when(storageProvider.getType()).thenReturn(StorageProviderType.LOCAL);
        when(fileRepository.save(any())).thenReturn(new FileAsset());
        when(fileMapper.toDto(any())).thenReturn(new FileResponseDto());

        fileService.upload(List.of(file), request, userId);

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

        when(validationStrategyResolver.resolve(any())).thenReturn(validationStrategy);
        when(validationStrategy.validate(any(), any())).thenReturn(ValidationResult.ok());
        when(providerResolver.resolveDefault()).thenReturn(storageProvider);
        when(storageProvider.upload(any(), anyString(), anyString())).thenReturn("task/stored.jpg");
        when(storageProvider.getType()).thenReturn(StorageProviderType.LOCAL);
        when(fileRepository.save(any())).thenReturn(new FileAsset());
        when(fileMapper.toDto(any())).thenReturn(new FileResponseDto());

        fileService.upload(List.of(file), request, userId);

        verify(storageProvider).upload(any(), anyString(), eq("task"));
    }

    @Test
    void upload_ShouldSetStatusAttached_WhenRelatedEntityIdIsProvided() {
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[512]);
        FileUploadRequestDto request = uploadRequest(RelatedEntityType.NEWS, 1L);
        ArgumentCaptor<FileAsset> captor = ArgumentCaptor.forClass(FileAsset.class);

        when(validationStrategyResolver.resolve(any())).thenReturn(validationStrategy);
        when(validationStrategy.validate(any(), any())).thenReturn(ValidationResult.ok());
        when(providerResolver.resolveDefault()).thenReturn(storageProvider);
        when(storageProvider.upload(any(), anyString(), anyString())).thenReturn("news/stored.jpg");
        when(storageProvider.getType()).thenReturn(StorageProviderType.LOCAL);
        when(fileRepository.save(any())).thenReturn(new FileAsset());
        when(fileMapper.toDto(any())).thenReturn(new FileResponseDto());

        fileService.upload(List.of(file), request, userId);

        verify(fileRepository).save(captor.capture());
        assertEquals(FileStatus.ATTACHED, captor.getValue().getStatus());
    }

    @Test
    void upload_ShouldSetStatusTemporary_WhenRelatedEntityIdIsNull() {
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[512]);
        FileUploadRequestDto request = uploadRequest(RelatedEntityType.NEWS, null);
        ArgumentCaptor<FileAsset> captor = ArgumentCaptor.forClass(FileAsset.class);

        when(validationStrategyResolver.resolve(any())).thenReturn(validationStrategy);
        when(validationStrategy.validate(any(), any())).thenReturn(ValidationResult.ok());
        when(providerResolver.resolveDefault()).thenReturn(storageProvider);
        when(storageProvider.upload(any(), anyString(), anyString())).thenReturn("news/stored.jpg");
        when(storageProvider.getType()).thenReturn(StorageProviderType.LOCAL);
        when(fileRepository.save(any())).thenReturn(new FileAsset());
        when(fileMapper.toDto(any())).thenReturn(new FileResponseDto());

        fileService.upload(List.of(file), request, userId);

        verify(fileRepository).save(captor.capture());
        assertEquals(FileStatus.TEMPORARY, captor.getValue().getStatus());
    }

    @Test
    void upload_ShouldThrowValidationException_WhenValidationFails() {
        MultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[512]);
        FileUploadRequestDto request = uploadRequest(RelatedEntityType.NEWS, 1L);

        when(validationStrategyResolver.resolve(any())).thenReturn(validationStrategy);
        when(validationStrategy.validate(any(), any())).thenReturn(
            ValidationResult.fail("File size exceeded"));

        var files = List.of(file);
        ValidationException exception = assertThrows(
            ValidationException.class, () -> fileService.upload(files, request, userId));

        assertTrue(exception.getMessage().contains("File size exceeded"));
        verifyNoInteractions(providerResolver);
        verify(fileRepository, never()).save(any());
    }

    @Test
    void upload_ShouldThrowFileUploadException_WhenInputStreamThrowsIOException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        FileUploadRequestDto request = uploadRequest(RelatedEntityType.NEWS, 1L);

        when(file.getOriginalFilename()).thenReturn("photo.jpg");
        when(file.getInputStream()).thenThrow(new IOException("disk error"));
        when(validationStrategyResolver.resolve(any())).thenReturn(validationStrategy);
        when(validationStrategy.validate(any(), any())).thenReturn(ValidationResult.ok());
        when(providerResolver.resolveDefault()).thenReturn(storageProvider);

        var files = List.of(file);
        FileUploadException exception = assertThrows(
            FileUploadException.class, () -> fileService.upload(files, request, userId));

        assertInstanceOf(IOException.class, exception.getCause());
        verify(fileRepository, never()).save(any());
    }

    @Test
    void upload_ShouldProcessEachFileIndependently_WhenMultipleFilesProvided() {
        MultipartFile file1 = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[256]);
        MultipartFile file2 = new MockMultipartFile("file", "b.jpg", "image/jpeg", new byte[512]);
        FileUploadRequestDto request = uploadRequest(RelatedEntityType.NEWS, 1L);

        when(validationStrategyResolver.resolve(any())).thenReturn(validationStrategy);
        when(validationStrategy.validate(any(), any())).thenReturn(ValidationResult.ok());
        when(providerResolver.resolveDefault()).thenReturn(storageProvider);
        when(storageProvider.upload(any(), anyString(), anyString()))
            .thenReturn("news/a.jpg")
            .thenReturn("news/b.jpg");
        when(storageProvider.getType()).thenReturn(StorageProviderType.LOCAL);
        when(fileRepository.save(any())).thenReturn(new FileAsset());
        when(fileMapper.toDto(any()))
            .thenReturn(FileResponseDto.builder().id(1L).build())
            .thenReturn(FileResponseDto.builder().id(2L).build());

        List<FileResponseDto> result = fileService.upload(List.of(file1, file2), request, userId);

        assertEquals(2, result.size());
        verify(storageProvider, times(2)).upload(any(), anyString(), anyString());
        verify(fileRepository, times(2)).save(any());
        verify(fileMapper, times(2)).toDto(any());
    }

    // --- Soft Delete Tests ---

    @Test
    void deleteSoft_ShouldUpdateStatusAndTimestamp_WhenFileExists() {
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(existingFile));

        fileService.deleteSoft(fileId);

        ArgumentCaptor<FileAsset> fileCaptor = ArgumentCaptor.forClass(FileAsset.class);
        verify(fileRepository).save(fileCaptor.capture());

        FileAsset savedFile = fileCaptor.getValue();
        assertEquals(FileStatus.SOFT_DELETED, savedFile.getStatus());
        assertNotNull(savedFile.getDeletedAt());
    }

    @Test
    void deleteSoft_ShouldThrowException_WhenFileNotFound() {
        when(fileRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(FileAssetNotFoundException.class, () -> fileService.deleteSoft(nonExistentId));
        verify(fileRepository, never()).save(any());
    }

    // --- Hard Delete Tests ---

    @Test
    void deleteHard_ShouldTriggerPhysicalDeletionAndClearPath() {
        String testPath = "/tmp/storage/test.txt";
        existingFile.setStorageKey(testPath);

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(existingFile));
        when(providerResolver.resolve(StorageProviderType.LOCAL)).thenReturn(storageProvider);

        fileService.deleteHard(fileId);

        verify(storageProvider).deletePhysical(testPath);

        ArgumentCaptor<FileAsset> captor = ArgumentCaptor.forClass(FileAsset.class);
        verify(fileRepository).save(captor.capture());

        FileAsset result = captor.getValue();
        assertEquals(FileStatus.HARD_DELETED, result.getStatus());
        assertEquals("", result.getStorageKey());
    }

    @Test
    void deleteHard_ShouldThrowUnsupportedStorageException_WhenResolverFails() {
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(existingFile));
        // Simulate resolver throwing exception if provider not found
        when(providerResolver.resolve(any())).thenThrow(UnsupportedStorageException.class);

        assertThrows(UnsupportedStorageException.class, () -> fileService.deleteHard(fileId));

        verify(fileRepository, never()).save(any());
    }

    @Test
    void deleteHard_ShouldSetStatusToFailed_WhenPhysicalDeletionThrowsException() {
        existingFile.setStorageKey("path/to/fail");
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(existingFile));
        when(providerResolver.resolve(any())).thenReturn(storageProvider);

        // Mock a failure during physical deletion
        doThrow(new RuntimeException("IO Error")).when(storageProvider).deletePhysical(anyString());

        fileService.deleteHard(fileId);

        ArgumentCaptor<FileAsset> captor = ArgumentCaptor.forClass(FileAsset.class);
        verify(fileRepository).save(captor.capture());

        assertEquals(FileStatus.FAILED, captor.getValue().getStatus());
    }

    @Test
    void deleteHard_ShouldThrowFileAssetNotFoundException_WhenFileMissingInDb() {
        when(fileRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(FileAssetNotFoundException.class, () -> fileService.deleteHard(nonExistentId));

        verifyNoInteractions(providerResolver, storageProvider);
        verify(fileRepository, never()).save(any());
    }

    // --- Helpers ---

    private static FileUploadRequestDto uploadRequest(RelatedEntityType type, Long relatedEntityId) {
        return FileUploadRequestDto.builder()
            .relatedEntityType(type)
            .relatedEntityId(relatedEntityId)
            .build();
    }
}