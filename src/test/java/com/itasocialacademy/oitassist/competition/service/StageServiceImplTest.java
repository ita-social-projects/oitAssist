package com.itasocialacademy.oitassist.competition.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.competition.dao.enums.StageScope;
import com.itasocialacademy.oitassist.competition.dao.enums.StageStatus;
import com.itasocialacademy.oitassist.competition.dao.model.Stage;
import com.itasocialacademy.oitassist.competition.dao.repository.StageRepository;
import com.itasocialacademy.oitassist.competition.dto.request.ChangeStageStatusRequest;
import com.itasocialacademy.oitassist.competition.dto.request.CreateStageRequest;
import com.itasocialacademy.oitassist.competition.dto.request.UpdateStageRequest;
import com.itasocialacademy.oitassist.competition.dto.response.StageResponse;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionHierarchyValidationException;
import com.itasocialacademy.oitassist.competition.exceptions.StageNotFoundException;
import com.itasocialacademy.oitassist.competition.exceptions.StaleEntityVersionException;
import com.itasocialacademy.oitassist.competition.mapper.StageMapper;
import com.itasocialacademy.oitassist.competition.validation.HierarchyValidator;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StageServiceImplTest {

    @Mock
    private StageRepository stageRepository;
    @Mock
    private HierarchyValidator validator;
    @Mock
    private StageMapper mapper;

    @InjectMocks
    private StageServiceImpl stageService;

    private Stage stage;
    private ZonedDateTime start;
    private ZonedDateTime finish;

    @BeforeEach
    void setUp() {
        start = ZonedDateTime.of(2026, 6, 25, 10, 0, 0, 0, ZoneId.of("UTC"));
        finish = start.plusDays(10);

        stage = Stage.builder()
            .id(10L)
            .competitionId(1L)
            .title("Regional Stage")
            .dateStart(start)
            .dateFinish(finish)
            .sortPosition((short) 1)
            .scope(StageScope.REGIONAL)
            .status(StageStatus.SCHEDULED)
            .version(1L)
            .build();
    }

    // ---- create ----

    @Test
    void create_withoutSortPosition_shouldAutoIncrementFromLastStage() {
        CreateStageRequest request = new CreateStageRequest("New Stage", "Desc", start, finish,
            StageScope.CITY);

        Stage mappedEntity = Stage.builder().title("New Stage").scope(StageScope.CITY).build();
        Stage lastStage = Stage.builder().sortPosition((short) 3).build();

        when(mapper.toEntity(request)).thenReturn(mappedEntity);
        when(stageRepository.existsByCompetitionIdAndTitle(1L, "New Stage")).thenReturn(false);
        when(stageRepository.existsByCompetitionIdAndScope(1L, StageScope.CITY)).thenReturn(false);
        when(stageRepository.findTopByCompetitionIdOrderBySortPositionDesc(1L)).thenReturn(Optional.of(lastStage));
        when(stageRepository.save(any(Stage.class))).thenReturn(mappedEntity);
        when(mapper.toResponse(mappedEntity)).thenReturn(getStageResponse());

        stageService.create(1L, request);

        assertEquals((short) 4, mappedEntity.getSortPosition());
    }

    @Test
    void create_withoutSortPosition_noExistingStages_shouldStartAtOne() {
        CreateStageRequest request = new CreateStageRequest("New Stage", "Desc", start, finish,
            StageScope.CITY);

        Stage mappedEntity = Stage.builder().title("New Stage").scope(StageScope.CITY).build();

        when(mapper.toEntity(request)).thenReturn(mappedEntity);
        when(stageRepository.existsByCompetitionIdAndTitle(1L, "New Stage")).thenReturn(false);
        when(stageRepository.existsByCompetitionIdAndScope(1L, StageScope.CITY)).thenReturn(false);
        when(stageRepository.findTopByCompetitionIdOrderBySortPositionDesc(1L)).thenReturn(Optional.empty());
        when(stageRepository.save(any(Stage.class))).thenReturn(mappedEntity);
        when(mapper.toResponse(mappedEntity)).thenReturn(getStageResponse());

        stageService.create(1L, request);

        assertEquals((short) 1, mappedEntity.getSortPosition());
    }

    @Test
    void create_duplicateTitle_shouldThrow() {
        CreateStageRequest request = new CreateStageRequest("Regional Stage", "Desc", start, finish,
            StageScope.CITY);
        Stage mappedEntity = Stage.builder().title("Regional Stage").scope(StageScope.CITY).build();

        when(mapper.toEntity(request)).thenReturn(mappedEntity);
        when(stageRepository.existsByCompetitionIdAndTitle(1L, "Regional Stage")).thenReturn(true);

        assertThrows(CompetitionHierarchyValidationException.class, () -> stageService.create(1L, request));
        verify(stageRepository, never()).save(any());
    }

    @Test
    void create_duplicateScope_shouldThrow() {
        CreateStageRequest request = new CreateStageRequest("New Stage", "Desc", start, finish,
            StageScope.REGIONAL);
        Stage mappedEntity = Stage.builder().title("New Stage").scope(StageScope.REGIONAL).build();

        when(mapper.toEntity(request)).thenReturn(mappedEntity);
        when(stageRepository.existsByCompetitionIdAndTitle(1L, "New Stage")).thenReturn(false);
        when(stageRepository.existsByCompetitionIdAndScope(1L, StageScope.REGIONAL)).thenReturn(true);

        CompetitionHierarchyValidationException exception = assertThrows(
            CompetitionHierarchyValidationException.class, () -> stageService.create(1L, request));

        assertEquals("A stage with scope REGIONAL already exists in this competition.", exception.getMessage());
        verify(stageRepository, never()).save(any());
    }

    @Test
    void create_duplicateSortPosition_shouldThrow() {
        CreateStageRequest request = new CreateStageRequest("New Stage", "Desc", start, finish,
            StageScope.CITY);
        Stage mappedEntity = Stage.builder().title("New Stage").scope(StageScope.CITY).sortPosition((short) 1).build();

        when(mapper.toEntity(request)).thenReturn(mappedEntity);
        when(stageRepository.existsByCompetitionIdAndTitle(1L, "New Stage")).thenReturn(false);
        when(stageRepository.existsByCompetitionIdAndScope(1L, StageScope.CITY)).thenReturn(false);
        when(stageRepository.existsByCompetitionIdAndSortPosition(1L, (short) 1)).thenReturn(true);

        assertThrows(CompetitionHierarchyValidationException.class, () -> stageService.create(1L, request));
        verify(stageRepository, never()).save(any());
    }

    // ---- getById ----

    @Test
    void getById_found_shouldReturnResponseAndCheckVisibility() {
        when(stageRepository.findById(10L)).thenReturn(Optional.of(stage));
        when(mapper.toResponse(stage)).thenReturn(getStageResponse());

        StageResponse response = stageService.getById(10L);

        assertNotNull(response);
        verify(validator).checkVisibilityAccess(1L);
    }

    @Test
    void getById_notFound_shouldThrow() {
        when(stageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(StageNotFoundException.class, () -> stageService.getById(99L));
        verify(validator, never()).checkVisibilityAccess(any());
    }

    // ---- getAllByCompetitionId ----

    @Test
    void getAllByCompetitionId_shouldReturnMappedList() {
        Stage other = Stage.builder().id(20L).competitionId(1L).build();
        when(stageRepository.findAllByCompetitionIdOrderBySortPositionAsc(1L)).thenReturn(List.of(stage, other));
        when(mapper.toResponse(stage)).thenReturn(getStageResponse());
        when(mapper.toResponse(other)).thenReturn(StageResponse.builder().id(20L).build());

        List<StageResponse> result = stageService.getAllByCompetitionId(1L);

        assertEquals(2, result.size());
        verify(validator).checkVisibilityAccess(1L);
    }

    // ---- update ----

    @Test
    void update_validRequest_shouldSucceed() {
        UpdateStageRequest request = new UpdateStageRequest("Updated Title", "New desc", start, finish,
            StageScope.REGIONAL, (short) 2, 1L);

        when(stageRepository.findById(10L)).thenReturn(Optional.of(stage));
        when(stageRepository.findAllByCompetitionIdOrderBySortPositionAsc(1L)).thenReturn(List.of(stage));
        when(stageRepository.save(any(Stage.class))).thenReturn(stage);
        when(mapper.toResponse(stage)).thenReturn(getStageResponse());

        StageResponse response = stageService.update(1L, 10L, request);

        assertNotNull(response);
        assertEquals("Updated Title", stage.getTitle());
        assertEquals((short) 2, stage.getSortPosition());
        verify(validator).validateEntityVersion(1L, 1L, Stage.class, 10L);
        verify(validator).validateStageEligibility(1L, 1L);
        verify(validator).validateImmutabilityByCompetitionId(1L);
        verify(validator).validateStageDatesAgainstExistingTours(10L, start, finish);
    }

    @Test
    void update_versionMismatch_shouldThrow() {
        UpdateStageRequest request = new UpdateStageRequest("Title", "Desc", start, finish,
            StageScope.REGIONAL, null, 5L);

        when(stageRepository.findById(10L)).thenReturn(Optional.of(stage));
        doThrow(new StaleEntityVersionException(Stage.class, 10L))
            .when(validator).validateEntityVersion(5L, 1L, Stage.class, 10L);

        assertThrows(StaleEntityVersionException.class, () -> stageService.update(1L, 10L, request));
        verify(stageRepository, never()).save(any());
    }

    @Test
    void update_stageNotBelongingToCompetition_shouldThrow() {
        UpdateStageRequest request = new UpdateStageRequest("Title", "Desc", start, finish,
            StageScope.REGIONAL, null, 1L);

        when(stageRepository.findById(10L)).thenReturn(Optional.of(stage));
        doThrow(new CompetitionHierarchyValidationException("Stage does not belong to this competition"))
            .when(validator).validateStageEligibility(999L, 1L);

        assertThrows(CompetitionHierarchyValidationException.class,
            () -> stageService.update(999L, 10L, request));
        verify(stageRepository, never()).save(any());
    }

    @Test
    void update_duplicateTitleAmongOtherStages_shouldThrow() {
        UpdateStageRequest request = new UpdateStageRequest("Existing Title", "Desc", start, finish,
            StageScope.REGIONAL, (short) 1, 1L);
        Stage other = Stage.builder().id(20L).title("Existing Title").scope(StageScope.CITY)
            .sortPosition((short) 9).build();

        when(stageRepository.findById(10L)).thenReturn(Optional.of(stage));
        when(stageRepository.findAllByCompetitionIdOrderBySortPositionAsc(1L)).thenReturn(List.of(stage, other));

        assertThrows(CompetitionHierarchyValidationException.class, () -> stageService.update(1L, 10L, request));
        verify(stageRepository, never()).save(any());
    }

    @Test
    void update_duplicateScopeAmongOtherStages_shouldThrow() {
        UpdateStageRequest request = new UpdateStageRequest("New Title", "Desc", start, finish,
            StageScope.CITY, (short) 1, 1L);
        Stage other = Stage.builder().id(20L).title("Other Title").scope(StageScope.CITY)
            .sortPosition((short) 9).build();

        when(stageRepository.findById(10L)).thenReturn(Optional.of(stage));
        when(stageRepository.findAllByCompetitionIdOrderBySortPositionAsc(1L)).thenReturn(List.of(stage, other));

        assertThrows(CompetitionHierarchyValidationException.class, () -> stageService.update(1L, 10L, request));
    }

    @Test
    void update_duplicateSortPositionAmongOtherStages_shouldThrow() {
        UpdateStageRequest request = new UpdateStageRequest("New Title", "Desc", start, finish,
            StageScope.REGIONAL, (short) 9, 1L);
        Stage other = Stage.builder().id(20L).title("Other Title").scope(StageScope.CITY)
            .sortPosition((short) 9).build();

        when(stageRepository.findById(10L)).thenReturn(Optional.of(stage));
        when(stageRepository.findAllByCompetitionIdOrderBySortPositionAsc(1L)).thenReturn(List.of(stage, other));

        assertThrows(CompetitionHierarchyValidationException.class, () -> stageService.update(1L, 10L, request));
    }

    @Test
    void update_notFound_shouldThrow() {
        UpdateStageRequest request = new UpdateStageRequest("Title", "Desc", start, finish,
            StageScope.REGIONAL, null, 1L);
        when(stageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(StageNotFoundException.class, () -> stageService.update(1L, 99L, request));
    }

    // ---- changeStatus ----

    @Test
    void changeStatus_toInProgress_shouldValidateEligibilityToStart() {
        ChangeStageStatusRequest request = new ChangeStageStatusRequest(StageStatus.IN_PROGRESS, 1L);

        when(stageRepository.findById(10L)).thenReturn(Optional.of(stage));
        when(stageRepository.save(any(Stage.class))).thenReturn(stage);
        when(mapper.toResponse(stage)).thenReturn(getStageResponse());

        stageService.changeStatus(1L, 10L, request);

        assertEquals(StageStatus.IN_PROGRESS, stage.getStatus());
        verify(validator).validateStageEligibilityToStart(stage);
        verify(validator, never()).validateAllToursCompletedForStage(any());
    }

    @Test
    void changeStatus_toFinished_shouldValidateAllToursCompleted() {
        stage.setStatus(StageStatus.IN_PROGRESS);
        ChangeStageStatusRequest request = new ChangeStageStatusRequest(StageStatus.FINISHED, 1L);

        when(stageRepository.findById(10L)).thenReturn(Optional.of(stage));
        when(stageRepository.save(any(Stage.class))).thenReturn(stage);
        when(mapper.toResponse(stage)).thenReturn(getStageResponse());

        stageService.changeStatus(1L, 10L, request);

        assertEquals(StageStatus.FINISHED, stage.getStatus());
        verify(validator).validateAllToursCompletedForStage(10L);
        verify(validator, never()).validateStageEligibilityToStart(any());
    }

    @Test
    void changeStatus_versionMismatch_shouldThrow() {
        ChangeStageStatusRequest request = new ChangeStageStatusRequest(StageStatus.IN_PROGRESS, 99L);

        when(stageRepository.findById(10L)).thenReturn(Optional.of(stage));
        doThrow(new StaleEntityVersionException(Stage.class, 10L))
            .when(validator).validateEntityVersion(99L, 1L, Stage.class, 10L);

        assertThrows(StaleEntityVersionException.class, () -> stageService.changeStatus(1L, 10L, request));
        verify(stageRepository, never()).save(any());
    }

    @Test
    void changeStatus_invalidTransition_shouldPropagate() {
        ChangeStageStatusRequest request = new ChangeStageStatusRequest(StageStatus.FINISHED, 1L);

        when(stageRepository.findById(10L)).thenReturn(Optional.of(stage));
        doThrow(
            new CompetitionHierarchyValidationException("Invalid stage status transition from SCHEDULED to FINISHED"))
            .when(validator).validateStageStatusTransition(StageStatus.SCHEDULED, StageStatus.FINISHED);

        assertThrows(CompetitionHierarchyValidationException.class, () -> stageService.changeStatus(1L, 10L, request));
        verify(stageRepository, never()).save(any());
    }

    @Test
    void changeStatus_notFound_shouldThrow() {
        ChangeStageStatusRequest request = new ChangeStageStatusRequest(StageStatus.IN_PROGRESS, 1L);
        when(stageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(StageNotFoundException.class, () -> stageService.changeStatus(1L, 99L, request));
    }

    // ---- delete ----

    @Test
    void delete_valid_shouldDeleteStage() {
        when(stageRepository.findById(10L)).thenReturn(Optional.of(stage));

        assertDoesNotThrow(() -> stageService.delete(1L, 10L));

        verify(validator).validateStageEligibility(1L, 1L);
        verify(validator).validateImmutabilityByCompetitionId(1L);
        verify(stageRepository).delete(stage);
    }

    @Test
    void delete_stageNotBelongingToCompetition_shouldThrow() {
        when(stageRepository.findById(10L)).thenReturn(Optional.of(stage));
        doThrow(new CompetitionHierarchyValidationException("Stage does not belong to this competition"))
            .when(validator).validateStageEligibility(999L, 1L);

        assertThrows(CompetitionHierarchyValidationException.class, () -> stageService.delete(999L, 10L));
        verify(stageRepository, never()).delete(any());
    }

    @Test
    void delete_notFound_shouldThrow() {
        when(stageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(StageNotFoundException.class, () -> stageService.delete(1L, 99L));
        verify(stageRepository, never()).delete(any());
    }

    private static StageResponse getStageResponse() {
        return StageResponse.builder().id(10L).title("Regional Stage").build();
    }
}
