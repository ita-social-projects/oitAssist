package com.itasocialacademy.oitassist.competition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.competition.api.dto.CompetitionDetail;
import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import com.itasocialacademy.oitassist.competition.dao.model.CompetitionTourView;
import com.itasocialacademy.oitassist.competition.dao.repository.CompetitionRepository;
import com.itasocialacademy.oitassist.competition.dao.repository.TourRepository;
import com.itasocialacademy.oitassist.competition.mapper.CompetitionMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CompetitionFacadeImplTest {

    private static final Set<CompetitionStatus> STATUSES =
        Set.of(CompetitionStatus.FINISHED, CompetitionStatus.ARCHIVED);
    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @Mock
    private CompetitionRepository competitionRepository;

    @Mock
    private CompetitionMapper competitionMapper;

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

    @Test
    void findCompetitionsByIdsAndStatuses_matchingCompetitions_shouldReturnMappedPage() {
        Competition competition = Competition.builder()
            .competitionStatus(CompetitionStatus.FINISHED)
            .build();
        CompetitionDetail detail = CompetitionDetail.builder()
            .id(1L)
            .competitionStatus(CompetitionStatus.FINISHED)
            .build();
        when(competitionRepository.findByIdInAndCompetitionStatusIn(List.of(1L), STATUSES, PAGEABLE))
            .thenReturn(new PageImpl<>(List.of(competition), PAGEABLE, 1));
        when(competitionMapper.toCompetitionDetail(competition)).thenReturn(detail);

        Page<CompetitionDetail> result =
            competitionFacade.findCompetitionsByIdsAndStatuses(List.of(1L), STATUSES, PAGEABLE);

        assertEquals(List.of(detail), result.getContent());
    }

    @Test
    void findCompetitionsByIdsAndStatuses_emptyCompetitionIds_shouldReturnEmptyPageWithoutQuerying() {
        Page<CompetitionDetail> result =
            competitionFacade.findCompetitionsByIdsAndStatuses(List.of(), STATUSES, PAGEABLE);

        assertTrue(result.isEmpty());
        assertEquals(PAGEABLE, result.getPageable());
        verifyNoInteractions(competitionRepository);
    }

    @Test
    void findCompetitionsByIdsAndStatuses_emptyStatuses_shouldReturnEmptyPageWithoutQuerying() {
        Page<CompetitionDetail> result =
            competitionFacade.findCompetitionsByIdsAndStatuses(List.of(1L), Set.of(), PAGEABLE);

        assertTrue(result.isEmpty());
        assertEquals(PAGEABLE, result.getPageable());
        verifyNoInteractions(competitionRepository);
    }

    @Test
    void findCompetitionsByIdsAndStatuses_nullCompetitionIds_shouldThrowNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> competitionFacade.findCompetitionsByIdsAndStatuses(null, STATUSES, PAGEABLE));
        verifyNoInteractions(competitionRepository);
    }

    @Test
    void findCompetitionsByIdsAndStatuses_nullStatuses_shouldThrowNullPointerException() {
        assertThrows(NullPointerException.class,
            () -> competitionFacade.findCompetitionsByIdsAndStatuses(List.of(1L), null, PAGEABLE));
        verifyNoInteractions(competitionRepository);
    }

    private static CompetitionTourView view(Long competitionId, Long tourId) {
        CompetitionTourView view = mock(CompetitionTourView.class);
        when(view.getCompetitionId()).thenReturn(competitionId);
        when(view.getTourId()).thenReturn(tourId);
        return view;
    }
}