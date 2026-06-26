package com.itasocialacademy.oitassist.competition.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.competition.dao.dto.request.CreateCompetitionRequest;
import com.itasocialacademy.oitassist.competition.dao.dto.response.CompetitionResponse;
import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import com.itasocialacademy.oitassist.competition.dao.repository.CompetitionRepository;
import com.itasocialacademy.oitassist.competition.dao.repository.StageRepository;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionHierarchyValidationException;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionNotFoundException;
import com.itasocialacademy.oitassist.competition.mapper.CompetitionMapper;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class CompetitionServiceTest {

    @Mock
    private CompetitionRepository competitionRepository;
    @Mock
    private StageRepository stageRepository;
    @Mock
    private CompetitionMapper mapper;
    @Mock
    private SecurityFacade securityFacade;

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

    @Test
    void changeStatus_draftToPublished_withValidHierarchy_shouldSucceed() {
        // Arrange
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
        when(stageRepository.existsByCompetitionId(1L)).thenReturn(true);
        when(stageRepository.countStagesWithoutTours(1L)).thenReturn(0L);
        when(competitionRepository.save(any(Competition.class))).thenReturn(competition);
        when(mapper.toResponse(any(Competition.class))).thenReturn(getCompetitionResponse());

        // Act
        competitionService.changeStatus(1L, CompetitionStatus.PUBLISHED);

        // Assert
        assertEquals(CompetitionStatus.PUBLISHED, competition.getCompetitionStatus());
        verify(competitionRepository).save(competition);
    }

    @Test
    void changeStatus_draftToPublished_withNoStages_shouldThrowException() {
        // Arrange
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
        when(stageRepository.existsByCompetitionId(1L)).thenReturn(false);

        // Act & Assert
        CompetitionHierarchyValidationException exception = assertThrows(CompetitionHierarchyValidationException.class,
            () -> competitionService.changeStatus(1L, CompetitionStatus.PUBLISHED));

        assertTrue(exception.getMessage().contains("must have at least one stage"));
        verify(competitionRepository, never()).save(any());
    }

    @Test
    void changeStatus_draftToPublished_withEmptyStages_shouldThrowException() {
        // Arrange
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
        when(stageRepository.existsByCompetitionId(1L)).thenReturn(true);
        when(stageRepository.countStagesWithoutTours(1L)).thenReturn(2L);

        // Act & Assert
        CompetitionHierarchyValidationException exception = assertThrows(CompetitionHierarchyValidationException.class,
            () -> competitionService.changeStatus(1L, CompetitionStatus.PUBLISHED));

        assertTrue(exception.getMessage().contains("All stages must have at least one tour"));
        verify(competitionRepository, never()).save(any());
    }

    @Test
    void changeStatus_invalidTransition_publishedToDraft_shouldThrowException() {
        // Arrange
        competition.setCompetitionStatus(CompetitionStatus.PUBLISHED);
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));

        // Act & Assert
        CompetitionHierarchyValidationException exception = assertThrows(CompetitionHierarchyValidationException.class,
            () -> competitionService.changeStatus(1L, CompetitionStatus.DRAFT));

        assertTrue(exception.getMessage().contains("Invalid status transition"));
        verify(competitionRepository, never()).save(any());
    }

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
        when(mapper.toResponse(mappedEntity)).thenReturn(expectedResponse);

        // Act
        CompetitionResponse actualResponse = competitionService.create(request);

        // Assert
        assertNotNull(actualResponse);

        ArgumentCaptor<Competition> captor = ArgumentCaptor.forClass(Competition.class);
        verify(competitionRepository).save(captor.capture());

        assertEquals(CompetitionStatus.DRAFT, captor.getValue().getCompetitionStatus());
    }

    @Test
    void getVisibleById_whenCompetitionNotFound_shouldThrowEntityNotFoundException() {
        // Arrange
        when(competitionRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CompetitionNotFoundException.class, () -> competitionService.getVisibleById(99L));
    }

    @Test
    void getVisibleById_draftCompetition_whenUserIsAdmin_shouldReturnCompetition() {
        // Arrange
        competition.setCompetitionStatus(CompetitionStatus.DRAFT);
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(mapper.toResponse(competition)).thenReturn(getCompetitionResponse());

        // Act
        CompetitionResponse response = competitionService.getVisibleById(1L);

        // Assert
        assertNotNull(response);
    }

    @Test
    void getVisibleById_draftCompetition_whenUserIsOwnerOrg_shouldReturnCompetition() {
        // Arrange
        competition.setCompetitionStatus(CompetitionStatus.DRAFT);
        competition.setCreatedBy(5L);

        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
        when(securityFacade.hasRole("ADMIN")).thenReturn(false);
        when(securityFacade.hasRole("ORG")).thenReturn(true);
        when(mapper.toResponse(competition)).thenReturn(getCompetitionResponse());

        // Act
        CompetitionResponse response = competitionService.getVisibleById(1L);

        // Assert
        assertNotNull(response);
    }

    @Test
    void getVisibleById_draftCompetition_whenUserIsRegular_shouldThrowAccessDenied() {
        // Arrange
        competition.setCompetitionStatus(CompetitionStatus.DRAFT);
        competition.setCreatedBy(5L);

        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
        when(securityFacade.hasRole("ADMIN")).thenReturn(false);
        when(securityFacade.hasRole("ORG")).thenReturn(false);

        // Act & Assert
        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
            () -> competitionService.getVisibleById(1L));

        assertTrue(exception.getMessage().contains("permission to view this draft"));
        verify(mapper, never()).toResponse(any());
    }

    @Test
    void getVisibleById_publishedCompetition_whenUserIsRegular_shouldReturnCompetition() {
        // Arrange
        competition.setCompetitionStatus(CompetitionStatus.PUBLISHED);
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
        when(mapper.toResponse(competition)).thenReturn(getCompetitionResponse());

        // Act
        CompetitionResponse response = competitionService.getVisibleById(1L);

        // Assert
        assertNotNull(response);
    }

    @Test
    void validateHierarchyImmutability_whenCompetitionNotFound_shouldThrowEntityNotFoundException() {
        // Arrange
        when(competitionRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CompetitionNotFoundException.class,
            () -> competitionService.validateHierarchyImmutability(99L));
    }

    @Test
    void validateHierarchyImmutability_whenArchived_shouldThrowHierarchyValidationException() {
        // Arrange
        competition.setCompetitionStatus(CompetitionStatus.ARCHIVED);
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));

        // Act & Assert
        CompetitionHierarchyValidationException exception = assertThrows(CompetitionHierarchyValidationException.class,
            () -> competitionService.validateHierarchyImmutability(1L));

        assertTrue(exception.getMessage().contains("is ARCHIVED (read-only)"));
    }

    @Test
    void validateHierarchyImmutability_whenDraft_shouldPassWithoutExceptions() {
        // Arrange
        competition.setCompetitionStatus(CompetitionStatus.DRAFT);
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));

        // Act & Assert
        assertDoesNotThrow(() -> competitionService.validateHierarchyImmutability(1L));
    }

    @Test
    void validateHierarchyImmutability_whenPublished_shouldPassWithoutExceptions() {
        // Arrange
        competition.setCompetitionStatus(CompetitionStatus.PUBLISHED);
        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));

        // Act & Assert
        assertDoesNotThrow(() -> competitionService.validateHierarchyImmutability(1L));
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