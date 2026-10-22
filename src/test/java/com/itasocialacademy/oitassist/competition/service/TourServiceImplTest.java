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

import com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus;
import com.itasocialacademy.oitassist.competition.dao.model.Tour;
import com.itasocialacademy.oitassist.competition.dao.repository.TourRepository;
import com.itasocialacademy.oitassist.competition.dto.request.ChangeTourStatusRequest;
import com.itasocialacademy.oitassist.competition.dto.request.CreateTourRequest;
import com.itasocialacademy.oitassist.competition.dto.request.ReorderToursRequest;
import com.itasocialacademy.oitassist.competition.dto.request.UpdateTourRequest;
import com.itasocialacademy.oitassist.competition.dto.response.TourResponse;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionHierarchyValidationException;
import com.itasocialacademy.oitassist.competition.exceptions.StaleEntityVersionException;
import com.itasocialacademy.oitassist.competition.exceptions.TourNotFoundException;
import com.itasocialacademy.oitassist.competition.mapper.TourMapper;
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
class TourServiceImplTest {

    @Mock
    private TourRepository tourRepository;
    @Mock
    private TourMapper mapper;
    @Mock
    private HierarchyValidator validator;

    @InjectMocks
    private TourServiceImpl tourService;

    private Tour tour;
    private ZonedDateTime start;
    private ZonedDateTime finish;

    @BeforeEach
    void setUp() {
        start = ZonedDateTime.of(2026, 6, 25, 10, 0, 0, 0, ZoneId.of("UTC"));
        finish = start.plusDays(1);

        tour = Tour.builder()
            .id(100L)
            .stageId(10L)
            .title("Theoretical Round")
            .dateStart(start)
            .dateFinish(finish)
            .sortPosition((short) 1)
            .location("Room 101")
            .executionStatus(ExecutionStatus.SCHEDULED)
            .version(1L)
            .build();
    }

    // ---- create ----

    @Test
    void create_withoutSortPosition_shouldAutoIncrementFromLastTour() {
        CreateTourRequest request = new CreateTourRequest("New Tour", "Desc", start, finish, "Room 1");
        Tour mappedEntity = Tour.builder().title("New Tour").build();
        Tour lastTour = Tour.builder().sortPosition((short) 2).build();

        when(mapper.toEntity(request)).thenReturn(mappedEntity);
        when(tourRepository.existsByStageIdAndTitle(10L, "New Tour")).thenReturn(false);
        when(tourRepository.findTopByStageIdOrderBySortPositionDesc(10L)).thenReturn(Optional.of(lastTour));
        when(tourRepository.save(any(Tour.class))).thenReturn(mappedEntity);
        when(mapper.toResponse(mappedEntity)).thenReturn(getTourResponse());

        tourService.create(10L, request);

        assertEquals((short) 3, mappedEntity.getSortPosition());
    }

    @Test
    void create_withoutSortPosition_noExistingTours_shouldStartAtOne() {
        CreateTourRequest request = new CreateTourRequest("New Tour", "Desc", start, finish, "Room 1");
        Tour mappedEntity = Tour.builder().title("New Tour").build();

        when(mapper.toEntity(request)).thenReturn(mappedEntity);
        when(tourRepository.existsByStageIdAndTitle(10L, "New Tour")).thenReturn(false);
        when(tourRepository.findTopByStageIdOrderBySortPositionDesc(10L)).thenReturn(Optional.empty());
        when(tourRepository.save(any(Tour.class))).thenReturn(mappedEntity);
        when(mapper.toResponse(mappedEntity)).thenReturn(getTourResponse());

        tourService.create(10L, request);

        assertEquals((short) 1, mappedEntity.getSortPosition());
    }

    @Test
    void create_duplicateTitle_shouldThrow() {
        CreateTourRequest request = new CreateTourRequest("Theoretical Round", "Desc", start, finish, "Room 1");
        Tour mappedEntity = Tour.builder().title("Theoretical Round").build();

        when(mapper.toEntity(request)).thenReturn(mappedEntity);
        when(tourRepository.existsByStageIdAndTitle(10L, "Theoretical Round")).thenReturn(true);

        assertThrows(CompetitionHierarchyValidationException.class, () -> tourService.create(10L, request));
        verify(tourRepository, never()).save(any());
    }

    // ---- getById ----

