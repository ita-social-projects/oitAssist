package com.itasocialacademy.oitassist.competition.dao.specification;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import org.springframework.data.jpa.domain.Specification;

public final class CompetitionSpecification {
    public static final String COMPETITION_STATUS = "competitionStatus";

    private CompetitionSpecification() {
    }

    /**
     * For public viewing (Role USER). Only published or finished Competitions are
     * visible.
     */
    public static Specification<Competition> isVisibleToUser() {
        return (root, query, cb) -> root.get(COMPETITION_STATUS)
            .in(CompetitionStatus.ENROLLMENT, CompetitionStatus.PUBLISHED, CompetitionStatus.FINISHED);
    }

    /**
     * For administrators (Role ADMIN or ORG). All Competitions are visible except
     * archived.
     */
    public static Specification<Competition> isVisibleToAdminOrOrg() {
        return (root, query, cb) -> root.get(COMPETITION_STATUS)
            .in(CompetitionStatus.DRAFT, CompetitionStatus.ENROLLMENT, CompetitionStatus.PUBLISHED,
                CompetitionStatus.FINISHED);
    }

    /**
     * For a separate archive endpoint.
     */
    public static Specification<Competition> isArchived() {
        return (root, query, cb) -> cb.equal(root.get(COMPETITION_STATUS), CompetitionStatus.ARCHIVED);
    }
}
