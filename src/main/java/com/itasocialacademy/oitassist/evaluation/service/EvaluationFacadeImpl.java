package com.itasocialacademy.oitassist.evaluation.service;

import com.itasocialacademy.oitassist.evaluation.api.dto.CompetitionScore;
import com.itasocialacademy.oitassist.evaluation.api.dto.OlympiadResults;
import com.itasocialacademy.oitassist.evaluation.api.facade.EvaluationFacade;
import com.itasocialacademy.oitassist.evaluation.service.interfaces.ResultsService;
import com.itasocialacademy.oitassist.evaluation.service.interfaces.UserScoreService;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EvaluationFacadeImpl implements EvaluationFacade {
    private final ResultsService resultsService;
    private final UserScoreService userScoreService;

    @Override
    public OlympiadResults getResults(Long competitionId, Set<Long> stageIds, Set<Long> tourIds, String search) {
        return resultsService.getResults(competitionId, stageIds, tourIds, search);
    }

    @Override
    public Map<Long, CompetitionScore> getUserScores(Long userId, Collection<Long> competitionIds) {
        return userScoreService.getUserScores(userId, competitionIds);
    }
}