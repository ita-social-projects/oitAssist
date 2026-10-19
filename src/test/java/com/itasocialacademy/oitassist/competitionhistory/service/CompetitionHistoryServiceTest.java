package com.itasocialacademy.oitassist.competitionhistory.service;

import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.competition.api.dto.CompetitionDetail;
import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competitionhistory.dto.response.CompetitionHistoryResponse;
import com.itasocialacademy.oitassist.competitionhistory.mapper.CompetitionHistoryMapper;
import com.itasocialacademy.oitassist.core.exceptions.InsufficientPermissionsException;
import com.itasocialacademy.oitassist.evaluation.api.dto.CompetitionScore;
import com.itasocialacademy.oitassist.evaluation.api.facade.EvaluationFacade;
import com.itasocialacademy.oitassist.participation.api.ParticipationFacade;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
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

import java.util.List;
import java.util.Set;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompetitionHistoryServiceTest {
    private static final Long USER_ID = 4L;
    private static final Set<CompetitionStatus> HISTORY_STATUSES =
        Set.of(CompetitionStatus.FINISHED, CompetitionStatus.ARCHIVED);
    private static final CompetitionScore SCORE = new CompetitionScore(28.5, 45);

    @Mock
    private SecurityFacade securityFacade;
    @Mock
    private ParticipationFacade participationFacade;
    @Mock
    private CompetitionFacade competitionFacade;
    @Mock
    private EvaluationFacade evaluationFacade;
    @Mock
    private CompetitionHistoryMapper mapper;

    @InjectMocks
    private CompetitionHistoryServiceImpl historyService;

    private Pageable pageable;
    private CompetitionDetail competitionDetail;
    private CompetitionHistoryResponse historyResponse;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);
        competitionDetail = CompetitionDetail.builder()
            .id(2L)
            .title("Olympiad")
            .competitionStatus(CompetitionStatus.FINISHED)
            .build();
        historyResponse = CompetitionHistoryResponse.builder()
            .id(2L)
            .title("Olympiad")
            .build();
    }

    @Test
    void getHistory_owner_shouldReturnMappedPage() {
        when(securityFacade.isOwner(USER_ID)).thenReturn(true);
        when(participationFacade.findCompetitionIdsByUserId(USER_ID)).thenReturn(List.of(2L));
        when(competitionFacade.findCompetitionsByIdsAndStatuses(List.of(2L), HISTORY_STATUSES, pageable))
            .thenReturn(new PageImpl<>(List.of(competitionDetail), pageable, 1));
        when(evaluationFacade.getUserScores(USER_ID, List.of(2L))).thenReturn(Map.of(2L, SCORE));
        when(mapper.toResponse(competitionDetail, SCORE)).thenReturn(historyResponse);

        Page<CompetitionHistoryResponse> result = historyService.getHistory(USER_ID, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(historyResponse, result.getContent().getFirst());
        verify(securityFacade, never()).hasRole(any());
    }

    @Test
    void getHistory_adminViewingOtherUser_shouldReturnMappedPage() {
        when(securityFacade.isOwner(USER_ID)).thenReturn(false);
        when(securityFacade.hasRole("ADMIN")).thenReturn(true);
        when(participationFacade.findCompetitionIdsByUserId(USER_ID)).thenReturn(List.of(2L));
        when(competitionFacade.findCompetitionsByIdsAndStatuses(List.of(2L), HISTORY_STATUSES, pageable))
            .thenReturn(new PageImpl<>(List.of(competitionDetail), pageable, 1));
        when(evaluationFacade.getUserScores(USER_ID, List.of(2L))).thenReturn(Map.of(2L, SCORE));
        when(mapper.toResponse(competitionDetail, SCORE)).thenReturn(historyResponse);

        Page<CompetitionHistoryResponse> result = historyService.getHistory(USER_ID, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(historyResponse, result.getContent().getFirst());
    }

    @Test
    void getHistory_notOwnerNotAdmin_shouldThrowInsufficientPermissionsException() {
        when(securityFacade.isOwner(USER_ID)).thenReturn(false);
        when(securityFacade.hasRole("ADMIN")).thenReturn(false);

        assertThrows(InsufficientPermissionsException.class,
            () -> historyService.getHistory(USER_ID, pageable));

        verifyNoInteractions(participationFacade, competitionFacade, evaluationFacade, mapper);
    }

    @Test
    void getHistory_noParticipation_shouldReturnEmptyPage() {
        when(securityFacade.isOwner(USER_ID)).thenReturn(true);
        when(participationFacade.findCompetitionIdsByUserId(USER_ID)).thenReturn(List.of());

        Page<CompetitionHistoryResponse> result = historyService.getHistory(USER_ID, pageable);

        assertTrue(result.isEmpty());
        verifyNoInteractions(competitionFacade, evaluationFacade, mapper);
    }

    @Test
    void getHistory_noFinishedCompetitions_shouldReturnEmptyPage() {
        when(securityFacade.isOwner(USER_ID)).thenReturn(true);
        when(participationFacade.findCompetitionIdsByUserId(USER_ID)).thenReturn(List.of(2L));
        when(competitionFacade.findCompetitionsByIdsAndStatuses(List.of(2L), HISTORY_STATUSES, pageable))
            .thenReturn(Page.empty(pageable));

        Page<CompetitionHistoryResponse> result = historyService.getHistory(USER_ID, pageable);

        assertTrue(result.isEmpty());
        verifyNoInteractions(evaluationFacade, mapper);
    }

    @Test
    void getHistory_severalCompetitions_shouldMatchScoresById() {
        CompetitionDetail otherDetail = CompetitionDetail.builder()
            .id(3L)
            .title("Other")
            .competitionStatus(CompetitionStatus.FINISHED)
            .build();
        CompetitionHistoryResponse otherResponse = CompetitionHistoryResponse.builder()
            .id(3L)
            .title("Other")
            .build();
        when(securityFacade.isOwner(USER_ID)).thenReturn(true);
        when(participationFacade.findCompetitionIdsByUserId(USER_ID)).thenReturn(List.of(2L, 3L));
        when(competitionFacade.findCompetitionsByIdsAndStatuses(List.of(2L, 3L), HISTORY_STATUSES, pageable))
            .thenReturn(new PageImpl<>(List.of(competitionDetail, otherDetail), pageable, 2));
        when(evaluationFacade.getUserScores(USER_ID, List.of(2L, 3L))).thenReturn(Map.of(2L, SCORE));
        when(mapper.toResponse(competitionDetail, SCORE)).thenReturn(historyResponse);
        when(mapper.toResponse(otherDetail, new CompetitionScore(0.0, 0))).thenReturn(otherResponse);

        Page<CompetitionHistoryResponse> result = historyService.getHistory(USER_ID, pageable);

        assertEquals(List.of(historyResponse, otherResponse), result.getContent());
    }
}
