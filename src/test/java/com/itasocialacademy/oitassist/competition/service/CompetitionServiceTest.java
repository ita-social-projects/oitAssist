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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import com.itasocialacademy.oitassist.competition.dao.repository.CompetitionRepository;
import com.itasocialacademy.oitassist.competition.dao.repository.StageRepository;
import com.itasocialacademy.oitassist.competition.dao.repository.TourRepository;
import com.itasocialacademy.oitassist.competition.dao.specification.CompetitionSpecification;
import com.itasocialacademy.oitassist.competition.dto.request.CreateCompetitionRequest;
import com.itasocialacademy.oitassist.competition.dto.response.CompetitionResponse;
import com.itasocialacademy.oitassist.competition.dto.response.CompetitionTreeResponse;
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
    void changeStatus_draftToPublished_withValidHierarchy_shouldSucceed() {
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
    void changeStatus_draftToPublished_withNoStages_shouldThrowException() {
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
        when(stageRepository.existsByCompetitionId(1L)).thenReturn(false);

        CompetitionHierarchyValidationException exception = assertThrows(
            CompetitionHierarchyValidationException.class,
            () -> competitionService.changeStatus(1L, CompetitionStatus.PUBLISHED));

        assertTrue(exception.getMessage().contains("must have at least one stage"));
        verify(competitionRepository, never()).save(any());
    }

    @Test
    void changeStatus_draftToPublished_withEmptyStages_shouldThrowException() {
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
    void changeStatus_invalidTransition_publishedToDraft_shouldThrowException() {
        competition.setCompetitionStatus(CompetitionStatus.PUBLISHED);
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));

        CompetitionHierarchyValidationException exception = assertThrows(
            CompetitionHierarchyValidationException.class,
            () -> competitionService.changeStatus(1L, CompetitionStatus.DRAFT));

        assertTrue(exception.getMessage().contains("Invalid status transition"));
        verify(competitionRepository, never()).save(any());
    }

    // ---- create ----

    @Test
    void create_validRequest_shouldSetDraftStatusAndSave() {
        CreateCompetitionRequest request = new CreateCompetitionRequest(
            "New Comp", "Desc", ZonedDateTime.now(), ZonedDateTime.now().plusDays(5));

        Competition mappedEntity = new Competition();
        mappedEntity.setTitle("New Comp");

        when(mapper.toEntity(request)).thenReturn(mappedEntity);
        when(competitionRepository.save(any(Competition.class))).thenReturn(mappedEntity);
        when(mapper.toResponse(mappedEntity)).thenReturn(getCompetitionResponse());

        CompetitionResponse actualResponse = competitionService.create(request);

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

        CompetitionResponse response = competitionService.getVisibleById(1L);

        assertNotNull(response);
        verify(validator).checkVisibilityAccess(1L);
    }

    // ---- getCompetitionTree ----

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

    // ---- getArchived ----

    @Test
    void getArchived_whenCompetitionsExist_shouldReturnMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);

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

        Page<CompetitionResponse> result = competitionService.getArchived(pageable);

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

        Page<Competition> emptyPage = Page.empty(pageable);

        when(competitionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

        Page<CompetitionResponse> result = competitionService.getArchived(pageable);

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
}