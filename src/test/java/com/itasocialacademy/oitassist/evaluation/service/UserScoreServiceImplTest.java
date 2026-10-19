package com.itasocialacademy.oitassist.evaluation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.evaluation.api.dto.CompetitionScore;
import com.itasocialacademy.oitassist.evaluation.dao.model.Evaluation;
import com.itasocialacademy.oitassist.evaluation.dao.repository.EvaluationRepository;
import com.itasocialacademy.oitassist.submission.api.SubmissionFacade;
import com.itasocialacademy.oitassist.submission.api.dto.SubmissionDetail;
import com.itasocialacademy.oitassist.taskassignment.api.TaskAssignmentFacade;
import com.itasocialacademy.oitassist.taskassignment.api.dto.TaskAssignmentDetailDTO;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserScoreServiceImplTest {
    private static final Long USER_ID = 7L;
    private static final Long COMPETITION_ID = 1L;
    private static final Long OTHER_COMPETITION_ID = 2L;
    private static final Long FIRST_TOUR_ID = 10L;
    private static final Long SECOND_TOUR_ID = 11L;
    private static final Long OTHER_TOUR_ID = 12L;
    private static final Long FIRST_ASSIGNMENT_ID = 100L;
    private static final Long SECOND_ASSIGNMENT_ID = 101L;
    private static final Long THIRD_ASSIGNMENT_ID = 102L;
    private static final Long OTHER_ASSIGNMENT_ID = 200L;
    private static final Set<Long> FIRST_TOUR_ASSIGNMENT_IDS =
        Set.of(FIRST_ASSIGNMENT_ID, SECOND_ASSIGNMENT_ID);

    @Mock
    private CompetitionFacade competitionFacade;
    @Mock
    private TaskAssignmentFacade taskAssignmentFacade;
    @Mock
    private SubmissionFacade submissionFacade;
    @Mock
    private EvaluationRepository evaluationRepository;

    @InjectMocks
    private UserScoreServiceImpl userScoreService;

    @Test
    void getUserScores_ShouldSumScoresAndMaxPoints_WhenUserHasEvaluatedSubmissions() {
        List<Long> competitionIds = List.of(COMPETITION_ID);
        when(competitionFacade.findTourIdsByCompetitionIds(competitionIds))
            .thenReturn(Map.of(COMPETITION_ID, List.of(FIRST_TOUR_ID, SECOND_TOUR_ID)));
        when(taskAssignmentFacade.findAssignmentsByTourIds(Set.of(FIRST_TOUR_ID, SECOND_TOUR_ID)))
            .thenReturn(List.of(
                assignment(FIRST_ASSIGNMENT_ID, FIRST_TOUR_ID, 10),
                assignment(SECOND_ASSIGNMENT_ID, FIRST_TOUR_ID, 15),
                assignment(THIRD_ASSIGNMENT_ID, SECOND_TOUR_ID, 20)));
        when(submissionFacade.getUserSubmissionsByTaskAssignmentIds(
            USER_ID, Set.of(FIRST_ASSIGNMENT_ID, SECOND_ASSIGNMENT_ID, THIRD_ASSIGNMENT_ID)))
            .thenReturn(List.of(
                submission(201L, FIRST_ASSIGNMENT_ID),
                submission(203L, THIRD_ASSIGNMENT_ID)));
        when(evaluationRepository.findAllBySubmissionIdIn(List.of(201L, 203L)))
            .thenReturn(List.of(evaluation(201L, 8.5), evaluation(203L, 20.0)));

        Map<Long, CompetitionScore> result = userScoreService.getUserScores(USER_ID, competitionIds);

        assertEquals(Map.of(COMPETITION_ID, new CompetitionScore(28.5, 45)), result);
    }

    @Test
    void getUserScores_ShouldCountSubmissionAsZero_WhenItHasNoEvaluation() {
        List<Long> competitionIds = List.of(COMPETITION_ID);
        when(competitionFacade.findTourIdsByCompetitionIds(competitionIds))
            .thenReturn(Map.of(COMPETITION_ID, List.of(FIRST_TOUR_ID)));
        when(taskAssignmentFacade.findAssignmentsByTourIds(Set.of(FIRST_TOUR_ID)))
            .thenReturn(List.of(
                assignment(FIRST_ASSIGNMENT_ID, FIRST_TOUR_ID, 10),
                assignment(SECOND_ASSIGNMENT_ID, FIRST_TOUR_ID, 15)));
        when(submissionFacade.getUserSubmissionsByTaskAssignmentIds(USER_ID, FIRST_TOUR_ASSIGNMENT_IDS))
            .thenReturn(List.of(submission(201L, FIRST_ASSIGNMENT_ID)));
        when(evaluationRepository.findAllBySubmissionIdIn(List.of(201L))).thenReturn(List.of());

        Map<Long, CompetitionScore> result = userScoreService.getUserScores(USER_ID, competitionIds);

        assertEquals(Map.of(COMPETITION_ID, new CompetitionScore(0.0, 25)), result);
    }

    @Test
    void getUserScores_ShouldIgnoreEvaluation_WhenItsScoreIsNull() {
        List<Long> competitionIds = List.of(COMPETITION_ID);
        when(competitionFacade.findTourIdsByCompetitionIds(competitionIds))
            .thenReturn(Map.of(COMPETITION_ID, List.of(FIRST_TOUR_ID)));
        when(taskAssignmentFacade.findAssignmentsByTourIds(Set.of(FIRST_TOUR_ID)))
            .thenReturn(List.of(
                assignment(FIRST_ASSIGNMENT_ID, FIRST_TOUR_ID, 10),
                assignment(SECOND_ASSIGNMENT_ID, FIRST_TOUR_ID, 15)));
        when(submissionFacade.getUserSubmissionsByTaskAssignmentIds(USER_ID, FIRST_TOUR_ASSIGNMENT_IDS))
            .thenReturn(List.of(
                submission(201L, FIRST_ASSIGNMENT_ID),
                submission(202L, SECOND_ASSIGNMENT_ID)));
        when(evaluationRepository.findAllBySubmissionIdIn(List.of(201L, 202L)))
            .thenReturn(List.of(evaluation(201L, 5.0), evaluation(202L, null)));

        Map<Long, CompetitionScore> result = userScoreService.getUserScores(USER_ID, competitionIds);

        assertEquals(Map.of(COMPETITION_ID, new CompetitionScore(5.0, 25)), result);
    }

    @Test
    void getUserScores_ShouldNotLoadEvaluations_WhenUserHasNoSubmissions() {
        List<Long> competitionIds = List.of(COMPETITION_ID);
        when(competitionFacade.findTourIdsByCompetitionIds(competitionIds))
            .thenReturn(Map.of(COMPETITION_ID, List.of(FIRST_TOUR_ID)));
        when(taskAssignmentFacade.findAssignmentsByTourIds(Set.of(FIRST_TOUR_ID)))
            .thenReturn(List.of(
                assignment(FIRST_ASSIGNMENT_ID, FIRST_TOUR_ID, 10),
                assignment(SECOND_ASSIGNMENT_ID, FIRST_TOUR_ID, 15)));
        when(submissionFacade.getUserSubmissionsByTaskAssignmentIds(USER_ID, FIRST_TOUR_ASSIGNMENT_IDS))
            .thenReturn(List.of());

        Map<Long, CompetitionScore> result = userScoreService.getUserScores(USER_ID, competitionIds);

        assertEquals(Map.of(COMPETITION_ID, new CompetitionScore(0.0, 25)), result);
        verifyNoInteractions(evaluationRepository);
    }

    @Test
    void getUserScores_ShouldCalculateEachCompetitionSeparately_WhenSeveralCompetitionsRequested() {
        List<Long> competitionIds = List.of(COMPETITION_ID, OTHER_COMPETITION_ID);
        when(competitionFacade.findTourIdsByCompetitionIds(competitionIds))
            .thenReturn(Map.of(
                COMPETITION_ID, List.of(FIRST_TOUR_ID),
                OTHER_COMPETITION_ID, List.of(OTHER_TOUR_ID)));
        when(taskAssignmentFacade.findAssignmentsByTourIds(Set.of(FIRST_TOUR_ID, OTHER_TOUR_ID)))
            .thenReturn(List.of(
                assignment(FIRST_ASSIGNMENT_ID, FIRST_TOUR_ID, 10),
                assignment(OTHER_ASSIGNMENT_ID, OTHER_TOUR_ID, 30)));
        when(submissionFacade.getUserSubmissionsByTaskAssignmentIds(
            USER_ID, Set.of(FIRST_ASSIGNMENT_ID, OTHER_ASSIGNMENT_ID)))
            .thenReturn(List.of(
                submission(201L, FIRST_ASSIGNMENT_ID),
                submission(301L, OTHER_ASSIGNMENT_ID)));
        when(evaluationRepository.findAllBySubmissionIdIn(List.of(201L, 301L)))
            .thenReturn(List.of(evaluation(201L, 8.5), evaluation(301L, 25.0)));

        Map<Long, CompetitionScore> result = userScoreService.getUserScores(USER_ID, competitionIds);

        assertEquals(Map.of(
            COMPETITION_ID, new CompetitionScore(8.5, 10),
            OTHER_COMPETITION_ID, new CompetitionScore(25.0, 30)), result);
    }

    @Test
    void getUserScores_ShouldKeepZeroScore_WhenOnlyOneOfCompetitionsHasSubmissions() {
        List<Long> competitionIds = List.of(COMPETITION_ID, OTHER_COMPETITION_ID);
        when(competitionFacade.findTourIdsByCompetitionIds(competitionIds))
            .thenReturn(Map.of(
                COMPETITION_ID, List.of(FIRST_TOUR_ID),
                OTHER_COMPETITION_ID, List.of(OTHER_TOUR_ID)));
        when(taskAssignmentFacade.findAssignmentsByTourIds(Set.of(FIRST_TOUR_ID, OTHER_TOUR_ID)))
            .thenReturn(List.of(
                assignment(FIRST_ASSIGNMENT_ID, FIRST_TOUR_ID, 10),
                assignment(OTHER_ASSIGNMENT_ID, OTHER_TOUR_ID, 30)));
        when(submissionFacade.getUserSubmissionsByTaskAssignmentIds(
            USER_ID, Set.of(FIRST_ASSIGNMENT_ID, OTHER_ASSIGNMENT_ID)))
            .thenReturn(List.of(submission(201L, FIRST_ASSIGNMENT_ID)));
        when(evaluationRepository.findAllBySubmissionIdIn(List.of(201L)))
            .thenReturn(List.of(evaluation(201L, 8.5)));

        Map<Long, CompetitionScore> result = userScoreService.getUserScores(USER_ID, competitionIds);

        assertEquals(Map.of(
            COMPETITION_ID, new CompetitionScore(8.5, 10),
            OTHER_COMPETITION_ID, new CompetitionScore(0.0, 30)), result);
    }

    @Test
    void getUserScores_ShouldReturnZeroScore_WhenCompetitionHasNoTours() {
        List<Long> competitionIds = List.of(COMPETITION_ID);
        when(competitionFacade.findTourIdsByCompetitionIds(competitionIds)).thenReturn(Map.of());

        Map<Long, CompetitionScore> result = userScoreService.getUserScores(USER_ID, competitionIds);

        assertEquals(Map.of(COMPETITION_ID, new CompetitionScore(0.0, 0)), result);
    }

    @Test
    void getUserScores_ShouldReturnSingleEntry_WhenCompetitionIdIsRepeated() {
        List<Long> competitionIds = List.of(COMPETITION_ID, COMPETITION_ID);
        when(competitionFacade.findTourIdsByCompetitionIds(competitionIds))
            .thenReturn(Map.of(COMPETITION_ID, List.of(FIRST_TOUR_ID)));
        when(taskAssignmentFacade.findAssignmentsByTourIds(Set.of(FIRST_TOUR_ID)))
            .thenReturn(List.of(assignment(FIRST_ASSIGNMENT_ID, FIRST_TOUR_ID, 10)));

        Map<Long, CompetitionScore> result = userScoreService.getUserScores(USER_ID, competitionIds);

        assertEquals(Map.of(COMPETITION_ID, new CompetitionScore(0.0, 10)), result);
    }

    @Test
    void getUserScores_ShouldReturnEmptyMap_WhenNoCompetitionIdsGiven() {
        Map<Long, CompetitionScore> result = userScoreService.getUserScores(USER_ID, List.of());

        assertEquals(Map.of(), result);
        verifyNoInteractions(
            competitionFacade, taskAssignmentFacade, submissionFacade, evaluationRepository);
    }

    @Test
    void getUserScores_ShouldThrowNullPointerException_WhenUserIdIsNull() {
        List<Long> competitionIds = List.of(COMPETITION_ID);

        assertThrows(NullPointerException.class,
            () -> userScoreService.getUserScores(null, competitionIds));
    }

    @Test
    void getUserScores_ShouldThrowNullPointerException_WhenCompetitionIdsIsNull() {
        assertThrows(NullPointerException.class,
            () -> userScoreService.getUserScores(USER_ID, null));
    }

    private static TaskAssignmentDetailDTO assignment(Long id, Long tourId, Integer maxPoints) {
        return new TaskAssignmentDetailDTO(id, null, tourId, null, maxPoints, null);
    }

    private static SubmissionDetail submission(Long id, Long taskAssignmentId) {
        return SubmissionDetail.builder().id(id).taskAssignmentId(taskAssignmentId).build();
    }

    private static Evaluation evaluation(Long submissionId, Double score) {
        return Evaluation.builder().submissionId(submissionId).score(score).build();
    }
}