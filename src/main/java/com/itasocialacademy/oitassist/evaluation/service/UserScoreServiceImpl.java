package com.itasocialacademy.oitassist.evaluation.service;

import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.evaluation.api.dto.CompetitionScore;
import com.itasocialacademy.oitassist.evaluation.dao.model.Evaluation;
import com.itasocialacademy.oitassist.evaluation.dao.repository.EvaluationRepository;
import com.itasocialacademy.oitassist.evaluation.service.interfaces.UserScoreService;
import com.itasocialacademy.oitassist.submission.api.SubmissionFacade;
import com.itasocialacademy.oitassist.submission.api.dto.SubmissionDetail;
import com.itasocialacademy.oitassist.taskassignment.api.TaskAssignmentFacade;
import com.itasocialacademy.oitassist.taskassignment.api.dto.TaskAssignmentDetailDTO;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserScoreServiceImpl implements UserScoreService {
    private final CompetitionFacade competitionFacade;
    private final TaskAssignmentFacade taskAssignmentFacade;
    private final SubmissionFacade submissionFacade;
    private final EvaluationRepository evaluationRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<Long, CompetitionScore> getUserScores(Long userId, Collection<Long> competitionIds) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(competitionIds, "competitionIds must not be null");
        if (competitionIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> competitionByTour =
            toCompetitionByTour(competitionFacade.findTourIdsByCompetitionIds(competitionIds));
        List<TaskAssignmentDetailDTO> assignments =
            taskAssignmentFacade.findAssignmentsByTourIds(competitionByTour.keySet());

        Map<Long, Integer> maxScores = sumMaxPoints(assignments, competitionByTour);
        Map<Long, Double> scores = sumUserScores(userId, assignments, competitionByTour);

        return competitionIds.stream()
            .distinct()
            .collect(Collectors.toMap(
                Function.identity(),
                competitionId -> new CompetitionScore(
                    scores.getOrDefault(competitionId, 0.0),
                    maxScores.getOrDefault(competitionId, 0))));
    }

    private Map<Long, Long> toCompetitionByTour(Map<Long, List<Long>> tourIdsByCompetition) {
        Map<Long, Long> competitionByTour = new HashMap<>();
        tourIdsByCompetition.forEach(
            (competitionId, tourIds) -> tourIds.forEach(tourId -> competitionByTour.put(tourId, competitionId)));
        return competitionByTour;
    }

    private Map<Long, Integer> sumMaxPoints(
        List<TaskAssignmentDetailDTO> assignments, Map<Long, Long> competitionByTour) {
        return assignments.stream()
            .collect(Collectors.groupingBy(
                assignment -> competitionByTour.get(assignment.tourId()),
                Collectors.summingInt(TaskAssignmentDetailDTO::maxPoints)));
    }

    private Map<Long, Double> sumUserScores(
        Long userId, List<TaskAssignmentDetailDTO> assignments, Map<Long, Long> competitionByTour) {
        Map<Long, Long> competitionByAssignment = assignments.stream()
            .collect(Collectors.toMap(
                TaskAssignmentDetailDTO::id,
                assignment -> competitionByTour.get(assignment.tourId())));

        List<SubmissionDetail> submissions =
            submissionFacade.getUserSubmissionsByTaskAssignmentIds(userId, competitionByAssignment.keySet());
        Map<Long, Double> scoreBySubmissionId = findScoresBySubmissionId(submissions);

        return submissions.stream()
            .collect(Collectors.groupingBy(
                submission -> competitionByAssignment.get(submission.taskAssignmentId()),
                Collectors.summingDouble(
                    submission -> scoreBySubmissionId.getOrDefault(submission.id(), 0.0))));
    }

    private Map<Long, Double> findScoresBySubmissionId(List<SubmissionDetail> submissions) {
        if (submissions.isEmpty()) {
            return Map.of();
        }
        List<Long> submissionIds = submissions.stream()
            .map(SubmissionDetail::id)
            .toList();
        return evaluationRepository.findAllBySubmissionIdIn(submissionIds).stream()
            .filter(evaluation -> evaluation.getScore() != null)
            .collect(Collectors.toMap(Evaluation::getSubmissionId, Evaluation::getScore));
    }
}