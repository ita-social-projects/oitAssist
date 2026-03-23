package com.itasocialacademy.oitassist.news.dao.specification;

import com.itasocialacademy.oitassist.news.dao.enums.NewsStatus;
import com.itasocialacademy.oitassist.news.dao.model.News;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class NewsSpecification {
    public static Specification<News> withFilters(NewsStatus status, String search, LocalDate date) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("status"), status));

            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase() + "%";
                Predicate title = cb.like(cb.lower(root.get("title")), like);
                Predicate content = cb.like(cb.lower(root.get("content")), like);
                predicates.add(cb.or(title, content));
            }

            if (date != null) {
                Expression<LocalDate> publishedDate =
                    cb.function("DATE", LocalDate.class, root.get("publishedAt"));
                predicates.add(cb.equal(publishedDate, date));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
