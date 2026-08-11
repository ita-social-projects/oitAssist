package com.itasocialacademy.oitassist.competition.dao.specification;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA {@link Specification} building blocks for querying {@link Competition}
 * entities. Individual specifications are meant to be composed via
 * {@link Specification#and(Specification)} in {@code CompetitionServiceImpl}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CompetitionSpecification {
    private static final String COMPETITION_STATUS = "competitionStatus";
    private static final String TITLE = "title";
    private static final String DATE_START = "dateStart";
    private static final String DATE_FINISH = "dateFinish";

    private static final char ESCAPE_CHAR = '\\';

    /**
     * For public viewing (Role USER). Only competitions that are open for
     * enrollment, published, or finished are visible. Archived competitions are
     * intentionally excluded here — they are exposed only via the separate archive
     * endpoint (see {@link #isArchived()}).
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
     * For a separate archive endpoint. Matches only competitions in
     * {@link CompetitionStatus#ARCHIVED}.
     */
    public static Specification<Competition> isArchived() {
        return (root, query, cb) -> cb.equal(root.get(COMPETITION_STATUS), CompetitionStatus.ARCHIVED);
    }

    /**
     * Case-insensitive substring match on the competition title. No restriction is
     * applied when {@code title} is {@code null} or blank.
     *
     * @param title free-text fragment to search for within the competition title
     */
    public static Specification<Competition> hasTitle(String title) {
        return (root, query, cb) -> {
            if (title == null || title.isBlank()) {
                return cb.conjunction();
            }
            String escaped = title.toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
            return cb.like(cb.lower(root.get(TITLE)), "%" + escaped + "%", ESCAPE_CHAR);
        };
    }

    /**
     * Part of the date-range overlap filter: excludes competitions that finished
     * before the given {@code dateStart}. Paired with
     * {@link #startsBeforeOrEqual(ZonedDateTime)} (using
     * {@code filter.dateFinish()}), this yields standard interval-overlap semantics
     * — i.e. any competition whose own date range intersects the requested
     * [{@code filter.dateStart()}, {@code filter.dateFinish()}] window, not just
     * competitions fully contained within it. No restriction is applied when
     * {@code dateStart} is {@code null}.
     *
     * @param dateStart lower bound of the requested date range
     *                  ({@code filter.dateStart()})
     */
    public static Specification<Competition> finishesAfterOrEqual(ZonedDateTime dateStart) {
        return (root, query, cb) -> {
            if (dateStart == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get(DATE_FINISH), dateStart);
        };
    }

    /**
     * Part of the date-range overlap filter: excludes competitions that start after
     * the given {@code dateFinish}. Paired with
     * {@link #finishesAfterOrEqual(ZonedDateTime)} (using
     * {@code filter.dateStart()}), this yields standard interval-overlap semantics.
     * No restriction is applied when {@code dateFinish} is {@code null}.
     *
     * @param dateFinish upper bound of the requested date range
     *                   ({@code filter.dateFinish()})
     */
    public static Specification<Competition> startsBeforeOrEqual(ZonedDateTime dateFinish) {
        return (root, query, cb) -> {
            if (dateFinish == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get(DATE_START), dateFinish);
        };
    }

    /**
     * Restricts results to the given set of statuses. No restriction is applied
     * when {@code statuses} is {@code null} or empty. Combined via AND with the
     * caller's role-based visibility specification, so this can only narrow — never
     * widen — what a given role is allowed to see.
     *
     * @param statuses statuses to filter by, or {@code null}/empty for no
     *                 restriction
     */
    public static Specification<Competition> hasAnyStatus(List<CompetitionStatus> statuses) {
        return (root, query, cb) -> {
            if (statuses == null || statuses.isEmpty()) {
                return cb.conjunction();
            }
            return root.get(COMPETITION_STATUS).in(statuses);
        };
    }
}
