package com.itasocialacademy.oitassist.participation.dao.specification;

import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import com.itasocialacademy.oitassist.participation.dao.model.Application;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApplicationSpecification {
    public static Specification<Application> hasCompetitionAndStage(Long competitionId, Long stageId) {
        return (root, query, cb) -> cb.and(
            cb.equal(root.get("competitionId"), competitionId),
            cb.equal(root.get("stageId"), stageId));
    }

    public static Specification<Application> userIdIn(List<Long> userIds) {
        return (root, query, cb) -> userIds == null
            ? cb.conjunction()
            : root.get("issuedBy").in(userIds);
    }

    public static Specification<Application> hasStatus(RequestStatus status) {
        return (root, query, cb) -> cb.and(
            cb.equal(root.get("status"), status));
    }
}
