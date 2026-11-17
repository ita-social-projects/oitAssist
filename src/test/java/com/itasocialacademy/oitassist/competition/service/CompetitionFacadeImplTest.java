package com.itasocialacademy.oitassist.competition.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.competition.api.dto.CompetitionDetail;
import com.itasocialacademy.oitassist.competition.api.dto.EnrollmentCompetitionStageDetail;
import com.itasocialacademy.oitassist.competition.api.dto.StageDetail;
import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.dao.enums.StageScope;
import com.itasocialacademy.oitassist.competition.dao.enums.StageStatus;
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
    void findOpenEnrollmentCompetitions_noEnrollmentCompetitions_shouldReturnEmptyList() {
        when(competitionRepository.findAllByCompetitionStatus(CompetitionStatus.ENROLLMENT))
            .thenReturn(List.of());

        List<EnrollmentCompetitionStageDetail> result = competitionFacade.findOpenEnrollmentCompetitions();

        assertTrue(result.isEmpty());
        verifyNoInteractions(stageRepository);
        verifyNoInteractions(competitionMapper);
        verifyNoInteractions(stageMapper);
    }

    @Test
    void findOpenEnrollmentCompetitions_validCompetitions_shouldTakeFirstInitialStageAndMapDetail() {
        Competition competition = Competition.builder()
            .id(1L)
            .competitionStatus(CompetitionStatus.ENROLLMENT)
            .build();
        Stage firstStage = Stage.builder()
            .id(10L)
            .competitionId(1L)
            .sortPosition((short) 1)
            .scope(StageScope.DISTRICT)
            .status(StageStatus.SCHEDULED)
            .build();
        Stage secondStage = Stage.builder()
            .id(11L)
            .competitionId(1L)
            .sortPosition((short) 2)
            .scope(StageScope.CITY)
            .status(StageStatus.SCHEDULED)
            .build();

        CompetitionDetail competitionDetail = CompetitionDetail.builder()
            .id(1L)
            .competitionStatus(CompetitionStatus.ENROLLMENT)
            .build();
        StageDetail stageDetail = StageDetail.builder()
            .id(10L)
            .competitionId(1L)
            .scope(StageScope.DISTRICT)
            .build();

        when(competitionRepository.findAllByCompetitionStatus(CompetitionStatus.ENROLLMENT))
            .thenReturn(List.of(competition));
        when(stageRepository.findAllByCompetitionIdInAndScopeInAndStatusOrderBySortPositionAsc(
            List.of(1L), Set.of(StageScope.DISTRICT, StageScope.CITY), StageStatus.SCHEDULED))
            .thenReturn(List.of(firstStage, secondStage));
        when(competitionMapper.toCompetitionDetail(competition)).thenReturn(competitionDetail);
        when(stageMapper.toStageDetail(firstStage)).thenReturn(stageDetail);

        List<EnrollmentCompetitionStageDetail> result = competitionFacade.findOpenEnrollmentCompetitions();

        assertEquals(1, result.size());
        assertEquals(competitionDetail, result.get(0).competition());
        assertEquals(stageDetail, result.get(0).stage());
        verify(competitionMapper).toCompetitionDetail(competition);
        verify(stageMapper).toStageDetail(firstStage);
        verifyNoInteractions(tourRepository);
    }

    private static CompetitionTourView view(Long competitionId, Long tourId) {
        CompetitionTourView view = mock(CompetitionTourView.class);
        when(view.getCompetitionId()).thenReturn(competitionId);
        when(view.getTourId()).thenReturn(tourId);
        return view;
    }
}