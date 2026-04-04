package com.itasocialacademy.oitassist.filemanager.service;

import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import com.itasocialacademy.oitassist.filemanager.dao.model.FileAsset;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileStatus;
import com.itasocialacademy.oitassist.filemanager.dao.repository.FileRepository;
import com.itasocialacademy.oitassist.filemanager.exceptions.FileAssetNotFoundException;
import com.itasocialacademy.oitassist.filemanager.exceptions.UnsupportedStorageException;
import com.itasocialacademy.oitassist.filemanager.providers.LocalStorageProvider;
import com.itasocialacademy.oitassist.filemanager.providers.interfaces.StorageProvider;
import com.itasocialacademy.oitassist.filemanager.providers.resolver.StorageProviderResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private FileServiceImpl fileService;

    @Mock
    private LocalStorageProvider localStorageProvider;

    private Long fileId;
    private Long nonExistentId;
    private FileAsset existingFile;

    @BeforeEach
    void setUp() {
        fileId = 1L;
        existingFile = new FileAsset();
        existingFile.setId(fileId);
        existingFile.setStatus(FileStatus.ATTACHED);
        existingFile.setStorageProvider(StorageProviderType.LOCAL);
        // authorities should be tested once we add security service

        nonExistentId = 999L;
    }

    // --- Upload Tests ---

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

        verify(localStorageProvider, never()).deletePhysical(any());
        verify(fileRepository, never()).save(any());
    }
}