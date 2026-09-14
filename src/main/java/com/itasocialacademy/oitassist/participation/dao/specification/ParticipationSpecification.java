package com.itasocialacademy.oitassist.participation.dao.specification;

import com.itasocialacademy.oitassist.participation.dao.model.Participation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ParticipationSpecification {
    public static Specification<Participation> hasCompetitionAndStage(Long competitionId, Long stageId) {
        return (root, query, cb) -> cb.and(
            cb.equal(root.get("competitionId"), competitionId),
            cb.equal(root.get("stageId"), stageId));
    }

    public static Specification<Participation> userIdIn(List<Long> userIds) {
        return (root, query, cb) -> userIds == null
            ? cb.conjunction()
            : root.get("userId").in(userIds);
    }
}
