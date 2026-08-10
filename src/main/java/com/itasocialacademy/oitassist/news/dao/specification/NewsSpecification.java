package com.itasocialacademy.oitassist.news.dao.specification;

import com.itasocialacademy.oitassist.news.dao.enums.NewsStatus;
import com.itasocialacademy.oitassist.news.dao.model.News;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NewsSpecification {
    public static Specification<News> withFilters(NewsStatus status, String search, LocalDate date) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("status"), status));
            addSearchPredicate(predicates, root, cb, search);

            if (date != null) {
                Expression<LocalDate> publishedDate =
                    cb.function("DATE", LocalDate.class, root.get("publishedAt"));
                predicates.add(cb.equal(publishedDate, date));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<News> withAdminFilters(String search, List<NewsStatus> statuses, LocalDate dateFrom,
        LocalDate dateTo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            addSearchPredicate(predicates, root, cb, search);

            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }

            if (dateFrom != null) {
                OffsetDateTime startOfDay = dateFrom.atStartOfDay().atZone(java.time.ZoneOffset.UTC).toOffsetDateTime();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startOfDay));
            }

            if (dateTo != null) {
                OffsetDateTime endOfDay =
                    dateTo.plusDays(1).atStartOfDay().atZone(java.time.ZoneOffset.UTC).toOffsetDateTime();
                predicates.add(cb.lessThan(root.get("createdAt"), endOfDay));
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addSearchPredicate(
        List<Predicate> predicates, Root<News> root, CriteriaBuilder cb, String search) {
        if (search == null || search.isBlank()) {
            return;
        }
        String like = "%" + search.toLowerCase(Locale.ROOT) + "%";
        Predicate title = cb.like(cb.lower(root.get("title")), like);
        Predicate content = cb.like(cb.lower(root.get("content")), like);
        predicates.add(cb.or(title, content));
    }
}
