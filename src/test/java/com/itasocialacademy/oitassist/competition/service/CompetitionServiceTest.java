package com.itasocialacademy.oitassist.competition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus;
import com.itasocialacademy.oitassist.competition.dao.enums.StageScope;
import com.itasocialacademy.oitassist.competition.dao.enums.StageStatus;
import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import com.itasocialacademy.oitassist.competition.dao.model.Stage;
import com.itasocialacademy.oitassist.competition.dao.model.Tour;
import com.itasocialacademy.oitassist.competition.dao.repository.CompetitionRepository;
import com.itasocialacademy.oitassist.competition.dao.repository.StageRepository;
import com.itasocialacademy.oitassist.competition.dao.repository.TourRepository;
import com.itasocialacademy.oitassist.competition.dto.filter.CompetitionSearchFilter;
import com.itasocialacademy.oitassist.competition.dto.request.CreateCompetitionRequest;
import com.itasocialacademy.oitassist.competition.dto.response.CompetitionResponse;
import com.itasocialacademy.oitassist.competition.dto.response.CompetitionTreeResponse;
import com.itasocialacademy.oitassist.competition.dto.response.StageResponse;
import com.itasocialacademy.oitassist.competition.dto.response.StageTreeResponse;
import com.itasocialacademy.oitassist.competition.dto.response.TourResponse;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionHierarchyValidationException;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionNotFoundException;
import com.itasocialacademy.oitassist.competition.mapper.CompetitionMapper;
import com.itasocialacademy.oitassist.competition.mapper.StageMapper;
import com.itasocialacademy.oitassist.competition.mapper.TourMapper;
import com.itasocialacademy.oitassist.competition.validation.HierarchyValidator;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class CompetitionServiceTest {

    @Mock
    private CompetitionRepository competitionRepository;
    @Mock
    private StageRepository stageRepository;
    @Mock
    private TourRepository tourRepository;
    @Mock
    private CompetitionMapper mapper;
    @Mock
    private StageMapper stageMapper;
    @Mock
    private TourMapper tourMapper;
    @Mock
    private SecurityFacade securityFacade;
    @Mock
    private HierarchyValidator validator;

    @InjectMocks
    private CompetitionServiceImpl competitionService;

    private Competition competition;

    @BeforeEach
    void setUp() {
        competition = Competition.builder()
            .id(1L)
            .title("Test Olympiad")
            .competitionStatus(CompetitionStatus.DRAFT)
            .build();
    }

    // ---- changeStatus ----

    @Test
    void changeStatus_draftToEnrollment_shouldSucceed() {
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));

        when(stageRepository.existsByCompetitionId(1L)).thenReturn(true);
        when(stageRepository.countStagesWithoutTours(1L)).thenReturn(0L);

        when(competitionRepository.save(any(Competition.class))).thenReturn(competition);
        when(mapper.toResponse(any(Competition.class))).thenReturn(getCompetitionResponse());

        competitionService.changeStatus(1L, CompetitionStatus.ENROLLMENT);

        assertEquals(CompetitionStatus.ENROLLMENT, competition.getCompetitionStatus());
        verify(competitionRepository).save(competition);
    }

    @Test
    void changeStatus_draftToPublished_directly_shouldThrowInvalidTransition() {
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));

        doThrow(new CompetitionHierarchyValidationException("Invalid status transition from DRAFT to PUBLISHED"))
            .when(validator).validateCompetitionStatusTransition(CompetitionStatus.DRAFT, CompetitionStatus.PUBLISHED);

        CompetitionHierarchyValidationException exception = assertThrows(
            CompetitionHierarchyValidationException.class,
            () -> competitionService.changeStatus(1L, CompetitionStatus.PUBLISHED));

        assertTrue(exception.getMessage().contains("Invalid status transition"));
        verify(competitionRepository, never()).save(any());
    }

    @Test
    void changeStatus_enrollmentToPublished_withValidHierarchy_shouldSucceed() {
        competition.setCompetitionStatus(CompetitionStatus.ENROLLMENT);
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
        when(stageRepository.existsByCompetitionId(1L)).thenReturn(true);
        when(stageRepository.countStagesWithoutTours(1L)).thenReturn(0L);
        when(competitionRepository.save(any(Competition.class))).thenReturn(competition);
        when(mapper.toResponse(any(Competition.class))).thenReturn(getCompetitionResponse());

        competitionService.changeStatus(1L, CompetitionStatus.PUBLISHED);

        assertEquals(CompetitionStatus.PUBLISHED, competition.getCompetitionStatus());
        verify(competitionRepository).save(competition);
    }

    @Test
    void changeStatus_enrollmentToPublished_withNoStages_shouldThrowException() {
        competition.setCompetitionStatus(CompetitionStatus.ENROLLMENT);
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
        when(stageRepository.existsByCompetitionId(1L)).thenReturn(false);

        CompetitionHierarchyValidationException exception = assertThrows(
            CompetitionHierarchyValidationException.class,
            () -> competitionService.changeStatus(1L, CompetitionStatus.PUBLISHED));

        assertTrue(exception.getMessage().contains("must have at least one stage"));
        verify(competitionRepository, never()).save(any());
    }

    @Test
    void changeStatus_enrollmentToPublished_withEmptyStages_shouldThrowException() {
        competition.setCompetitionStatus(CompetitionStatus.ENROLLMENT);
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
        when(stageRepository.existsByCompetitionId(1L)).thenReturn(true);
        when(stageRepository.countStagesWithoutTours(1L)).thenReturn(2L);

        CompetitionHierarchyValidationException exception = assertThrows(
            CompetitionHierarchyValidationException.class,
            () -> competitionService.changeStatus(1L, CompetitionStatus.PUBLISHED));

        assertTrue(exception.getMessage().contains("All stages must have at least one tour"));
        verify(competitionRepository, never()).save(any());
    }

    @Test
    void changeStatus_enrollmentToDraft_shouldThrowInvalidTransition() {
        competition.setCompetitionStatus(CompetitionStatus.ENROLLMENT);
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));

        doThrow(new CompetitionHierarchyValidationException("Invalid status transition from ENROLLMENT to DRAFT"))
            .when(validator).validateCompetitionStatusTransition(CompetitionStatus.ENROLLMENT, CompetitionStatus.DRAFT);

        assertThrows(CompetitionHierarchyValidationException.class,
            () -> competitionService.changeStatus(1L, CompetitionStatus.DRAFT));

        verify(competitionRepository, never()).save(any());
    }

    @Test
    void changeStatus_invalidTransition_publishedToDraft_shouldThrowException() {
        // Arrange
        competition.setCompetitionStatus(CompetitionStatus.PUBLISHED);
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));

        doThrow(new CompetitionHierarchyValidationException("Invalid status transition from PUBLISHED to DRAFT"))
            .when(validator).validateCompetitionStatusTransition(CompetitionStatus.PUBLISHED, CompetitionStatus.DRAFT);

        CompetitionHierarchyValidationException exception = assertThrows(
            CompetitionHierarchyValidationException.class,
            () -> competitionService.changeStatus(1L, CompetitionStatus.DRAFT));

        assertTrue(exception.getMessage().contains("Invalid status transition"));
        verify(competitionRepository, never()).save(any());
    }

    @Test
    void changeStatus_publishedToFinished_withAllStagesCompleted_shouldSucceed() {
        competition.setCompetitionStatus(CompetitionStatus.PUBLISHED);
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
        when(competitionRepository.save(any(Competition.class))).thenReturn(competition);
        when(mapper.toResponse(any(Competition.class))).thenReturn(getCompetitionResponse());

        competitionService.changeStatus(1L, CompetitionStatus.FINISHED);

        assertEquals(CompetitionStatus.FINISHED, competition.getCompetitionStatus());
        verify(validator).validateAllStagesCompletedForCompetition(1L);
        verify(competitionRepository).save(competition);
    }

    @Test
    void changeStatus_publishedToFinished_withIncompleteStage_shouldThrowException() {
        competition.setCompetitionStatus(CompetitionStatus.PUBLISHED);
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));

        doThrow(new CompetitionHierarchyValidationException(
            "Cannot finish competition: Not all stages are completed. "
                + "Incomplete stages: 'Stage 1' (Status: SCHEDULED)"))
            .when(validator).validateAllStagesCompletedForCompetition(1L);

        CompetitionHierarchyValidationException exception = assertThrows(
            CompetitionHierarchyValidationException.class,
            () -> competitionService.changeStatus(1L, CompetitionStatus.FINISHED));

        assertTrue(exception.getMessage().contains("Not all stages are completed"));
        verify(competitionRepository, never()).save(any());
    }

    // ---- create ----

    @Test
    void create_validRequest_shouldSetDraftStatusAndSave() {
        // Arrange
        CreateCompetitionRequest request = new CreateCompetitionRequest(
            "New Comp", "Desc", ZonedDateTime.now(), ZonedDateTime.now().plusDays(5));

        Competition mappedEntity = new Competition();
        mappedEntity.setTitle("New Comp");

        CompetitionResponse expectedResponse = getCompetitionResponse();

        when(mapper.toEntity(request)).thenReturn(mappedEntity);
        when(competitionRepository.save(any(Competition.class))).thenReturn(mappedEntity);
        when(mapper.toResponse(mappedEntity)).thenReturn(getCompetitionResponse());

        // Act
        CompetitionResponse actualResponse = competitionService.create(request);

        // Assert
        assertNotNull(actualResponse);

        ArgumentCaptor<Competition> captor = ArgumentCaptor.forClass(Competition.class);
        verify(competitionRepository).save(captor.capture());

        assertEquals(CompetitionStatus.DRAFT, captor.getValue().getCompetitionStatus());
    }

    // ---- getVisibleById ----

    @Test
    void getVisibleById_whenValidatorDeniesAccess_shouldPropagateAccessDenied() {
        doThrow(new AccessDeniedException("You do not have permission to view this draft competition"))
            .when(validator).checkVisibilityAccess(1L);

        // Act & Assert
        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
            () -> competitionService.getVisibleById(1L));

        assertTrue(exception.getMessage().contains("permission to view this draft"));
        verify(competitionRepository, never()).findById(any());
    }

    @Test
    void getVisibleById_whenValidatorReportsNotFound_shouldPropagate() {
        doThrow(new CompetitionNotFoundException(99L)).when(validator).checkVisibilityAccess(99L);

        assertThrows(CompetitionNotFoundException.class, () -> competitionService.getVisibleById(99L));
    }

    @Test
    void getVisibleById_whenValidatorAllows_shouldReturnCompetition() {
        // validator.checkVisibilityAccess(1L) does nothing by default (allowed)
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
        when(mapper.toResponse(competition)).thenReturn(getCompetitionResponse());

        // Act
        CompetitionResponse response = competitionService.getVisibleById(1L);

        // Assert
        assertNotNull(response);
        verify(validator).checkVisibilityAccess(1L);
    }

    // ---- getCompetitionTree ----

    @Test
    void getCompetitionTree_withStagesAndTours_shouldGroupToursByStageCorrectly() {
        Stage stage1 = Stage.builder()
            .id(10L).competitionId(1L).title("Stage 1")
            .dateStart(testDateStart()).dateFinish(testDateFinish())
            .sortPosition((short) 1).scope(StageScope.REGIONAL).status(StageStatus.SCHEDULED)
            .build();
        Stage stage2 = Stage.builder()
            .id(20L).competitionId(1L).title("Stage 2")
            .dateStart(testDateStart()).dateFinish(testDateFinish())
            .sortPosition((short) 2).scope(StageScope.REGIONAL).status(StageStatus.SCHEDULED)
            .build();

        Tour tourA = Tour.builder()
            .id(100L).stageId(10L).title("Tour A")
            .dateStart(testDateStart()).dateFinish(testDateFinish())
            .sortPosition((short) 1).location("Room 101").executionStatus(ExecutionStatus.SCHEDULED)
            .build();
        Tour tourB = Tour.builder()
            .id(101L).stageId(10L).title("Tour B")
            .dateStart(testDateStart()).dateFinish(testDateFinish())
            .sortPosition((short) 2).location("Room 102").executionStatus(ExecutionStatus.SCHEDULED)
            .build();

        StageResponse stage1Response = StageResponse.builder().id(10L).title("Stage 1").build();
        StageResponse stage2Response = StageResponse.builder().id(20L).title("Stage 2").build();
        TourResponse tourAResponse = TourResponse.builder().id(100L).title("Tour A").build();
        TourResponse tourBResponse = TourResponse.builder().id(101L).title("Tour B").build();

        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
        when(mapper.toResponse(competition)).thenReturn(getCompetitionResponse());
        when(stageRepository.findAllByCompetitionIdOrderBySortPositionAsc(1L))
            .thenReturn(List.of(stage1, stage2));
        when(tourRepository.findAllByStageIdInOrderBySortPositionAsc(List.of(10L, 20L)))
            .thenReturn(List.of(tourA, tourB));
        when(stageMapper.toResponse(stage1)).thenReturn(stage1Response);
        when(stageMapper.toResponse(stage2)).thenReturn(stage2Response);
        when(tourMapper.toResponse(tourA)).thenReturn(tourAResponse);
        when(tourMapper.toResponse(tourB)).thenReturn(tourBResponse);

        CompetitionTreeResponse tree = competitionService.getCompetitionTree(1L);

        assertNotNull(tree);
        verify(validator).checkVisibilityAccess(1L);

        CompetitionTreeResponse expected = new CompetitionTreeResponse(
            getCompetitionResponse(),
            List.of(
                new StageTreeResponse(stage1Response, List.of(tourAResponse, tourBResponse)),
                new StageTreeResponse(stage2Response, List.of())));
        assertEquals(expected, tree);
    }

    @Test
    void getCompetitionTree_shouldFetchToursOnlyOnceForAllStages() {
        Stage stage1 = Stage.builder()
            .id(10L).competitionId(1L).title("Stage 1")
            .dateStart(testDateStart()).dateFinish(testDateFinish())
            .sortPosition((short) 1).scope(StageScope.CITY).status(StageStatus.SCHEDULED)
            .build();
        Stage stage2 = Stage.builder()
            .id(20L).competitionId(1L).title("Stage 2")
            .dateStart(testDateStart()).dateFinish(testDateFinish())
            .sortPosition((short) 2).scope(StageScope.CITY).status(StageStatus.SCHEDULED)
            .build();

        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
        when(mapper.toResponse(competition)).thenReturn(getCompetitionResponse());
        when(stageRepository.findAllByCompetitionIdOrderBySortPositionAsc(1L))
            .thenReturn(List.of(stage1, stage2));
        when(tourRepository.findAllByStageIdInOrderBySortPositionAsc(any())).thenReturn(List.of());
        when(stageMapper.toResponse(any(Stage.class))).thenReturn(StageResponse.builder().id(1L).title("S").build());

        competitionService.getCompetitionTree(1L);

        verify(tourRepository, times(1)).findAllByStageIdInOrderBySortPositionAsc(List.of(10L, 20L));
    }

    @Test
    void getCompetitionTree_whenNoStages_shouldReturnEmptyStageList() {
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
        when(mapper.toResponse(competition)).thenReturn(getCompetitionResponse());
        when(stageRepository.findAllByCompetitionIdOrderBySortPositionAsc(1L)).thenReturn(List.of());

        CompetitionTreeResponse tree = competitionService.getCompetitionTree(1L);

        assertNotNull(tree);
        verify(validator).checkVisibilityAccess(1L);
        verify(tourRepository, never()).findAllByStageIdInOrderBySortPositionAsc(any());
    }

    @Test
    void getCompetitionTree_whenValidatorDeniesAccess_shouldPropagateAndSkipLookup() {
        doThrow(new AccessDeniedException("denied")).when(validator).checkVisibilityAccess(1L);

        assertThrows(AccessDeniedException.class, () -> competitionService.getCompetitionTree(1L));

        verify(competitionRepository, never()).findById(any());
        verify(stageRepository, never()).findAllByCompetitionIdOrderBySortPositionAsc(anyLong());
    }

    // ---- getAllVisible ----

    @Test
    void getAllVisible_asAdmin_shouldQueryWithoutCallingOrgCheck() {
        Pageable pageable = PageRequest.of(0, 20);
        CompetitionSearchFilter filter = CompetitionSearchFilter.builder().build();

        when(securityFacade.hasRole("ADMIN")).thenReturn(true);

        Page<Competition> competitions = new PageImpl<>(List.of(competition), pageable, 1);
        when(competitionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(competitions);
        when(mapper.toResponse(competition)).thenReturn(getCompetitionResponse());

        Page<CompetitionResponse> result = competitionService.getAllVisible(filter, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(securityFacade).hasRole("ADMIN");
        verify(securityFacade, never()).hasRole("ORG"); // short-circuit ||
        verify(competitionRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getAllVisible_asOrg_shouldUseAdminOrgVisibility() {
        Pageable pageable = PageRequest.of(0, 20);
        CompetitionSearchFilter filter = CompetitionSearchFilter.builder().build();

        when(securityFacade.hasRole("ADMIN")).thenReturn(false);
        when(securityFacade.hasRole("ORG")).thenReturn(true);

        Page<Competition> competitions = new PageImpl<>(List.of(competition), pageable, 1);
        when(competitionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(competitions);
        when(mapper.toResponse(competition)).thenReturn(getCompetitionResponse());

        Page<CompetitionResponse> result = competitionService.getAllVisible(filter, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(securityFacade).hasRole("ADMIN");
        verify(securityFacade).hasRole("ORG");
    }

    @Test
    void getAllVisible_asPlainUser_shouldUseUserVisibility() {
        Pageable pageable = PageRequest.of(0, 20);
        CompetitionSearchFilter filter = CompetitionSearchFilter.builder().build();

        when(securityFacade.hasRole("ADMIN")).thenReturn(false);
        when(securityFacade.hasRole("ORG")).thenReturn(false);

        Page<Competition> emptyPage = Page.empty(pageable);
        when(competitionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

        Page<CompetitionResponse> result = competitionService.getAllVisible(filter, pageable);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(mapper, never()).toResponse(any());
    }

    // ---- getArchived ----

    @Test
    void getArchived_whenCompetitionsExist_shouldReturnMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        CompetitionSearchFilter filter = CompetitionSearchFilter.builder().build();

        Competition secondCompetition = Competition.builder()
            .id(2L)
            .title("Archived Competition")
            .competitionStatus(CompetitionStatus.ARCHIVED)
            .build();

        CompetitionResponse firstResponse = getCompetitionResponse();
        CompetitionResponse secondResponse = new CompetitionResponse(
            2L,
            "Archived Competition",
            "Description",
            ZonedDateTime.now(),
            ZonedDateTime.now().plusDays(5),
            CompetitionStatus.ARCHIVED,
            100L,
            100L);

        Page<Competition> competitions = new PageImpl<>(List.of(competition, secondCompetition), pageable, 2);

        when(competitionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(competitions);
        when(mapper.toResponse(competition)).thenReturn(firstResponse);
        when(mapper.toResponse(secondCompetition)).thenReturn(secondResponse);

        Page<CompetitionResponse> result = competitionService.getArchived(filter, pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(firstResponse, result.getContent().get(0));
        assertEquals(secondResponse, result.getContent().get(1));

        verify(competitionRepository).findAll(any(Specification.class), eq(pageable));
        verify(mapper).toResponse(competition);
        verify(mapper).toResponse(secondCompetition);
    }

    @Test
    void getArchived_whenNoCompetitions_shouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        CompetitionSearchFilter filter = CompetitionSearchFilter.builder().build();

        Page<Competition> emptyPage = Page.empty(pageable);
        when(competitionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

        Page<CompetitionResponse> result = competitionService.getArchived(filter, pageable);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(competitionRepository).findAll(any(Specification.class), eq(pageable));
        verify(mapper, never()).toResponse(any());
    }

    private static CompetitionResponse getCompetitionResponse() {
        final ZonedDateTime testDateStart = ZonedDateTime.of(2026, 6, 25, 10, 0, 0, 0, ZoneId.of("UTC"));
        return new CompetitionResponse(
            1L,
            "Всеукраїнська Олімпіада 2026",
            "Опис тестової олімпіади",
            testDateStart,
            testDateStart.plusDays(10),
            CompetitionStatus.DRAFT,
            100L,
            100L);
    }

    private static ZonedDateTime testDateStart() {
        return ZonedDateTime.of(2026, 6, 25, 10, 0, 0, 0, ZoneId.of("UTC"));
    }

    private static ZonedDateTime testDateFinish() {
        return testDateStart().plusDays(10);
    }
}