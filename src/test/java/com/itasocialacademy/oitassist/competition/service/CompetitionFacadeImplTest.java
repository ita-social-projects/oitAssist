package com.itasocialacademy.oitassist.competition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.competition.api.dto.CompetitionDetail;
import com.itasocialacademy.oitassist.competition.api.dto.StageDetail;
import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import com.itasocialacademy.oitassist.competition.dao.model.CompetitionTourView;
import com.itasocialacademy.oitassist.competition.dao.model.Stage;
import com.itasocialacademy.oitassist.competition.dao.repository.CompetitionRepository;
import com.itasocialacademy.oitassist.competition.dao.repository.StageRepository;
import com.itasocialacademy.oitassist.competition.dao.repository.TourRepository;
import com.itasocialacademy.oitassist.competition.mapper.CompetitionMapper;
import com.itasocialacademy.oitassist.competition.mapper.StageMapper;
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

    @Mock
    private StageRepository stageRepository;

    @Mock
    private StageMapper stageMapper;

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

    @Test
    void findCompetitionsByIds_existingIds_shouldReturnMappedDetails() {
        Competition first = Competition.builder().id(1L).build();
        Competition second = Competition.builder().id(2L).build();
        CompetitionDetail firstDetail = CompetitionDetail.builder().id(1L).build();
        CompetitionDetail secondDetail = CompetitionDetail.builder().id(2L).build();
        when(competitionRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(first, second));
        when(competitionMapper.toCompetitionDetail(first)).thenReturn(firstDetail);
        when(competitionMapper.toCompetitionDetail(second)).thenReturn(secondDetail);

        List<CompetitionDetail> result = competitionFacade.findCompetitionsByIds(List.of(1L, 2L));

        assertEquals(List.of(firstDetail, secondDetail), result);
    }

    @Test
    void findCompetitionsByIds_emptyIds_shouldReturnEmptyListWithoutQuerying() {
        List<CompetitionDetail> result = competitionFacade.findCompetitionsByIds(List.of());

        assertTrue(result.isEmpty());
        verifyNoInteractions(competitionRepository, competitionMapper);
    }

    @Test
    void findCompetitionsByIds_nullIds_shouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> competitionFacade.findCompetitionsByIds(null));
        verifyNoInteractions(competitionRepository, competitionMapper);
    }

    @Test
    void findStagesByIds_existingIds_shouldReturnMappedDetails() {
        Stage first = Stage.builder().id(1L).build();
        Stage second = Stage.builder().id(2L).build();
        StageDetail firstDetail = StageDetail.builder().id(1L).build();
        StageDetail secondDetail = StageDetail.builder().id(2L).build();
        when(stageRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(first, second));
        when(stageMapper.toStageDetail(first)).thenReturn(firstDetail);
        when(stageMapper.toStageDetail(second)).thenReturn(secondDetail);

        List<StageDetail> result = competitionFacade.findStagesByIds(List.of(1L, 2L));

        assertEquals(List.of(firstDetail, secondDetail), result);
    }

    @Test
    void findStagesByIds_emptyIds_shouldReturnEmptyListWithoutQuerying() {
        List<StageDetail> result = competitionFacade.findStagesByIds(List.of());

        assertTrue(result.isEmpty());
        verifyNoInteractions(stageRepository, stageMapper);
    }

    @Test
    void findStagesByIds_nullIds_shouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> competitionFacade.findStagesByIds(null));
        verifyNoInteractions(stageRepository, stageMapper);
    }

    private static CompetitionTourView view(Long competitionId, Long tourId) {
        CompetitionTourView view = mock(CompetitionTourView.class);
        when(view.getCompetitionId()).thenReturn(competitionId);
        when(view.getTourId()).thenReturn(tourId);
        return view;
    }
}