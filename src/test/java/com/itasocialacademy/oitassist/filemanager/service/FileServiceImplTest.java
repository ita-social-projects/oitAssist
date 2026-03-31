package com.itasocialacademy.oitassist.filemanager.service;

import com.itasocialacademy.oitassist.filemanager.dao.enums.StorageProviderType;
import com.itasocialacademy.oitassist.filemanager.dao.model.FileAsset;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileStatus;
import com.itasocialacademy.oitassist.filemanager.dao.repository.FileRepository;
import com.itasocialacademy.oitassist.filemanager.exceptions.UnsupportedStorageException;
import com.itasocialacademy.oitassist.filemanager.providers.LocalStorageProvider;
import java.io.FileNotFoundException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private FileRepository fileRepository;

    @InjectMocks
    private FileServiceImpl fileService;

    @Mock
    private LocalStorageProvider localStorageProvider;

    private Long fileId;
    private Long nonExistentId;
    private FileAsset existingFile;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileService, "providers", List.of(localStorageProvider));

        fileId = 1L;
        existingFile = new FileAsset();
        existingFile.setId(fileId);
        existingFile.setStatus(FileStatus.ATTACHED);
        existingFile.setStorageProvider(StorageProviderType.LOCAL);
        // todo: authorities should be tested once we add security service

        nonExistentId = 999L;
    }

    // --- Upload Tests ---

    // --- Soft Delete Tests ---

    @Test
    void deleteSoft_ShouldUpdateStatusAndTimestamp_WhenFileExists() throws FileNotFoundException {
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(existingFile));

        ArgumentCaptor<FileAsset> fileCaptor = ArgumentCaptor.forClass(FileAsset.class);

        fileService.deleteSoft(fileId);

        verify(fileRepository).save(fileCaptor.capture());
        FileAsset savedFile = fileCaptor.getValue();

        assertEquals(FileStatus.SOFT_DELETED, savedFile.getStatus());
        assertNotNull(savedFile.getDeletedAt(), "DeletedAt timestamp should be populated");
        assertEquals(fileId, savedFile.getId());
    }

    @Test
    void deleteSoft_ShouldThrowException_WhenFileNotFound() {
        when(fileRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class, () -> fileService.deleteSoft(nonExistentId));

        verify(fileRepository, never()).save(any());
    }

    // --- Hard Delete Tests ---

    @Test
    void deleteHard_ShouldTriggerPhysicalDeletionAndClearPath() throws FileNotFoundException {
        String testPath = "/tmp/storage/test.txt";
        existingFile.setStorageKey(testPath);

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(existingFile));
        when(localStorageProvider.supports(StorageProviderType.LOCAL)).thenReturn(true);

        fileService.deleteHard(fileId);
        verify(localStorageProvider).deletePhysical(testPath);

        ArgumentCaptor<FileAsset> captor = ArgumentCaptor.forClass(FileAsset.class);
        verify(fileRepository).save(captor.capture());

        FileAsset result = captor.getValue();
        assertEquals(FileStatus.HARD_DELETED, result.getStatus());
        // todo: change the expected storage_key below, when it is changed in the
        // service!
        assertEquals("DELETED " + existingFile.getId(), result.getStorageKey());

        verify(localStorageProvider, times(1)).deletePhysical(testPath);
    }

    @Test
    void deleteHard_ShouldThrowFileNotFoundException_WhenFileMissingInDb() {
        when(fileRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(FileNotFoundException.class, () -> fileService.deleteHard(nonExistentId));

        verify(localStorageProvider, never()).deletePhysical(any());
        verify(fileRepository, never()).save(any());
    }

    @Test
    void deleteHard_ShouldThrowUnsupportedStorageException_WhenNoProviderMatches() {
        existingFile.setStorageProvider(null);
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(existingFile));
        when(localStorageProvider.supports(any())).thenReturn(false);

        assertThrows(UnsupportedStorageException.class, () -> fileService.deleteHard(fileId));

        verify(localStorageProvider, never()).deletePhysical(any());
        verify(fileRepository, never()).save(any());
    }

    @Test
    void deleteHard_ShouldSucceed_EvenIfPhysicalFileIsAlreadyMissing() {
        String path = "/uploads/test.txt";
        existingFile.setStorageKey(path);

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(existingFile));
        when(localStorageProvider.supports(any())).thenReturn(true);

        assertDoesNotThrow(() -> fileService.deleteHard(fileId));

        verify(localStorageProvider).deletePhysical(path);
        verify(fileRepository).save(any());
    }
}