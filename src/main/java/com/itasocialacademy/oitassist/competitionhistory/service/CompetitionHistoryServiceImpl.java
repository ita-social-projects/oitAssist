package com.itasocialacademy.oitassist.competitionhistory.service;

import com.itasocialacademy.oitassist.competition.api.CompetitionFacade;
import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competitionhistory.dto.response.CompetitionHistoryResponse;
import com.itasocialacademy.oitassist.competitionhistory.mapper.CompetitionHistoryMapper;
import com.itasocialacademy.oitassist.competitionhistory.service.interfaces.CompetitionHistoryService;
import com.itasocialacademy.oitassist.core.exceptions.InsufficientPermissionsException;
import com.itasocialacademy.oitassist.participation.api.ParticipationFacade;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import java.util.List;
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

    private final SecurityFacade securityFacade;
    private final ParticipationFacade participationFacade;
    private final CompetitionFacade competitionFacade;
    private final CompetitionHistoryMapper mapper;

    @Override
    public Page<CompetitionHistoryResponse> getHistory(Long userId, Pageable pageable) {
        checkOwnerOrAdmin(userId);
        List<Long> competitionIds = participationFacade.findCompetitionIdsByUserId(userId);
        if (competitionIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return competitionFacade
            .findCompetitionsByIdsAndStatuses(competitionIds, HISTORY_STATUSES, pageable)
            .map(mapper::toResponse);
    }

    private void checkOwnerOrAdmin(Long userId) {
        if (!securityFacade.isOwner(userId) && !securityFacade.hasRole(ADMIN_ROLE)) {
            throw new InsufficientPermissionsException();
        }
    }
}
