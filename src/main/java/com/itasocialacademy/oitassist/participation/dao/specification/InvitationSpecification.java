package com.itasocialacademy.oitassist.participation.dao.specification;

import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import com.itasocialacademy.oitassist.participation.dao.model.Invitation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InvitationSpecification {
    public static Specification<Invitation> hasCompetitionAndStage(Long competitionId, Long stageId) {
        return (root, query, cb) -> cb.and(
            cb.equal(root.get("competitionId"), competitionId),
            cb.equal(root.get("stageId"), stageId));
    }

    public static Specification<Invitation> studentIdIn(List<Long> userIds) {
        return (root, query, cb) -> userIds == null
            ? cb.conjunction()
            : root.get("studentId").in(userIds);
    }

    public static Specification<Invitation> hasStatus(RequestStatus status) {
        return (root, query, cb) -> cb.and(
            cb.equal(root.get("status"), status));
    }
}
