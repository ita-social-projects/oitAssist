package com.itasocialacademy.oitassist.evaluation.service;

import com.itasocialacademy.oitassist.competition.api.dto.StageDetail;
import com.itasocialacademy.oitassist.competition.api.dto.TourDetail;
import com.itasocialacademy.oitassist.evaluation.api.dto.ParticipantResult;
import com.itasocialacademy.oitassist.evaluation.api.dto.StageResult;
import com.itasocialacademy.oitassist.evaluation.api.dto.TourResult;
import com.itasocialacademy.oitassist.evaluation.dao.dto.request.ParticipantTaskScore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ResultsAggregator {
    public List<ParticipantResult> aggregate(List<ParticipantTaskScore> scores,
        List<TourDetail> scopeTours, List<StageDetail> stages) {
        Map<Long, StageDetail> stageById = stages.stream()
            .collect(Collectors.toMap(StageDetail::id, stage -> stage));

        Map<Long, String> nameByUser = new LinkedHashMap<>();
        Map<Long, Map<Long, Integer>> scoreByUserAndTour = new LinkedHashMap<>();

        for (ParticipantTaskScore score : scores) {
            nameByUser.putIfAbsent(score.userId(), score.participantName());
            Map<Long, Integer> byTour = scoreByUserAndTour
                .computeIfAbsent(score.userId(), id -> new LinkedHashMap<>());

            if (score.score() == null) {
                byTour.put(score.tourId(), null);
            } else if (byTour.containsKey(score.tourId())) {
                Integer current = byTour.get(score.tourId());
                byTour.put(score.tourId(), current == null ? null : current + score.score());
            } else {
                byTour.put(score.tourId(), score.score());
            }
        }

        return nameByUser.entrySet().stream()
            .map(user -> toParticipantResult(
                user.getValue(),
                scoreByUserAndTour.getOrDefault(user.getKey(), Map.of()),
                scopeTours, stageById))
            .sorted(Comparator.comparing(ParticipantResult::totalScore,
                Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ParticipantResult::participantName))
            .toList();
    }

    private ParticipantResult toParticipantResult(String name, Map<Long, Integer> scoreByTour,
        List<TourDetail> scopeTours, Map<Long, StageDetail> stageById) {
        Map<Long, List<TourResult>> toursByStage = new LinkedHashMap<>();
        for (TourDetail tour : scopeTours) {
            Integer tourScore = scoreByTour.get(tour.id());
            toursByStage
                .computeIfAbsent(tour.stageId(), id -> new ArrayList<>())
                .add(new TourResult(tour.title(), tourScore));
        }

        List<StageResult> stages = toursByStage.entrySet().stream()
            .map(entry -> {
                List<TourResult> tours = entry.getValue();
                Integer stageScore = sumOrNull(tours.stream().map(TourResult::tourScore).toList());
                String stageTitle = stageById.get(entry.getKey()).title();
                return new StageResult(stageTitle, stageScore, tours);
            })
            .toList();

        Integer total = sumOrNull(stages.stream().map(StageResult::stageScore).toList());
        return new ParticipantResult(name, total, stages);
    }

    private Integer sumOrNull(List<Integer> scores) {
        int sum = 0;
        for (Integer score : scores) {
            if (score == null) {
                return null;
            }
            sum += score;
        }
        return sum;
    }
}
