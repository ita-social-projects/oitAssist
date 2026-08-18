package com.itasocialacademy.oitassist.evaluation.service;

import com.itasocialacademy.oitassist.evaluation.api.dto.OlympiadResults;
import com.itasocialacademy.oitassist.evaluation.api.facade.EvaluationFacade;
import com.itasocialacademy.oitassist.evaluation.service.interfaces.ResultsService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EvaluationFacadeImpl implements EvaluationFacade {
    private final ResultsService resultsService;

    @Override
    public OlympiadResults getResults(Long competitionId, Set<Long> stageIds, Set<Long> tourIds, String search) {
        return resultsService.getResults(competitionId, stageIds, tourIds, search);
    }
}
