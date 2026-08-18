package com.itasocialacademy.oitassist.export.service;

import com.itasocialacademy.oitassist.evaluation.api.dto.OlympiadResults;
import com.itasocialacademy.oitassist.evaluation.api.facade.EvaluationFacade;
import com.itasocialacademy.oitassist.export.dao.dto.ExportData;
import com.itasocialacademy.oitassist.export.service.interfaces.ExportService;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {
    private static final int MAX_FILE_NAME_LENGTH = 100;
    private final EvaluationFacade evaluationFacade;

    @Override
    public ExportData getExportData(Long olympiadId, List<Long> stageIds, List<Long> tourIds, String search) {
        Set<Long> stages = stageIds == null ? Set.of() : Set.copyOf(stageIds);
        Set<Long> tours = tourIds == null ? Set.of() : Set.copyOf(tourIds);

        OlympiadResults results = evaluationFacade.getResults(olympiadId, stages, tours, search);

        return new ExportData(
            buildFileName(results.olympiadTitle(), results.scopeTitle()),
            results.participants());
    }

    private String buildFileName(String olympiadTitle, String scopeTitle) {
        String scope = scopeTitle.isBlank() ? "" : " - " + scopeTitle;
        String cleaned = (olympiadTitle + scope).replaceAll("[\\\\/:*?\"<>|]", "-");
        if (cleaned.length() <= MAX_FILE_NAME_LENGTH) {
            return cleaned.strip();
        }
        return cleaned.substring(0, MAX_FILE_NAME_LENGTH).strip();
    }
}
