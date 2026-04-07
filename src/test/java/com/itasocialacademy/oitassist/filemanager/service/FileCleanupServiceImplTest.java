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
import org.mockito.InOrder;
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    @DisplayName("Full Cleanup: Should mark dangling files, flush, and then purge")
    void runFullCleanup_ShouldExecuteAllPhases() {
        FileAsset danglingFile = FileAsset.builder()
            .id(1L).status(FileStatus.ATTACHED)
            .relatedEntityType(RelatedEntityType.NEWS).relatedEntityId(100L).build();
        InOrder order = inOrder(repository, storageProvider);

        when(repository.findAllAttachedFiles()).thenReturn(List.of(danglingFile));
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(query);
        when(query.setParameter(eq("ids"), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList()); // Parent missing
        when(repository.findIdsEligibleForCleanup(any(), any())).thenReturn(List.of(5L));
        when(storageProvider.listAllPhysicalKeys()).thenReturn(List.of("rogue.jpg"));
        when(repository.findAllActiveStorageKeys()).thenReturn(Collections.emptyList());
        when(storageProvider.getLastModified("rogue.jpg")).thenReturn(OffsetDateTime.now().minusDays(2));

        cleanupService.runFullCleanup();

        order.verify(repository).findAllAttachedFiles();
        order.verify(repository).findIdsEligibleForCleanup(any(), any());
        order.verify(storageProvider).listAllPhysicalKeys();

        assertThat(danglingFile.getStatus()).isEqualTo(FileStatus.SOFT_DELETED);
        verify(entityManager).flush();
        verify(fileService).deleteHard(5L);
        verify(storageProvider).deletePhysical("rogue.jpg");
    }

    @Test
    @DisplayName("Full Cleanup: Should be resilient if one phase fails (e.g., Purge)")
    void runFullCleanup_ShouldContinueDespiteIndividualFileErrors() {
        when(repository.findIdsEligibleForCleanup(any(), any())).thenReturn(List.of(1L, 2L));
        doThrow(new RuntimeException("Hard delete failed")).when(fileService).deleteHard(1L);

        cleanupService.runFullCleanup();

        verify(fileService).deleteHard(1L);
        verify(fileService).deleteHard(2L);
        verify(storageProvider).listAllPhysicalKeys();
    }

    // --- 1. PURGE PHASE TESTS ---

    @Test
    @DisplayName("Full Cleanup: Purge phase should be skipped gracefully when no IDs are found")
    void runFullCleanup_ShouldHandleEmptyPurgeList() {
        when(repository.findAllAttachedFiles()).thenReturn(Collections.emptyList());
        when(repository.findIdsEligibleForCleanup(any(), any())).thenReturn(Collections.emptyList());
        when(storageProvider.listAllPhysicalKeys()).thenReturn(Collections.emptyList());

        cleanupService.runFullCleanup();

        verify(repository).findIdsEligibleForCleanup(any(), any());
        verify(fileService, never()).deleteHard(anyLong());
    }

    @Test
    @DisplayName("Full Cleanup: Purge phase should be resilient if one hard delete fails")
    void runFullCleanup_PurgePhaseShouldBeResilientToExceptions() {
        when(repository.findAllAttachedFiles()).thenReturn(Collections.emptyList());
        when(repository.findIdsEligibleForCleanup(any(), any())).thenReturn(List.of(1L, 2L));
        doThrow(new RuntimeException("Hard delete failed for file 1")).when(fileService).deleteHard(1L);

        when(storageProvider.listAllPhysicalKeys()).thenReturn(Collections.emptyList());

        cleanupService.runFullCleanup();

        verify(fileService).deleteHard(1L);
        verify(fileService).deleteHard(2L);
        verify(storageProvider).listAllPhysicalKeys();
    }

    @Test
    @DisplayName("Full Cleanup: Purge phase should use combined repository query results")
    void runFullCleanup_PurgePhaseShouldUseCombinedQuery() {
        List<Long> ids = List.of(1L, 2L, 3L);
        when(repository.findIdsEligibleForCleanup(any(OffsetDateTime.class), any(OffsetDateTime.class)))
            .thenReturn(ids);
        when(repository.findAllAttachedFiles()).thenReturn(Collections.emptyList());
        when(storageProvider.listAllPhysicalKeys()).thenReturn(Collections.emptyList());

        cleanupService.runFullCleanup();

        verify(repository).findIdsEligibleForCleanup(any(OffsetDateTime.class), any(OffsetDateTime.class));
        verify(fileService, times(3)).deleteHard(anyLong());
        verify(fileService).deleteHard(1L);
        verify(fileService).deleteHard(2L);
        verify(fileService).deleteHard(3L);
    }

    @Test
    @DisplayName("Full Cleanup: Purge phase should continue processing subsequent files if one hard delete fails")
    void runFullCleanup_PurgePhase_ShouldBeResilientToErrors() {
        List<Long> idsToPurge = List.of(1L, 2L);
        when(repository.findIdsEligibleForCleanup(any(OffsetDateTime.class), any(OffsetDateTime.class)))
            .thenReturn(idsToPurge);
        doThrow(new RuntimeException("IO Error or DB Constraint"))
            .when(fileService).deleteHard(1L);

        when(repository.findAllAttachedFiles()).thenReturn(Collections.emptyList());
        when(storageProvider.listAllPhysicalKeys()).thenReturn(Collections.emptyList());

        cleanupService.runFullCleanup();

        verify(fileService).deleteHard(1L);
        verify(fileService).deleteHard(2L);
        verify(storageProvider).listAllPhysicalKeys();
    }

    // --- 2. ROGUE PHASE TESTS ---

    @Test
    @DisplayName("Full Cleanup: Rogue phase should skip deletion and continue if metadata retrieval fails")
    void runFullCleanup_RoguePhase_ShouldHandleMetadataErrors() {
        String errorKey = "unreadable-file.txt";
        String normalKey = "old-rogue.txt";

        when(storageProvider.listAllPhysicalKeys()).thenReturn(List.of(errorKey, normalKey));
        when(repository.findAllActiveStorageKeys()).thenReturn(Collections.emptyList());
        when(storageProvider.getLastModified(errorKey)).thenThrow(new RuntimeException("IO Error reading attributes"));
        when(storageProvider.getLastModified(normalKey)).thenReturn(OffsetDateTime.now().minusDays(10));
        when(repository.findAllAttachedFiles()).thenReturn(Collections.emptyList());
        when(repository.findIdsEligibleForCleanup(any(), any())).thenReturn(Collections.emptyList());

        cleanupService.runFullCleanup();

        verify(storageProvider, never()).deletePhysical(errorKey);
        verify(storageProvider).deletePhysical(normalKey);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideRogueFileScenarios")
    @DisplayName("Full Cleanup: Rogue phase should correctly handle various file scenarios")
    void runFullCleanup_RoguePhase_Parameterized(
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

        when(repository.findAllAttachedFiles()).thenReturn(Collections.emptyList());
        when(repository.findIdsEligibleForCleanup(any(), any())).thenReturn(Collections.emptyList());

        cleanupService.runFullCleanup();

        if (expectDelete) {
            verify(storageProvider).deletePhysical(fileName);
        } else {
            verify(storageProvider, never()).deletePhysical(fileName);
        }
    }

    @Test
    @DisplayName("Full Cleanup: Should NOT delete rogue files newer than the safety threshold (Grace Period)")
    void runFullCleanup_RoguePhase_ShouldSkipNewRogueFiles() {
        String newFile = "just/uploaded.png";

        when(storageProvider.listAllPhysicalKeys()).thenReturn(List.of(newFile));
        when(repository.findAllActiveStorageKeys()).thenReturn(Collections.emptyList());
        when(storageProvider.getLastModified(newFile)).thenReturn(OffsetDateTime.now());
        when(repository.findAllAttachedFiles()).thenReturn(Collections.emptyList());
        when(repository.findIdsEligibleForCleanup(any(), any())).thenReturn(Collections.emptyList());

        cleanupService.runFullCleanup();

        verify(storageProvider, never()).deletePhysical(newFile);
    }

    // --- 3. DANGLING PHASE TESTS ---

    @Test
    @DisplayName("Full Cleanup: Dangling phase should mark orphaned files as soft deleted")
    void runFullCleanup_DanglingPhase_ShouldProcessMissingParents() {
        FileAsset newsFile = FileAsset.builder()
            .id(1L)
            .status(FileStatus.ATTACHED)
            .relatedEntityType(RelatedEntityType.NEWS)
            .relatedEntityId(100L)
            .build();

        when(repository.findAllAttachedFiles()).thenReturn(List.of(newsFile));
        when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(query);
        when(query.setParameter(eq("ids"), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());
        when(repository.findIdsEligibleForCleanup(any(), any())).thenReturn(Collections.emptyList());
        when(storageProvider.listAllPhysicalKeys()).thenReturn(Collections.emptyList());

        cleanupService.runFullCleanup();

        assertThat(newsFile.getStatus()).isEqualTo(FileStatus.SOFT_DELETED);
        assertThat(newsFile.getDeletedAt()).isNotNull();
        verify(entityManager).flush();
    }

    @Test
    @DisplayName("Full Cleanup: Dangling phase should ignore files with null entity IDs")
    void runFullCleanup_DanglingPhase_ShouldIgnoreNullRelatedIds() {
        FileAsset nullIdFile = FileAsset.builder()
            .id(1L)
            .status(FileStatus.ATTACHED)
            .relatedEntityType(RelatedEntityType.NEWS)
            .relatedEntityId(null)
            .build();

        when(repository.findAllAttachedFiles()).thenReturn(List.of(nullIdFile));
        when(repository.findIdsEligibleForCleanup(any(), any())).thenReturn(Collections.emptyList());
        when(storageProvider.listAllPhysicalKeys()).thenReturn(Collections.emptyList());

        cleanupService.runFullCleanup();

        assertThat(nullIdFile.getStatus()).isEqualTo(FileStatus.ATTACHED);
        verify(entityManager, never()).createQuery(anyString(), any());
    }

    @Test
    @DisplayName("Full Cleanup: Should throw IllegalArgumentException if an attached file has an unmapped entity type")
    void runFullCleanup_ShouldThrowExceptionForUnmappedType() {
        FileAsset competitionFile = FileAsset.builder()
            .id(99L)
            .status(FileStatus.ATTACHED)
            .relatedEntityType(RelatedEntityType.COMPETITION)
            .relatedEntityId(123L)
            .build();

        when(repository.findAllAttachedFiles()).thenReturn(List.of(competitionFile));

        assertThatThrownBy(() -> cleanupService.runFullCleanup())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("EntityManager returned null query. Unknown entity type: Competition");

        verify(repository, never()).findIdsEligibleForCleanup(any(), any());
    }

    @Test
    @DisplayName("Full Cleanup: Rogue phase should skip files within the grace period (race condition safety)")
    void runFullCleanup_RoguePhase_ShouldSkipNewFiles() {
        String newFile = "newly-uploaded-file.txt";

        when(storageProvider.listAllPhysicalKeys()).thenReturn(List.of(newFile));
        when(repository.findAllActiveStorageKeys()).thenReturn(Collections.emptyList());
        when(storageProvider.getLastModified(newFile)).thenReturn(OffsetDateTime.now());
        when(repository.findAllAttachedFiles()).thenReturn(Collections.emptyList());
        when(repository.findIdsEligibleForCleanup(any(), any())).thenReturn(Collections.emptyList());

        cleanupService.runFullCleanup();

        verify(storageProvider, never()).deletePhysical(newFile);
    }

    @Test
    @DisplayName("Full Cleanup: Dangling phase should skip database queries if no attached files exist")
    void runFullCleanup_DanglingPhase_ShouldHandleEmptyList() {
        when(repository.findAllAttachedFiles()).thenReturn(Collections.emptyList());
        when(repository.findIdsEligibleForCleanup(any(), any())).thenReturn(Collections.emptyList());
        when(storageProvider.listAllPhysicalKeys()).thenReturn(Collections.emptyList());

        cleanupService.runFullCleanup();

        verify(entityManager, never()).createQuery(anyString(), any());

        verify(repository).findIdsEligibleForCleanup(any(), any());
        verify(storageProvider).listAllPhysicalKeys();
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