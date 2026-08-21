package com.itasocialacademy.oitassist.competition.service.interfaces;

import com.itasocialacademy.oitassist.competition.dto.filter.CompetitionSearchFilter;
import com.itasocialacademy.oitassist.competition.dto.request.ChangeCompetitionStatusRequest;
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
     * Transitions the competition to a new status. Publishing (ENROLLMENT or
     * PUBLISHED) requires at least one Stage and one Tour; finishing (FINISHED)
     * additionally requires every Stage to be completed. The request's
     * {@code version} must match the competition's current version, or the
     * transition is rejected as a stale-version conflict.
     *
     * @param competitionId Competition ID
     * @param request       the target status and the expected current version
     * @return {@link CompetitionResponse}
     */
    CompetitionResponse changeStatus(Long competitionId, ChangeCompetitionStatusRequest request);

    /**
     * Retrieves a paginated list of competitions visible to the current user,
     * filtered by {@code filter}. Visibility depends on user role:
     * <li>a plain USER sees only competitions open for enrollment, published, or
     * finished;</li>
     * <li>ADMIN and ORG additionally see DRAFT competitions, including DRAFTs
     * created by other organizations. Archived competitions are never returned here
     * — see {@link #getArchived(CompetitionSearchFilter, Pageable)}.</li>
     *
     * @param filter   search/date-range/status criteria to narrow the results
     * @param pageable pagination and sorting
     * @return a page of {@link CompetitionResponse}
     */
    Page<CompetitionResponse> getAllVisible(CompetitionSearchFilter filter, Pageable pageable);

    /**
     * Retrieves a paginated list of competitions that have been archived, filtered
     * by {@code filter}.
     *
     * @param filter   search/date-range/status criteria to narrow the results
     * @param pageable pagination and sorting
     * @return a page of {@link CompetitionResponse}
     */
    Page<CompetitionResponse> getArchived(CompetitionSearchFilter filter, Pageable pageable);

    /**
     * Retrieves the competition together with its full hierarchy of stages and
     * tours in a single nested response. Access to a DRAFT competition's tree
     * follows the same visibility rules as {@link #getVisibleById(Long)}.
     *
     * @param competitionId Competition ID
     * @return {@link CompetitionTreeResponse}
     */
    CompetitionTreeResponse getCompetitionTree(Long competitionId);
}
