package com.itasocialacademy.oitassist.competition.service.interfaces;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.dto.request.CreateCompetitionRequest;
import com.itasocialacademy.oitassist.competition.dto.response.CompetitionResponse;
import com.itasocialacademy.oitassist.competition.dto.response.CompetitionTreeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CompetitionService {
    /**
     * Creates a new competition.
     *
     * @param request has data about Competition
     */
    CompetitionResponse create(CreateCompetitionRequest request);

    /**
     * Retrieve a competition by ID w/o checking access role.
     *
     * @param competitionId Competition ID
     */
    CompetitionResponse getById(Long competitionId);

    /**
     * Retrieves details of a specific by ID. Visibility depends on user role.
     *
     * @param competitionId Competition ID
     * @return {@link CompetitionResponse}
     */
    CompetitionResponse getVisibleById(Long competitionId);

    /**
     * Transitions the competition to a new status. Publishing requires at least one
     * Stage and one Tour.
     *
     * @param competitionId Competition ID
     * @param status        a status of a Competition
     * @return {@link CompetitionResponse}
     */
    CompetitionResponse changeStatus(Long competitionId, CompetitionStatus status);

    Page<CompetitionResponse> getAllVisible(Pageable pageable);

    /**
     * Retrieves a paginated list of competitions that have been archived.
     *
     */
    Page<CompetitionResponse> getArchived(Pageable pageable);

    CompetitionTreeResponse getCompetitionTree(Long competitionId);
}
