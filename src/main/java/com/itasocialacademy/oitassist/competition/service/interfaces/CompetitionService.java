package com.itasocialacademy.oitassist.competition.service.interfaces;

import com.itasocialacademy.oitassist.competition.dao.dto.request.CreateCompetitionRequest;
import com.itasocialacademy.oitassist.competition.dao.dto.response.CompetitionResponse;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionHierarchyValidationException;

public interface CompetitionService {
    CompetitionResponse create(CreateCompetitionRequest request);

    CompetitionResponse getById(Long id);

    /**
     * Checks whether it is allowed to change the hierarchy (add/remove stages and tours).
     *
     * @param competitionId
     * @throws CompetitionHierarchyValidationException if changes are prohibited
     */
    void validateHierarchyImmutability(Long competitionId);
}
