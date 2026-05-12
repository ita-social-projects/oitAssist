package com.itasocialacademy.oitassist.usercompetition.api.interfaces;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import org.springframework.modulith.NamedInterface;

import java.util.List;

@NamedInterface("UserCompetitionFacade")
public interface UserCompetitionFacade {
    /**
     * Checks if the user has any competitions with the given statuses.
     *
     * @param userId   the ID of the user to check
     * @param statuses list of competition statuses to filter by
     * @return true if the user has at least one competition with any of the given statuses, false otherwise
     */
    boolean hasActiveCompetitions(Long userId, List<CompetitionStatus> statuses);
}
