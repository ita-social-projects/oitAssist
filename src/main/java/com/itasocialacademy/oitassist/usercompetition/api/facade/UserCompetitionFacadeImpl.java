package com.itasocialacademy.oitassist.usercompetition.api.facade;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.usercompetition.api.interfaces.UserCompetitionFacade;
import com.itasocialacademy.oitassist.usercompetition.service.interfaces.UserCompetitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserCompetitionFacadeImpl implements UserCompetitionFacade {
    private final UserCompetitionService userCompetitionService;

    @Override
    public boolean hasActiveCompetitions(Long userId, List<CompetitionStatus> statuses) {
        return userCompetitionService.hasActiveCompetitions(userId, statuses);
    }
}
