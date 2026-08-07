package com.itasocialacademy.oitassist.evaluation.service;

import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.competition.api.dto.CompetitionTreeDetail;
import com.itasocialacademy.oitassist.competition.api.dto.StageDetail;
import com.itasocialacademy.oitassist.competition.api.dto.StageTreeDetail;
import com.itasocialacademy.oitassist.competition.api.dto.TourDetail;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionNotFoundException;
import com.itasocialacademy.oitassist.evaluation.api.dto.OlympiadResults;
import com.itasocialacademy.oitassist.evaluation.api.dto.ParticipantResult;
import com.itasocialacademy.oitassist.evaluation.dao.repository.StubScoreRepository;
import com.itasocialacademy.oitassist.evaluation.service.interfaces.ResultsService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResultsServiceImpl implements ResultsService {
    private final StubScoreRepository scoreRepository;
    private final ResultsAggregator resultsAggregator;
    private final CompetitionFacade competitionFacade;

    @Override
    public OlympiadResults getResults(Long competitionId, Set<Long> stageIds, Set<Long> tourIds, String search) {
        CompetitionTreeDetail tree = competitionFacade.findCompetitionTreeByCompetitionId(competitionId)
            .orElseThrow(() -> new CompetitionNotFoundException(competitionId));

        List<StageDetail> stages = tree.stages().stream()
            .map(StageTreeDetail::stage)
            .toList();

        List<TourDetail> allTours = tree.stages().stream()
            .flatMap(stage -> stage.tours().stream())
            .toList();
        List<TourDetail> scopeTours = resolveScope(allTours, stageIds, tourIds);

        Set<Long> scopeTourIds = scopeTours.stream()
            .map(TourDetail::id)
            .collect(Collectors.toSet());

        List<ParticipantResult> all = resultsAggregator.aggregate(
            scoreRepository.findScores(competitionId, scopeTourIds), scopeTours, stages);

        return new OlympiadResults(tree.competition().title(),
            buildScopeTitle(scopeTours, stages, stageIds, tourIds),
            filterByName(all, search));
    }

    private List<ParticipantResult> filterByName(List<ParticipantResult> all, String search) {
        if (search == null || search.isBlank()) {
            return all;
        }
        String query = search.toLowerCase(Locale.ROOT);
        return all.stream()
            .filter(participant -> participant.participantName().toLowerCase(Locale.ROOT).contains(query))
            .toList();
    }

    private String buildScopeTitle(List<TourDetail> scopeTours, List<StageDetail> stages,
        Set<Long> stageIds, Set<Long> tourIds) {
        Map<Long, String> stageTitleById = stages.stream()
            .collect(Collectors.toMap(StageDetail::id, StageDetail::title));

        if (tourIds != null && !tourIds.isEmpty()) {
            Map<Long, List<String>> toursByStage = new LinkedHashMap<>();
            for (TourDetail tour : scopeTours) {
                toursByStage.computeIfAbsent(tour.stageId(), id -> new ArrayList<>()).add(tour.title());
            }
            return toursByStage.entrySet().stream()
                .map(entry -> stageTitleById.get(entry.getKey()) + " - " + String.join(", ", entry.getValue()))
                .collect(Collectors.joining("; "));
        }
        if (stageIds != null && !stageIds.isEmpty()) {
            return stageIds.stream()
                .map(stageTitleById::get)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
        }
        return "";
    }

    private List<TourDetail> resolveScope(List<TourDetail> allTours, Set<Long> stageIds, Set<Long> tourIds) {
        if (tourIds != null && !tourIds.isEmpty()) {
            return allTours.stream()
                .filter(tour -> tourIds.contains(tour.id()))
                .toList();
        }
        if (stageIds != null && !stageIds.isEmpty()) {
            return allTours.stream()
                .filter(tour -> stageIds.contains(tour.stageId()))
                .toList();
        }
        return allTours;
    }

    @Override
    public Page<ParticipantResult> getResultsPage(Long competitionId, Set<Long> stageIds, Set<Long> tourIds,
        String search, Pageable pageable) {
        List<ParticipantResult> filtered = getResults(competitionId, stageIds, tourIds, search).participants();

        int from = (int) pageable.getOffset();
        if (from >= filtered.size()) {
            return new PageImpl<>(List.of(), pageable, filtered.size());
        }
        int to = Math.min(from + pageable.getPageSize(), filtered.size());

        return new PageImpl<>(filtered.subList(from, to), pageable, filtered.size());
    }
}
