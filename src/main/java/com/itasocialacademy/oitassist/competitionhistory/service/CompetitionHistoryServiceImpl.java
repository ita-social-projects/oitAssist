package com.itasocialacademy.oitassist.competitionhistory.service;

import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.competition.api.dto.CompetitionDetail;
import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competitionhistory.dto.response.CompetitionHistoryResponse;
import com.itasocialacademy.oitassist.competitionhistory.mapper.CompetitionHistoryMapper;
import com.itasocialacademy.oitassist.competitionhistory.service.interfaces.CompetitionHistoryService;
import com.itasocialacademy.oitassist.core.exceptions.InsufficientPermissionsException;
import com.itasocialacademy.oitassist.evaluation.api.dto.CompetitionScore;
import com.itasocialacademy.oitassist.evaluation.api.facade.EvaluationFacade;
import com.itasocialacademy.oitassist.participation.api.ParticipationFacade;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompetitionHistoryServiceImpl implements CompetitionHistoryService {
    private static final String ADMIN_ROLE = "ADMIN";
    private static final Set<CompetitionStatus> HISTORY_STATUSES =
        Set.of(CompetitionStatus.FINISHED, CompetitionStatus.ARCHIVED);
    private static final CompetitionScore NO_SCORE = new CompetitionScore(0.0, 0);

    private final SecurityFacade securityFacade;
    private final ParticipationFacade participationFacade;
    private final CompetitionFacade competitionFacade;
    private final EvaluationFacade evaluationFacade;
    private final CompetitionHistoryMapper mapper;

    @Override
    public Page<CompetitionHistoryResponse> getHistory(Long userId, Pageable pageable) {
        checkOwnerOrAdmin(userId);
        List<Long> competitionIds = participationFacade.findCompetitionIdsByUserId(userId);
        if (competitionIds.isEmpty()) {
            return Page.empty(pageable);
        }
        Page<CompetitionDetail> page = competitionFacade
            .findCompetitionsByIdsAndStatuses(competitionIds, HISTORY_STATUSES, pageable);
        Map<Long, CompetitionScore> scores = loadScores(userId, page);
        return page.map(detail -> mapper.toResponse(detail, scores.getOrDefault(detail.id(), NO_SCORE)));
    }

    private Map<Long, CompetitionScore> loadScores(Long userId, Page<CompetitionDetail> page) {
        if (page.isEmpty()) {
            return Map.of();
        }
        List<Long> pageIds = page.getContent().stream().map(CompetitionDetail::id).toList();
        return evaluationFacade.getUserScores(userId, pageIds);
    }

    private void checkOwnerOrAdmin(Long userId) {
        if (!securityFacade.isOwner(userId) && !securityFacade.hasRole(ADMIN_ROLE)) {
            throw new InsufficientPermissionsException();
        }
    }
}