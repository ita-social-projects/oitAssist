package com.itasocialacademy.oitassist.usercompetition.service.interfaces;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.core.rest.service.interfaces.BaseService;
import com.itasocialacademy.oitassist.usercompetition.dao.dto.request.CreateUserCompetitionDTO;
import com.itasocialacademy.oitassist.usercompetition.dao.dto.request.UpdateUserCompetitionDTO;
import com.itasocialacademy.oitassist.usercompetition.dao.dto.response.ResponseUserCompetitionDTO;
import com.itasocialacademy.oitassist.usercompetition.dao.enums.UserCompetitionStatus;
import com.itasocialacademy.oitassist.usercompetition.dao.model.UserCompetitionId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import java.util.List;

public interface UserCompetitionService extends BaseService<UserCompetitionId, CreateUserCompetitionDTO, UpdateUserCompetitionDTO, ResponseUserCompetitionDTO> {
    /**
     * Checks if user has competitions with given statuses.
     *
     * @param userId   the user's id
     * @param statuses list of competition statuses to check
     * @return true if at least one competition with given statuses exists
     */
    boolean hasActiveCompetitions(Long userId, List<CompetitionStatus> statuses);

    Page<ResponseUserCompetitionDTO> getAllCompetitionsByStatus(UserCompetitionStatus status, Pageable pageable);

    void markAsRead(Long competitionId);

    Long countOfUnreadInvites();

    ResponseUserCompetitionDTO updateUserCompetitionStatus(Long competitionId, UserCompetitionStatus status);
}