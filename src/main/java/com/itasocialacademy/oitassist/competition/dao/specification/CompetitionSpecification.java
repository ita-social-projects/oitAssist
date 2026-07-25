package com.itasocialacademy.oitassist.competition.dao.specification;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class CompetitionSpecification {
    public static final String COMPETITION_STATUS = "competitionStatus";
    public static final String TITLE = "title";
    public static final String DATE_START = "dateStart";
    public static final String DATE_FINISH = "dateFinish";

    private CompetitionSpecification() {
    }

    /**
     * For public viewing (Role USER). Only published or finished Competitions are visible.
     */
    public static Specification<Competition> isVisibleToUser() {
        return (root, query, cb) -> root.get(COMPETITION_STATUS)
            .in(CompetitionStatus.ENROLLMENT, CompetitionStatus.PUBLISHED, CompetitionStatus.FINISHED);
    }

    /**
     * For administrators (Role ADMIN or ORG). All Competitions are visible except archived.
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

    public static Specification<Competition> hasTitle(String title) {
        return (root, query, cb) -> {
            if (title == null || title.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get(TITLE)), "%" + title.toLowerCase() + "%");
        };
    }

    public static Specification<Competition> startsAfterOrEqual(ZonedDateTime dateStart) {
        return (root, query, cb) -> {
            if (dateStart == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get(DATE_START), dateStart);
        };
    }

    public static Specification<Competition> finishesBeforeOrEqual(ZonedDateTime dateFinish) {
        return (root, query, cb) -> {
            if (dateFinish == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get(DATE_FINISH), dateFinish);
        };
    }

    public static Specification<Competition> hasAnyStatus(List<CompetitionStatus> statuses) {
        return (root, query, cb) -> {
            if (statuses == null || statuses.isEmpty()) {
                return cb.conjunction();
            }
            return root.get(COMPETITION_STATUS).in(statuses);
        };
    }
}
