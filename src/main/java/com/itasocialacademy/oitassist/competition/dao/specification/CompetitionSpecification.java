package com.itasocialacademy.oitassist.competition.dao.specification;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor
public class CompetitionSpecification {
    /**
     * For public viewing (Role USER). Only published or finished Competitions are visible.
     */
    public static Specification<Competition> isVisibleToUser() {
        return (root, query, cb) -> root.get("competitionStatus")
            .in(CompetitionStatus.PUBLISHED, CompetitionStatus.FINISHED);
    }

    /**
     * For administrators (Role ADMIN). All Competitions are visible except archived.
     */
    public static Specification<Competition> isVisibleToAdmin() {
        return (root, query, cb) -> root.get("competitionStatus")
            .in(CompetitionStatus.DRAFT, CompetitionStatus.PUBLISHED, CompetitionStatus.FINISHED);
    }

    /**
     * For organizers (Role ORG). Published or finished Competitions are visible. Also, self-created (DRAFT) competitions
     * are visible.
     */
    public static Specification<Competition> isVisibleToOrg(Long currentUserId) {
        return (root, query, cb) -> cb.or(
            root.get("competitionStatus").in(CompetitionStatus.PUBLISHED, CompetitionStatus.FINISHED),
            cb.and(
                cb.equal(root.get("competitionStatus"), CompetitionStatus.DRAFT),
                cb.equal(root.get("createdBy"), currentUserId)
            )
        );
    }

    /**
     * For a separate archive endpoint.
     */
    public static Specification<Competition> isArchived() {
        return (root, query, cb) -> cb.equal(root.get("competitionStatus"), CompetitionStatus.ARCHIVED);
    }
}
