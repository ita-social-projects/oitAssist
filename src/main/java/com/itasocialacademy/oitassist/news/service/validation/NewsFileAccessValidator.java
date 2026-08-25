package com.itasocialacademy.oitassist.news.service.validation;

import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
import com.itasocialacademy.oitassist.filemanager.access.FileAccessValidator;
import com.itasocialacademy.oitassist.news.dao.enums.NewsStatus;
import com.itasocialacademy.oitassist.news.dao.model.News;
import com.itasocialacademy.oitassist.news.dao.repository.NewsRepository;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Validator that dictates the access rules for files attached to the NEWS
 * entity.
 */
@Component
@RequiredArgsConstructor
public class NewsFileAccessValidator implements FileAccessValidator {
    private final NewsRepository newsRepository;

    /**
     * {@inheritDoc}
     *
     * @return {@link RelatedEntityType#NEWS}
     */
    @Override
    public RelatedEntityType getEntityType() {
        return RelatedEntityType.NEWS;
    }

    /**
     * Checks if the user is authorized to access the file attached to the specified news entity.
     * Access is granted to everyone (including guests) if the news is {@link NewsStatus#PUBLISHED}.
     * For non-published news (drafts/archived), access is restricted to the news author, ADMIN, or ORG roles.
     *
     * @param newsId  the ID of the news entity
     * @param userId  the ID of the current user, or {@code null} if unauthenticated
     * @param hasRole predicate to check current user roles
     * @return true if access is permitted, false otherwise
     */
    @Override
    public boolean canAccess(Long newsId, Long userId, Predicate<String> hasRole) {
        News news = newsRepository.findById(newsId).orElse(null);
        if (news == null) {
            return false;
        }
        if (news.getStatus() == NewsStatus.PUBLISHED) {
            return true;
        }
        return hasRole.test("ADMIN") || hasRole.test("ORG")
            || (userId != null && userId.equals(news.getAuthorId()));
    }
}
