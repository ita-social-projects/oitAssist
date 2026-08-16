package com.itasocialacademy.oitassist.evaluation.service;

import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.competition.api.dto.CompetitionDetail;
import com.itasocialacademy.oitassist.competition.api.dto.CompetitionTreeDetail;
import com.itasocialacademy.oitassist.competition.api.dto.StageDetail;
import com.itasocialacademy.oitassist.competition.api.dto.StageTreeDetail;
import com.itasocialacademy.oitassist.competition.api.dto.TourDetail;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionNotFoundException;
import com.itasocialacademy.oitassist.evaluation.api.dto.OlympiadResults;
import com.itasocialacademy.oitassist.evaluation.api.dto.TourResult;
import com.itasocialacademy.oitassist.evaluation.dao.dto.request.ParticipantTaskScore;
import com.itasocialacademy.oitassist.evaluation.dao.repository.StubScoreRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResultsServiceImplTest {
    private static final Long COMPETITION_ID = 1L;

    private static final StageDetail STAGE = StageDetail.builder()
        .id(1L).title("Регіональний етап").sortPosition((short) 1).build();

    private static final TourDetail TOUR_1 = TourDetail.builder()
        .id(1L).stageId(1L).title("Тур 1").sortPosition((short) 1).build();

    private static final TourDetail TOUR_2 = TourDetail.builder()
        .id(2L).stageId(1L).title("Тур 2").sortPosition((short) 2).build();

    @Mock
    private StubScoreRepository scoreRepository;

    @Mock
    private CompetitionFacade competitionFacade;

    private ResultsServiceImpl resultsService;

    @BeforeEach
    void setUp() {
        resultsService = new ResultsServiceImpl(scoreRepository, new ResultsAggregator(), competitionFacade);
    }

    private void mockCompetitionWithScores() {
        CompetitionTreeDetail tree = CompetitionTreeDetail.builder()
            .competition(CompetitionDetail.builder().id(COMPETITION_ID).title("Олімпіада з Java").build())
            .stages(List.of(StageTreeDetail.builder()
                .stage(STAGE)
                .tours(List.of(TOUR_1, TOUR_2))
                .build()))
            .build();

        when(competitionFacade.findCompetitionTreeByCompetitionId(COMPETITION_ID))
            .thenReturn(Optional.of(tree));

        when(scoreRepository.findScores(any(), any())).thenReturn(List.of(
            new ParticipantTaskScore(1L, "Мельник Ігор Васильович", 1L, 10L, 5),
            new ParticipantTaskScore(1L, "Мельник Ігор Васильович", 2L, 11L, 7),
            new ParticipantTaskScore(2L, "Чорновіл Каріна Олегівна", 1L, 12L, 4)));
    }

    @Test
    void getResults_ShouldIncludeAllTours_WhenNoScopeSelected() {
        mockCompetitionWithScores();

        OlympiadResults results = resultsService.getResults(COMPETITION_ID, Set.of(), Set.of(), null);

        assertEquals(2, results.participants().getFirst().stages().getFirst().tours().size());
        assertEquals("Олімпіада з Java", results.olympiadTitle());
        assertEquals("", results.scopeTitle());
    }

    @Test
    void getResults_ShouldKeepOnlySelectedTour_WhenTourIdsProvided() {
        mockCompetitionWithScores();

        OlympiadResults results = resultsService.getResults(COMPETITION_ID, Set.of(), Set.of(2L), null);

        List<TourResult> tours = results.participants().getFirst().stages().getFirst().tours();
        assertEquals(1, tours.size());
        assertEquals("Тур 2", tours.getFirst().tourTitle());
    }

    @Test
    void getResults_ShouldIgnoreStageIds_WhenTourIdsAlsoProvided() {
        mockCompetitionWithScores();

        OlympiadResults results = resultsService.getResults(COMPETITION_ID, Set.of(1L), Set.of(2L), null);

        List<TourResult> tours = results.participants().getFirst().stages().getFirst().tours();
        assertEquals(1, tours.size());
        assertEquals("Тур 2", tours.getFirst().tourTitle());
    }

    @Test
    void getResults_ShouldFilterParticipants_WhenSearchProvided() {
        mockCompetitionWithScores();

        OlympiadResults results = resultsService.getResults(COMPETITION_ID, Set.of(), Set.of(), "чорновіл");

        assertEquals(1, results.participants().size());
        assertEquals("Чорновіл Каріна Олегівна", results.participants().getFirst().participantName());
    }

    @Test
    void getResults_ShouldBuildScopeTitleWithStageAndTour_WhenTourIdsProvided() {
        mockCompetitionWithScores();

        OlympiadResults results = resultsService.getResults(COMPETITION_ID, Set.of(), Set.of(2L), null);

        assertEquals("Регіональний етап - Тур 2", results.scopeTitle());
    }

    @Test
    void getResults_ShouldBuildScopeTitleWithStageOnly_WhenStageIdsProvided() {
        mockCompetitionWithScores();

        OlympiadResults results = resultsService.getResults(COMPETITION_ID, Set.of(1L), Set.of(), null);

        assertEquals("Регіональний етап", results.scopeTitle());
    }

    @Test
    void getResults_ShouldThrowCompetitionNotFound_WhenCompetitionDoesNotExist() {
        when(competitionFacade.findCompetitionTreeByCompetitionId(999L)).thenReturn(Optional.empty());

        assertThrows(CompetitionNotFoundException.class,
            () -> resultsService.getResults(999L, Set.of(), Set.of(), null));
    }

    @Test
    void getResultsPage_ShouldReturnRequestedPage_WhenPageableProvided() {
        mockCompetitionWithScores();

        Page<?> page = resultsService.getResultsPage(
            COMPETITION_ID, Set.of(), Set.of(), null, PageRequest.of(1, 1));

        assertEquals(2, page.getTotalElements());
        assertEquals(1, page.getContent().size());
        assertEquals(1, page.getNumber());
    }
}
