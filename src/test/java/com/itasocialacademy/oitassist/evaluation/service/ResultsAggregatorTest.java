package com.itasocialacademy.oitassist.evaluation.service;

import com.itasocialacademy.oitassist.competition.api.dto.StageDetail;
import com.itasocialacademy.oitassist.competition.api.dto.TourDetail;
import com.itasocialacademy.oitassist.evaluation.api.dto.ParticipantResult;
import com.itasocialacademy.oitassist.evaluation.api.dto.StageResult;
import com.itasocialacademy.oitassist.evaluation.api.dto.TourResult;
import com.itasocialacademy.oitassist.evaluation.dao.dto.request.ParticipantTaskScore;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ResultsAggregatorTest {
    private static final StageDetail STAGE = StageDetail.builder()
        .id(1L)
        .title("Регіональний етап")
        .sortPosition((short) 1)
        .build();

    private static final TourDetail TOUR_1 = TourDetail.builder()
        .id(1L)
        .stageId(1L)
        .title("Тур 1")
        .sortPosition((short) 1)
        .build();

    private static final TourDetail TOUR_2 = TourDetail.builder()
        .id(2L)
        .stageId(1L)
        .title("Тур 2")
        .sortPosition((short) 2)
        .build();

    private final ResultsAggregator aggregator = new ResultsAggregator();

    @Test
    void aggregate_ShouldSumTaskScoresIntoTourScore_WhenParticipantHasSeveralTasksInTour() {
        List<ParticipantTaskScore> scores = List.of(
            new ParticipantTaskScore(1L, "Ігор", 1L, 10L, 5),
            new ParticipantTaskScore(1L, "Ігор", 1L, 11L, 3));

        List<ParticipantResult> result = aggregator.aggregate(scores, List.of(TOUR_1), List.of(STAGE));

        assertEquals(8, firstTour(result.getFirst()).tourScore());
    }

    @Test
    void aggregate_ShouldReturnNullTourScore_WhenParticipantHasNoScoresInTour() {
        List<ParticipantTaskScore> scores = List.of(
            new ParticipantTaskScore(2L, "Каріна", 1L, 10L, 4));

        List<ParticipantResult> result = aggregator.aggregate(scores, List.of(TOUR_1, TOUR_2), List.of(STAGE));

        List<TourResult> tours = result.getFirst().stages().getFirst().tours();
        assertEquals(2, tours.size());
        assertNull(tours.getLast().tourScore());
    }

    @Test
    void aggregate_ShouldSumTourScoresIntoTotalScore_WhenParticipantHasSeveralTours() {
        List<ParticipantTaskScore> scores = List.of(
            new ParticipantTaskScore(1L, "Ігор", 1L, 10L, 5),
            new ParticipantTaskScore(1L, "Ігор", 1L, 11L, 3),
            new ParticipantTaskScore(1L, "Ігор", 2L, 12L, 7));

        List<ParticipantResult> result = aggregator.aggregate(scores, List.of(TOUR_1, TOUR_2), List.of(STAGE));

        assertEquals(15, result.getFirst().totalScore());
        assertEquals(15, result.getFirst().stages().getFirst().stageScore());
    }

    @Test
    void aggregate_ShouldSortParticipantsByTotalScoreDesc_WhenSeveralParticipants() {
        List<ParticipantTaskScore> scores = List.of(
            new ParticipantTaskScore(2L, "Каріна", 1L, 10L, 4),
            new ParticipantTaskScore(1L, "Ігор", 1L, 10L, 15));

        List<ParticipantResult> result = aggregator.aggregate(scores, List.of(TOUR_1), List.of(STAGE));

        assertEquals("Ігор", result.getFirst().participantName());
        assertEquals("Каріна", result.getLast().participantName());
    }

    @Test
    void aggregate_ShouldUseTitlesFromCompetitionStructure_WhenBuildingResult() {
        List<ParticipantTaskScore> scores = List.of(
            new ParticipantTaskScore(1L, "Ігор", 1L, 10L, 5));

        List<ParticipantResult> result = aggregator.aggregate(scores, List.of(TOUR_1), List.of(STAGE));

        StageResult stage = result.getFirst().stages().getFirst();
        assertEquals("Регіональний етап", stage.stageTitle());
        assertEquals("Тур 1", stage.tours().getFirst().tourTitle());
    }

    private TourResult firstTour(ParticipantResult participant) {
        return participant.stages().getFirst().tours().getFirst();
    }

    @Test
    void aggregate_ShouldReturnNullStageAndTotal_WhenAnyTourIsNotScored() {
        List<ParticipantTaskScore> scores = List.of(
            new ParticipantTaskScore(2L, "Каріна", 1L, 10L, 4));

        List<ParticipantResult> result = aggregator.aggregate(scores, List.of(TOUR_1, TOUR_2), List.of(STAGE));

        ParticipantResult participant = result.getFirst();
        assertNull(participant.stages().getFirst().stageScore());
        assertNull(participant.totalScore());
    }

    @Test
    void aggregate_ShouldPlaceParticipantsWithUnknownTotalLast_WhenSomeAreNotScored() {
        List<ParticipantTaskScore> scores = List.of(
            new ParticipantTaskScore(2L, "Каріна", 1L, 10L, 4),
            new ParticipantTaskScore(1L, "Ігор", 1L, 11L, 5),
            new ParticipantTaskScore(1L, "Ігор", 2L, 12L, 7));

        List<ParticipantResult> result = aggregator.aggregate(scores, List.of(TOUR_1, TOUR_2), List.of(STAGE));

        assertEquals("Ігор", result.getFirst().participantName());
        assertEquals("Каріна", result.getLast().participantName());
        assertNull(result.getLast().totalScore());
    }

    @Test
    void aggregate_ShouldReturnNullTourScore_WhenAnyTaskIsNotScored() {
        List<ParticipantTaskScore> scores = List.of(
            new ParticipantTaskScore(1L, "Ігор", 1L, 10L, 5),
            new ParticipantTaskScore(1L, "Ігор", 1L, 11L, null));

        List<ParticipantResult> result = aggregator.aggregate(scores, List.of(TOUR_1), List.of(STAGE));

        assertNull(firstTour(result.getFirst()).tourScore());
    }

    @Test
    void aggregate_ShouldReturnNullTourScore_WhenNotScoredTaskComesFirst() {
        List<ParticipantTaskScore> scores = List.of(
            new ParticipantTaskScore(1L, "Ігор", 1L, 10L, null),
            new ParticipantTaskScore(1L, "Ігор", 1L, 11L, 5));

        List<ParticipantResult> result = aggregator.aggregate(scores, List.of(TOUR_1), List.of(STAGE));

        assertNull(firstTour(result.getFirst()).tourScore());
    }
}
