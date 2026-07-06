package com.itasocialacademy.oitassist.competition.api;

import com.itasocialacademy.oitassist.competition.api.dto.CompetitionDetail;
import com.itasocialacademy.oitassist.competition.api.dto.StageDetail;
import java.util.Optional;

/**
 * Read-only facade exposing minimal Competition/Stage/Tour lookups to other modules (e.g. {@code participation}).
 * Deliberately returns DTOs rather than JPA entities to keep {@code competition}'s persistence model private and to
 * ensure callers can't bypass the business rules enforced by {@code HierarchyValidator} /
 * {@code CompetitionServiceImpl}.
 */
public interface CompetitionFacade {
    Optional<CompetitionDetail> findCompetitionById(Long competitionId);

    Optional<StageDetail> findStageById(Long stageId);
}
