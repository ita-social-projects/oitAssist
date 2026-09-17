package com.itasocialacademy.oitassist.usercompetition.service.interfaces;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import java.util.List;

public interface UserCompetitionService {
    /**
     * Checks if user has competitions with given statuses.
     *
     * @param userId   the user's id
     * @param statuses list of competition statuses to check
     * @return true if at least one competition with given statuses exists
     */
    boolean hasActiveCompetitions(Long userId, List<CompetitionStatus> statuses);
}