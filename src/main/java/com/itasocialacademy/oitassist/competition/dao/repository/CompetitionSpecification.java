package com.itasocialacademy.oitassist.competition.dao.repository;

import com.itasocialacademy.oitassist.competition.dao.dto.CompetitionFilter;
import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;

/**
 * Utility class providing JPA Specifications for filtering competitions.
 *
 * This class is used to build dynamic queries based on
 * {@link CompetitionFilter}. All methods are static and the class cannot be
 * instantiated.
 */
public class CompetitionSpecification {
    private CompetitionSpecification() {
    }

    /**
     * Creates a {@link Specification} for {@link Competition} entities based on the
     * provided {@link CompetitionFilter}.
     *
     * @param filter filter object containing search and filtering criteria
     * @return a JPA {@link Specification} representing the filter
     */
    public static Specification<Competition> filter(CompetitionFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // Add search by name (case-insensitive)
            addSearchPredicate(filter.search(), () -> predicates.add(
                cb.like(
                    cb.lower(root.get("name")),
                    "%" + filter.search().toLowerCase() + "%")));

            // Add filtering by level, status, year
            addPredicate(filter.level(), () -> predicates.add(cb.equal(root.get("level"), filter.level())));
            addPredicate(filter.status(),
                () -> predicates.add(cb.equal(root.get("competitionStatus"), filter.status())));
            addPredicate(filter.year(), () -> predicates.add(cb.equal(root.get("year"), filter.year())));

            // Combine all predicates with AND
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Helper method to conditionally add a search predicate if the search string is
     * not blank.
     *
     * @param value  the search string
     * @param action action to execute if the search string is present
     */
    private static void addSearchPredicate(String value, Runnable action) {
        Optional.ofNullable(value)
            .filter(s -> !s.isBlank())
            .ifPresent(search -> action.run());
    }

    /**
     * Helper method to conditionally add a predicate if the value is not null.
     *
     * @param value  value to check
     * @param action action to execute if the value is present
     */
    private static void addPredicate(Object value, Runnable action) {
        Optional.ofNullable(value)
            .ifPresent(v -> action.run());
    }
}
