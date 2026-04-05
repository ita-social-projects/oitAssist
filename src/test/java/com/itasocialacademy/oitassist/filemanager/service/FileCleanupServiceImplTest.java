package com.itasocialacademy.oitassist.filemanager.service;

import com.itasocialacademy.oitassist.filemanager.config.FileCleanupConfig;
import com.itasocialacademy.oitassist.filemanager.dao.enums.FileStatus;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.dao.model.FileAsset;
import com.itasocialacademy.oitassist.filemanager.dao.repository.FileRepository;
import com.itasocialacademy.oitassist.filemanager.providers.interfaces.StorageProvider;
import com.itasocialacademy.oitassist.filemanager.providers.resolver.StorageProviderResolver;
import com.itasocialacademy.oitassist.filemanager.service.interfaces.FileService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileCleanupServiceImplTest {
    @Mock
    private FileService fileService;
    @Mock
    private FileRepository repository;
    @Mock
    private StorageProviderResolver providerResolver;
    @Mock
    private FileCleanupConfig cleanupConfig;
    @Mock
    private EntityManager entityManager;
    @Mock
    private StorageProvider storageProvider;
    @Mock
    private TypedQuery<Long> query;

    @InjectMocks
    private FileCleanupServiceImpl cleanupService;

    @BeforeEach
    void setUp() {
        lenient().when(providerResolver.resolveDefault()).thenReturn(storageProvider);
        lenient().when(cleanupConfig.getOrphanHours()).thenReturn(24);
        lenient().when(cleanupConfig.getExpiredHours()).thenReturn(720);
        lenient().when(cleanupConfig.getRogueGraceHours()).thenReturn(1);

        ReflectionTestUtils.setField(cleanupService, "entityManager", entityManager);
    }

    @Test
    @DisplayName("Full Cleanup Orchestration should run all sub-tasks")
    void runFullCleanup_ShouldInvokeAllSteps() {
        when(repository.findIdsEligibleForCleanup(any(), any())).thenReturn(Collections.emptyList());
        when(storageProvider.listAllPhysicalKeys()).thenReturn(Collections.emptyList());
        when(repository.findAllAttachedFiles()).thenReturn(Collections.emptyList());

        cleanupService.runFullCleanup();

        verify(repository).findIdsEligibleForCleanup(any(), any());
        verify(storageProvider).listAllPhysicalKeys();
        verify(repository).findAllAttachedFiles();
    }

    // --- 1. PURGE TESTS ---

    @Test
    @DisplayName("Purge should log and return when no IDs found")
    void purgeExpiredAndOrphanedFiles_ShouldHandleEmptyList() {
        when(repository.findIdsEligibleForCleanup(any(), any())).thenReturn(Collections.emptyList());
        cleanupService.purgeExpiredAndOrphanedFiles();
        verify(fileService, never()).deleteHard(anyLong());
    }

    @Test
    @DisplayName("Purge should continue processing if one hard delete fails")
    void purge_ShouldBeResilientToExceptions() {
        when(repository.findIdsEligibleForCleanup(any(), any())).thenReturn(List.of(1L, 2L));
        doThrow(new RuntimeException("Failure")).when(fileService).deleteHard(1L);

        cleanupService.purgeExpiredAndOrphanedFiles();

        verify(fileService).deleteHard(1L);
        verify(fileService).deleteHard(2L);
    }

    @Test
    @DisplayName("Should purge files using the combined repository query")
    void purgeExpiredAndOrphanedFiles_ShouldUseCombinedQuery() {
        List<Long> ids = List.of(1L, 2L, 3L);
        when(repository.findIdsEligibleForCleanup(any(OffsetDateTime.class), any(OffsetDateTime.class)))
            .thenReturn(ids);

        cleanupService.purgeExpiredAndOrphanedFiles();

        verify(fileService, times(3)).deleteHard(anyLong());
        verify(fileService).deleteHard(1L);
        verify(fileService).deleteHard(2L);
        verify(fileService).deleteHard(3L);
    }

    @Test
    @DisplayName("Should continue processing subsequent files if one hard delete fails")
    void purgeExpiredAndOrphanedFiles_ShouldBeResilientToErrors() {
        List<Long> idsToPurge = List.of(1L, 2L);

        when(repository.findIdsEligibleForCleanup(any(OffsetDateTime.class), any(OffsetDateTime.class)))
            .thenReturn(idsToPurge);

        doThrow(new RuntimeException("IO Error or DB Constraint"))
            .when(fileService).deleteHard(1L);

        cleanupService.purgeExpiredAndOrphanedFiles();

        verify(fileService).deleteHard(1L);
        verify(fileService).deleteHard(2L);
        verifyNoMoreInteractions(fileService);
    }

    // --- 2. ROGUE FILE TESTS ---

    @Test
    @DisplayName("Rogue cleanup should handle metadata retrieval errors")
    void rogue_ShouldHandleMetadataErrors() {
        String key = "error.txt";
        when(storageProvider.listAllPhysicalKeys()).thenReturn(List.of(key));
        when(repository.findAllActiveStorageKeys()).thenReturn(Collections.emptyList());
        when(storageProvider.getLastModified(key)).thenThrow(new RuntimeException("IO Error"));

        cleanupService.cleanupRoguePhysicalFiles();

        verify(storageProvider, never()).deletePhysical(key);
    }

    @Test
    @DisplayName("Rogue cleanup should handle errors when getting last modified time")
    void cleanupRoguePhysicalFiles_ShouldHandleMetadataErrors() {
        String key = "error-file.txt";
        when(storageProvider.listAllPhysicalKeys()).thenReturn(List.of(key));
        when(repository.findAllActiveStorageKeys()).thenReturn(Collections.emptyList());
        when(storageProvider.getLastModified(key)).thenThrow(new RuntimeException("IO Error"));

        cleanupService.cleanupRoguePhysicalFiles();

        verify(storageProvider, never()).deletePhysical(key);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideRogueFileScenarios")
    @DisplayName("Rogue cleanup should correctly handle various file scenarios")
    void cleanupRoguePhysicalFiles_Parameterized(
        String description,
        List<String> activeKeys,
        OffsetDateTime lastModified,
        boolean expectDelete) {

        String fileName = "test-file.png";
        when(storageProvider.listAllPhysicalKeys()).thenReturn(List.of(fileName));
        when(repository.findAllActiveStorageKeys()).thenReturn(activeKeys);

        if (!activeKeys.contains(fileName)) {
            when(storageProvider.getLastModified(fileName)).thenReturn(lastModified);
        }

        cleanupService.cleanupRoguePhysicalFiles();

        if (expectDelete) {
            verify(storageProvider).deletePhysical(fileName);
        } else {
            verify(storageProvider, never()).deletePhysical(fileName);
        }
    }

    @Test
    @DisplayName("Should NOT delete rogue files newer than the safety threshold")
    void cleanupRoguePhysicalFiles_ShouldSkipNewRogueFiles() {
        String newFile = "just/uploaded.png";
        when(storageProvider.listAllPhysicalKeys()).thenReturn(List.of(newFile));
        when(repository.findAllActiveStorageKeys()).thenReturn(Collections.emptyList());

        when(storageProvider.getLastModified(newFile)).thenReturn(OffsetDateTime.now());

        cleanupService.cleanupRoguePhysicalFiles();

        verify(storageProvider, never()).deletePhysical(newFile);
    }

    // --- 3. DANGLING REFERENCE TESTS ---

    @Test
    @DisplayName("Dangling check should mark orphaned files as soft deleted")
    void dangling_ShouldProcessMissingParents() {
        FileAsset newsFile = FileAsset.builder().id(1L).status(FileStatus.ATTACHED)
            .relatedEntityType(RelatedEntityType.NEWS).relatedEntityId(100L).build();

        when(repository.findAllAttachedFiles()).thenReturn(List.of(newsFile));

        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(query);
        when(query.setParameter(eq("ids"), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList()); // Parent missing

        cleanupService.handleDanglingAttachedFiles();

        assertThat(newsFile.getStatus()).isEqualTo(FileStatus.SOFT_DELETED);
        assertThat(newsFile.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Dangling check should ignore files with null entity IDs")
    void dangling_ShouldIgnoreNullRelatedIds() {
        FileAsset nullIdFile = FileAsset.builder().id(1L).status(FileStatus.ATTACHED)
            .relatedEntityType(RelatedEntityType.NEWS).relatedEntityId(null).build();

        when(repository.findAllAttachedFiles()).thenReturn(List.of(nullIdFile));

        cleanupService.handleDanglingAttachedFiles();

        assertThat(nullIdFile.getStatus()).isEqualTo(FileStatus.SOFT_DELETED);
        verify(entityManager, never()).createQuery(anyString(), any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when entity type is not mapped in switch")
    void dangling_ShouldThrowExceptionForUnmappedType() {
        FileAsset competitionFile = FileAsset.builder()
            .id(99L)
            .status(FileStatus.ATTACHED)
            .relatedEntityType(RelatedEntityType.COMPETITION)
            .relatedEntityId(123L)
            .build();

        when(repository.findAllAttachedFiles()).thenReturn(List.of(competitionFile));

        assertThatThrownBy(() -> cleanupService.handleDanglingAttachedFiles())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown entity type: COMPETITION");
    }

    @Test
    @DisplayName("Rogue cleanup should skip new files (race condition logic)")
    void cleanupRoguePhysicalFiles_ShouldSkipNewFiles() {
        String newFile = "new.txt";
        when(storageProvider.listAllPhysicalKeys()).thenReturn(List.of(newFile));
        when(repository.findAllActiveStorageKeys()).thenReturn(Collections.emptyList());
        when(storageProvider.getLastModified(newFile)).thenReturn(OffsetDateTime.now());

        cleanupService.cleanupRoguePhysicalFiles();

        verify(storageProvider, never()).deletePhysical(newFile);
    }

    @Test
    @DisplayName("Should skip dangling check if no attached files exist")
    void handleDanglingAttachedFiles_ShouldHandleEmptyList() {
        when(repository.findAllAttachedFiles()).thenReturn(Collections.emptyList());
        cleanupService.handleDanglingAttachedFiles();
        verify(entityManager, never()).createQuery(anyString(), any());
    }

    private static Stream<Arguments> provideRogueFileScenarios() {
        return Stream.of(
            // Scenario 1: File is active in DB -> Skip (regardless of age)
            Arguments.of("Skip: File is active in DB",
                List.of("test-file.png"), OffsetDateTime.now().minusDays(10), false),

            // Scenario 2: File is rogue but very new (within grace period) -> Skip
            Arguments.of("Skip: File is rogue but newly uploaded",
                Collections.emptyList(), OffsetDateTime.now(), false),

            // Scenario 3: File is rogue and old (outside grace period) -> DELETE
            Arguments.of("Delete: File is rogue and older than threshold",
                Collections.emptyList(), OffsetDateTime.now().minusDays(2), true));
    }
}