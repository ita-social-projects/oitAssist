package com.itasocialacademy.oitassist.competition.service.interfaces;

import com.itasocialacademy.oitassist.competition.dao.dto.request.CreateCompetitionRequest;
import com.itasocialacademy.oitassist.competition.dao.dto.response.CompetitionResponse;
import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionHierarchyValidationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CompetitionService {
    /**
     * Creates a new competition.
     *
     * @param request
     */
    CompetitionResponse create(CreateCompetitionRequest request);

    /**
     * Retrieve a competition by ID w/o checking access role.
     *
     * @param id
     */
    CompetitionResponse getById(Long id);

    /**
     * Retrieves details of a specific by ID. Visibility depends on user role.
     *
     * @param id Competition ID
     * @return {@link CompetitionResponse}
     */
    CompetitionResponse getVisibleById(Long id);

    /**
     * Checks whether it is allowed to change the hierarchy (add/remove stages and
     * tours).
     *
     * @param competitionId ID
     * @throws CompetitionHierarchyValidationException if changes are prohibited
     */
    void validateHierarchyImmutability(Long competitionId);

    /**
     * Transitions the competition to a new status. Publishing requires at least one
     * Stage and one Tour.
     *
     * @param id        Competition ID
     * @param newStatus a status of a Competition
     * @return {@link CompetitionResponse}
     */
    CompetitionResponse changeStatus(Long id, CompetitionStatus newStatus);

    Page<CompetitionResponse> getAllVisible(Pageable pageable);

    /**
     * Retrieves a paginated list of competitions that have been archived.
     *
     * @param pageable
     */
    Page<CompetitionResponse> getArchived(Pageable pageable);
}
