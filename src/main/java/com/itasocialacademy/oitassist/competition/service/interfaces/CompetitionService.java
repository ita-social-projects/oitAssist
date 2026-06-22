package com.itasocialacademy.oitassist.competition.service.interfaces;

import com.itasocialacademy.oitassist.competition.dao.dto.request.CreateCompetitionRequest;
import com.itasocialacademy.oitassist.competition.dao.dto.response.CompetitionResponse;
import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionHierarchyValidationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CompetitionService {
    CompetitionResponse create(CreateCompetitionRequest request);

    CompetitionResponse getById(Long id);
    CompetitionResponse getVisibleById(Long id);

    /**
     * Checks whether it is allowed to change the hierarchy (add/remove stages and tours).
     *
     * @param competitionId ID
     * @throws CompetitionHierarchyValidationException if changes are prohibited
     */
    void validateHierarchyImmutability(Long competitionId);

    CompetitionResponse changeStatus(Long id, CompetitionStatus newStatus);

    Page<CompetitionResponse> getAllVisible(Pageable pageable);

    Page<CompetitionResponse> getArchived(Pageable pageable);
}