    @Test
    void getById_found_shouldReturnResponseAndCheckVisibility() {
        when(tourRepository.findById(100L)).thenReturn(Optional.of(tour));
        when(mapper.toResponse(tour)).thenReturn(getTourResponse());

        TourResponse response = tourService.getById(100L);

        assertNotNull(response);
        verify(validator).checkVisibilityAccessByStageId(10L);
    }

    @Test
    void getById_notFound_shouldThrow() {
        when(tourRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(TourNotFoundException.class, () -> tourService.getById(999L));
        verify(validator, never()).checkVisibilityAccessByStageId(any());
    }

    // ---- getAllByStageId ----

    @Test
    void getAllByStageId_shouldReturnMappedList() {
        Tour other = Tour.builder().id(101L).stageId(10L).build();
        when(tourRepository.findAllByStageIdOrderBySortPositionAsc(10L)).thenReturn(List.of(tour, other));
        when(mapper.toResponse(tour)).thenReturn(getTourResponse());
        when(mapper.toResponse(other)).thenReturn(TourResponse.builder().id(101L).build());

        List<TourResponse> result = tourService.getAllByStageId(10L);

        assertEquals(2, result.size());
        verify(validator).checkVisibilityAccessByStageId(10L);
    }

    // ---- update ----

    @Test
    void update_validRequest_shouldSucceed() {
        UpdateTourRequest request = new UpdateTourRequest("Updated Title", "New desc", start, finish,
            "Room 2", (short) 2, 1L);

        when(tourRepository.findById(100L)).thenReturn(Optional.of(tour));
        when(tourRepository.findAllByStageIdOrderBySortPositionAsc(10L)).thenReturn(List.of(tour));
        when(tourRepository.save(any(Tour.class))).thenReturn(tour);
        when(mapper.toResponse(tour)).thenReturn(getTourResponse());

        TourResponse response = tourService.update(10L, 100L, request);

        assertNotNull(response);
        assertEquals("Updated Title", tour.getTitle());
        assertEquals("Room 2", tour.getLocation());
        assertEquals((short) 2, tour.getSortPosition());
        verify(validator).validateEntityVersion(1L, 1L, Tour.class, 100L);
        verify(validator).validateTourEligibility(10L, 10L);
        verify(validator).validateImmutabilityByStageId(10L);
    }

    @Test
    void update_versionMismatch_shouldThrow() {
        UpdateTourRequest request = new UpdateTourRequest("Title", "Desc", start, finish, "Room", null, 5L);

        when(tourRepository.findById(100L)).thenReturn(Optional.of(tour));
        doThrow(new StaleEntityVersionException(Tour.class, 100L))
            .when(validator).validateEntityVersion(5L, 1L, Tour.class, 100L);

        assertThrows(StaleEntityVersionException.class, () -> tourService.update(10L, 100L, request));
        verify(tourRepository, never()).save(any());
    }

    @Test
    void update_tourNotBelongingToStage_shouldThrow() {
        UpdateTourRequest request = new UpdateTourRequest("Title", "Desc", start, finish, "Room", null, 1L);

        when(tourRepository.findById(100L)).thenReturn(Optional.of(tour));
        doThrow(new CompetitionHierarchyValidationException("Tour does not belong to this stage"))
            .when(validator).validateTourEligibility(999L, 10L);

        assertThrows(CompetitionHierarchyValidationException.class, () -> tourService.update(999L, 100L, request));
        verify(tourRepository, never()).save(any());
    }

    @Test
    void update_duplicateTitleAmongOtherTours_shouldThrow() {
        UpdateTourRequest request = new UpdateTourRequest("Existing Title", "Desc", start, finish,
            "Room", (short) 1, 1L);
        Tour other = Tour.builder().id(101L).title("Existing Title").sortPosition((short) 9).build();

        when(tourRepository.findById(100L)).thenReturn(Optional.of(tour));
        when(tourRepository.findAllByStageIdOrderBySortPositionAsc(10L)).thenReturn(List.of(tour, other));

        assertThrows(CompetitionHierarchyValidationException.class, () -> tourService.update(10L, 100L, request));
        verify(tourRepository, never()).save(any());
    }

    @Test
    void update_duplicateSortPositionAmongOtherTours_shouldThrow() {
        UpdateTourRequest request = new UpdateTourRequest("New Title", "Desc", start, finish, "Room", (short) 9, 1L);
        Tour other = Tour.builder().id(101L).title("Other Title").sortPosition((short) 9).build();

        when(tourRepository.findById(100L)).thenReturn(Optional.of(tour));
        when(tourRepository.findAllByStageIdOrderBySortPositionAsc(10L)).thenReturn(List.of(tour, other));

        assertThrows(CompetitionHierarchyValidationException.class, () -> tourService.update(10L, 100L, request));
    }

    @Test
    void update_notFound_shouldThrow() {
        UpdateTourRequest request = new UpdateTourRequest("Title", "Desc", start, finish, "Room", null, 1L);
        when(tourRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(TourNotFoundException.class, () -> tourService.update(10L, 999L, request));
    }

    // ---- changeStatus ----

    @Test
    void changeStatus_fromScheduledToInProgress_shouldValidateEligibilityToStart() {
        ChangeTourStatusRequest request = new ChangeTourStatusRequest(ExecutionStatus.IN_PROGRESS, 1L);

        when(tourRepository.findById(100L)).thenReturn(Optional.of(tour));
        when(tourRepository.save(any(Tour.class))).thenReturn(tour);
        when(mapper.toResponse(tour)).thenReturn(getTourResponse());

        tourService.changeStatus(10L, 100L, request);

        assertEquals(ExecutionStatus.IN_PROGRESS, tour.getExecutionStatus());
        verify(validator).validateTourEligibilityToStart(tour);
        verify(validator, never()).validateTourEligibilityToResume(any());
    }

    @Test
    void changeStatus_fromClosedToInProgress_shouldValidateEligibilityToResume() {
        tour.setExecutionStatus(ExecutionStatus.CLOSED);
        ChangeTourStatusRequest request = new ChangeTourStatusRequest(ExecutionStatus.IN_PROGRESS, 1L);

        when(tourRepository.findById(100L)).thenReturn(Optional.of(tour));
        when(tourRepository.save(any(Tour.class))).thenReturn(tour);
        when(mapper.toResponse(tour)).thenReturn(getTourResponse());

        tourService.changeStatus(10L, 100L, request);

        assertEquals(ExecutionStatus.IN_PROGRESS, tour.getExecutionStatus());
        verify(validator).validateTourEligibilityToResume(tour);
        verify(validator, never()).validateTourEligibilityToStart(any());
    }

    @Test
    void changeStatus_toCancelled_shouldSkipEligibilityChecks() {
        ChangeTourStatusRequest request = new ChangeTourStatusRequest(ExecutionStatus.CANCELLED, 1L);

        when(tourRepository.findById(100L)).thenReturn(Optional.of(tour));
        when(tourRepository.save(any(Tour.class))).thenReturn(tour);
        when(mapper.toResponse(tour)).thenReturn(getTourResponse());

        tourService.changeStatus(10L, 100L, request);

        assertEquals(ExecutionStatus.CANCELLED, tour.getExecutionStatus());
        verify(validator, never()).validateTourEligibilityToStart(any());
        verify(validator, never()).validateTourEligibilityToResume(any());
    }

    @Test
    void changeStatus_versionMismatch_shouldThrow() {
        ChangeTourStatusRequest request = new ChangeTourStatusRequest(ExecutionStatus.IN_PROGRESS, 99L);

        when(tourRepository.findById(100L)).thenReturn(Optional.of(tour));
        doThrow(new StaleEntityVersionException(Tour.class, 100L))
            .when(validator).validateEntityVersion(99L, 1L, Tour.class, 100L);

        assertThrows(StaleEntityVersionException.class, () -> tourService.changeStatus(10L, 100L, request));
        verify(tourRepository, never()).save(any());
    }

    @Test
    void changeStatus_notFound_shouldThrow() {
        ChangeTourStatusRequest request = new ChangeTourStatusRequest(ExecutionStatus.IN_PROGRESS, 1L);
        when(tourRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(TourNotFoundException.class, () -> tourService.changeStatus(10L, 999L, request));
    }

    // ---- reorder ----

    @Test
    void reorder_validRequest_shouldReassignPositionsInGivenOrder() {
        Tour tourA = Tour.builder().id(1L).sortPosition((short) 1).build();
        Tour tourB = Tour.builder().id(2L).sortPosition((short) 2).build();
        Tour tourC = Tour.builder().id(3L).sortPosition((short) 3).build();

        when(tourRepository.findAllByStageIdOrderBySortPositionAsc(10L))
            .thenReturn(List.of(tourA, tourB, tourC));
        when(tourRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Tour.class))).thenReturn(getTourResponse());

        ReorderToursRequest request = new ReorderToursRequest(List.of(3L, 1L, 2L));

        List<TourResponse> result = tourService.reorder(10L, request);

        assertEquals(3, result.size());
        assertEquals((short) 1, tourC.getSortPosition());
        assertEquals((short) 2, tourA.getSortPosition());
        assertEquals((short) 3, tourB.getSortPosition());
        verify(validator).validateToursNotStartedByStageId(10L);
    }

    @Test
    void reorder_duplicateIds_shouldThrow() {
        Tour tourA = Tour.builder().id(1L).build();
        Tour tourB = Tour.builder().id(2L).build();

        when(tourRepository.findAllByStageIdOrderBySortPositionAsc(10L)).thenReturn(List.of(tourA, tourB));

        ReorderToursRequest request = new ReorderToursRequest(List.of(1L, 1L));

        assertThrows(CompetitionHierarchyValidationException.class, () -> tourService.reorder(10L, request));
        verify(tourRepository, never()).saveAll(any());
    }

    @Test
    void reorder_missingTourId_shouldThrow() {
        Tour tourA = Tour.builder().id(1L).build();
        Tour tourB = Tour.builder().id(2L).build();

        when(tourRepository.findAllByStageIdOrderBySortPositionAsc(10L)).thenReturn(List.of(tourA, tourB));

        ReorderToursRequest request = new ReorderToursRequest(List.of(1L)); // missing id 2

        assertThrows(CompetitionHierarchyValidationException.class, () -> tourService.reorder(10L, request));
        verify(tourRepository, never()).saveAll(any());
    }

    @Test
    void reorder_extraUnknownTourId_shouldThrow() {
        Tour tourA = Tour.builder().id(1L).build();

        when(tourRepository.findAllByStageIdOrderBySortPositionAsc(10L)).thenReturn(List.of(tourA));

        ReorderToursRequest request = new ReorderToursRequest(List.of(1L, 999L));

        assertThrows(CompetitionHierarchyValidationException.class, () -> tourService.reorder(10L, request));
        verify(tourRepository, never()).saveAll(any());
    }

    // ---- delete ----

    @Test
    void delete_valid_shouldDeleteTour() {
        when(tourRepository.findById(100L)).thenReturn(Optional.of(tour));

        assertDoesNotThrow(() -> tourService.delete(10L, 100L));

        verify(validator).validateTourEligibility(10L, 10L);
        verify(validator).validateImmutabilityByStageId(10L);
        verify(validator).validateTourDeletionKeepsStageNonEmpty(10L);
        verify(tourRepository).delete(tour);
    }

    @Test
    void delete_tourNotBelongingToStage_shouldThrow() {
        when(tourRepository.findById(100L)).thenReturn(Optional.of(tour));
        doThrow(new CompetitionHierarchyValidationException("Tour does not belong to this stage"))
            .when(validator).validateTourEligibility(999L, 10L);

        assertThrows(CompetitionHierarchyValidationException.class, () -> tourService.delete(999L, 100L));
        verify(tourRepository, never()).delete(any());
    }

    @Test
    void delete_lastTourOnNonDraftCompetition_shouldThrow() {
        when(tourRepository.findById(100L)).thenReturn(Optional.of(tour));
        doThrow(new CompetitionHierarchyValidationException(
            "Cannot delete tour: it is the last tour of this stage, and the competition has already left DRAFT status."))
            .when(validator).validateTourDeletionKeepsStageNonEmpty(10L);

        assertThrows(CompetitionHierarchyValidationException.class, () -> tourService.delete(10L, 100L));
        verify(tourRepository, never()).delete(any());
    }

    @Test
    void delete_notFound_shouldThrow() {
        when(tourRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(TourNotFoundException.class, () -> tourService.delete(10L, 999L));
        verify(tourRepository, never()).delete(any());
    }

    private static TourResponse getTourResponse() {
        return TourResponse.builder().id(100L).title("Theoretical Round").build();
    }
}