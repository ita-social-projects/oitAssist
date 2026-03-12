package com.itasocialacademy.oitassist.competition.dao.repository;

import com.itasocialacademy.oitassist.competition.dao.dto.CompetitionFilter;
import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;

public class CompetitionSpecification {
    private CompetitionSpecification() {
    }

    public static Specification<Competition> filter(CompetitionFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            addSearchPredicate(filter.search(), () -> predicates.add(
                cb.like(
                    cb.lower(root.get("name")),
                    "%" + filter.search().toLowerCase() + "%")));

            addPredicate(filter.level(), () -> predicates.add(cb.equal(root.get("level"), filter.level())));
            addPredicate(filter.status(),
                () -> predicates.add(cb.equal(root.get("competitionStatus"), filter.status())));
            addPredicate(filter.year(), () -> predicates.add(cb.equal(root.get("year"), filter.year())));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addSearchPredicate(String value, Runnable action) {
        Optional.ofNullable(value)
            .filter(s -> !s.isBlank())
            .ifPresent(search -> action.run());
    }

    private static void addPredicate(Object value, Runnable action) {
        Optional.ofNullable(value)
            .ifPresent(v -> action.run());
    }
}
