package com.itasocialacademy.oitassist.participation.service;

import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.competition.api.dto.CompetitionDetail;
import com.itasocialacademy.oitassist.competition.api.dto.StageDetail;
import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.dao.enums.StageScope;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionHierarchyValidationException;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionNotFoundException;
import com.itasocialacademy.oitassist.competition.exceptions.StageNotFoundException;
import com.itasocialacademy.oitassist.participation.dao.dto.response.ParticipationListItemResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.UserSummary;
import com.itasocialacademy.oitassist.participation.dao.model.Participation;
import com.itasocialacademy.oitassist.participation.dao.repository.ParticipationRepository;
import com.itasocialacademy.oitassist.participation.mapper.UserEnrollmentAssembler;
import com.itasocialacademy.oitassist.participation.mapper.interfaces.UserSummaryMapper;
import com.itasocialacademy.oitassist.user.api.dto.UserProfileDetails;
import com.itasocialacademy.oitassist.user.api.interfaces.UserFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParticipationServiceTest {
    @Mock
    private ParticipationRepository participationRepository;
    @Mock
    private CompetitionFacade competitionFacade;
    @Mock
    private UserSummaryMapper userSummaryMapper;
    @Mock
    private UserEnrollmentAssembler enrollmentAssembler;
    @Mock
    private UserFacade userFacade;

    @InjectMocks
    private ParticipationServiceImpl participationService;

    private CompetitionDetail competitionDetail;
    private StageDetail stageDetail;

    @BeforeEach
    void setUp() {
        competitionDetail = CompetitionDetail.builder()
            .id(2L)
            .competitionStatus(CompetitionStatus.ENROLLMENT)
            .build();

        stageDetail = StageDetail.builder()
            .id(3L)
            .competitionId(2L)
            .scope(StageScope.DISTRICT)
            .build();
    }

    @Test
    void getParticipationList_noCandidates_shouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);

        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(stageDetail));
        when(participationRepository.findAll(any(Specification.class))).thenReturn(List.of());

        Page<ParticipationListItemResponse> result = participationService.getParticipationList(2L, 3L, null, pageable);

        assertTrue(result.isEmpty());
        verify(userFacade, never()).findUserIdsBySearchWithinIds(any(), any());
        verify(participationRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getParticipationList_noSearch_shouldReturnAllCandidates() {
        Pageable pageable = PageRequest.of(0, 10);

        Participation candidateParticipation = new Participation();
        candidateParticipation.setId(1L);
        candidateParticipation.setUserId(4L);
        candidateParticipation.setCompetitionId(2L);
        candidateParticipation.setStageId(3L);

        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(stageDetail));
        when(participationRepository.findAll(any(Specification.class))).thenReturn(List.of(candidateParticipation));
        when(userFacade.findUserIdsBySearchWithinIds(null, List.of(4L))).thenReturn(Optional.empty());

        Page<Participation> participationPage = new PageImpl<>(List.of(candidateParticipation), pageable, 1);
        when(participationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(participationPage);

        UserProfileDetails user = new UserProfileDetails(4L, "Test", "Test Surname", "test@mail.com");
        when(userSummaryMapper.toUserSummary(user))
            .thenReturn(new UserSummary("Test", "Test Surname", "test@mail.com"));
        when(enrollmentAssembler.enrichWithUser(any(), any(), any())).thenAnswer(invocation -> {
            List<Participation> apps = invocation.getArgument(0);
            BiFunction<Participation, UserProfileDetails, ParticipationListItemResponse> combiner =
                invocation.getArgument(2);
            return apps.stream()
                .map(app -> combiner.apply(app, user))
                .toList();
        });
        Page<ParticipationListItemResponse> result = participationService.getParticipationList(2L, 3L, null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().getFirst().participationId());
        assertEquals(4L, result.getContent().getFirst().studentId());
    }

    @Test
    void getParticipationList_searchMatchesNoOne_shouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);

        Participation candidateParticipation = new Participation();
        candidateParticipation.setUserId(4L);

        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(stageDetail));
        when(participationRepository.findAll(any(Specification.class))).thenReturn(List.of(candidateParticipation));
        when(userFacade.findUserIdsBySearchWithinIds("xyz", List.of(4L))).thenReturn(Optional.of(List.of()));

        Page<ParticipationListItemResponse> result = participationService.getParticipationList(2L, 3L, "xyz", pageable);

        assertTrue(result.isEmpty());
        verify(participationRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getParticipationList_stageDoesNotBelongToCompetition_shouldThrowCompetitionHierarchyValidationException() {
        Pageable pageable = PageRequest.of(0, 10);

        StageDetail mismatchedStage = StageDetail.builder()
            .id(3L)
            .competitionId(99L)
            .scope(StageScope.DISTRICT)
            .build();

        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(mismatchedStage));

        CompetitionHierarchyValidationException exception = assertThrows(
            CompetitionHierarchyValidationException.class,
            () -> participationService.getParticipationList(2L, 3L, null, pageable));

        assertTrue(exception.getMessage().contains("does not belong to this competition"));
        verify(participationRepository, never()).findAll(any(Specification.class));
    }

    @Test
    void getParticipationList_searchMatchesSubset_shouldFilterByMatchingIds() {
        Pageable pageable = PageRequest.of(0, 10);

        Participation candidateA = new Participation();
        candidateA.setUserId(4L);
        Participation candidateB = new Participation();
        candidateB.setUserId(5L);

        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.of(stageDetail));
        when(participationRepository.findAll(any(Specification.class))).thenReturn(List.of(candidateA, candidateB));
        when(userFacade.findUserIdsBySearchWithinIds("test", List.of(4L, 5L))).thenReturn(Optional.of(List.of(4L)));

        Participation matched = new Participation();
        matched.setId(9L);
        matched.setUserId(4L);

        Page<Participation> participationPage = new PageImpl<>(List.of(matched), pageable, 1);
        when(participationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(participationPage);

        UserProfileDetails user = new UserProfileDetails(4L, "Test", "Surname", "test@mail.com");
        when(enrollmentAssembler.enrichWithUser(any(), any(), any())).thenAnswer(invocation -> {
            List<Participation> items = invocation.getArgument(0);
            BiFunction<Participation, UserProfileDetails, ParticipationListItemResponse> combiner =
                invocation.getArgument(2);
            return items.stream().map(p -> combiner.apply(p, user)).toList();
        });

        Page<ParticipationListItemResponse> result =
            participationService.getParticipationList(2L, 3L, "test", pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(9L, result.getContent().getFirst().participationId());
    }

    @Test
    void getParticipationList_competitionNotFound_shouldThrowCompetitionNotFoundException() {
        Pageable pageable = PageRequest.of(0, 10);

        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.empty());

        assertThrows(CompetitionNotFoundException.class,
            () -> participationService.getParticipationList(2L, 3L, null, pageable));

        verify(participationRepository, never()).findAll(any(Specification.class));
    }

    @Test
    void getParticipationList_stageNotFound_shouldThrowStageNotFoundException() {
        Pageable pageable = PageRequest.of(0, 10);

        when(competitionFacade.findCompetitionById(2L)).thenReturn(Optional.of(competitionDetail));
        when(competitionFacade.findStageById(3L)).thenReturn(Optional.empty());

        assertThrows(StageNotFoundException.class,
            () -> participationService.getParticipationList(2L, 3L, null, pageable));

        verify(participationRepository, never()).findAll(any(Specification.class));
    }
}
