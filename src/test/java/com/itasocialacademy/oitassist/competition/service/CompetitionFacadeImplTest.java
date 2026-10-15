package com.itasocialacademy.oitassist.competition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.competition.dao.model.CompetitionTourView;
import com.itasocialacademy.oitassist.competition.dao.repository.TourRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompetitionFacadeImplTest {

    @Mock
    private TourRepository tourRepository;

    @InjectMocks
    private CompetitionFacadeImpl competitionFacade;

    @Test
    void findTourIdsByCompetitionIds_competitionsWithTours_shouldGroupTourIdsByCompetition() {
        List<CompetitionTourView> views = List.of(view(1L, 100L), view(1L, 101L), view(2L, 102L));
        when(tourRepository.findTourIdsByCompetitionIds(List.of(1L, 2L))).thenReturn(views);

        Map<Long, List<Long>> result = competitionFacade.findTourIdsByCompetitionIds(List.of(1L, 2L));

        assertEquals(Map.of(1L, List.of(100L, 101L), 2L, List.of(102L)), result);
    }

    @Test
    void findTourIdsByCompetitionIds_competitionWithoutTours_shouldOmitItFromResult() {
        List<CompetitionTourView> views = List.of(view(1L, 100L));
        when(tourRepository.findTourIdsByCompetitionIds(List.of(1L, 2L))).thenReturn(views);

        Map<Long, List<Long>> result = competitionFacade.findTourIdsByCompetitionIds(List.of(1L, 2L));

        assertEquals(Map.of(1L, List.of(100L)), result);
    }

    @Test
    void findTourIdsByCompetitionIds_emptyCompetitionIds_shouldReturnEmptyMapWithoutQuerying() {
        Map<Long, List<Long>> result = competitionFacade.findTourIdsByCompetitionIds(List.of());

        assertTrue(result.isEmpty());
        verifyNoInteractions(tourRepository);
    }

    @Test
    void findTourIdsByCompetitionIds_nullCompetitionIds_shouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> competitionFacade.findTourIdsByCompetitionIds(null));
        verifyNoInteractions(tourRepository);
    }

    private static CompetitionTourView view(Long competitionId, Long tourId) {
        CompetitionTourView view = mock(CompetitionTourView.class);
        when(view.getCompetitionId()).thenReturn(competitionId);
        when(view.getTourId()).thenReturn(tourId);
        return view;
    }
}