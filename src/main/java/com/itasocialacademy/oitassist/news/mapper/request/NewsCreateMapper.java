package com.itasocialacademy.oitassist.news.mapper.request;

import com.itasocialacademy.oitassist.news.dao.dto.request.CreateNewsDTO;
import com.itasocialacademy.oitassist.news.dao.enums.NewsStatus;
import com.itasocialacademy.oitassist.news.dao.model.News;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

@Component
public class NewsCreateMapper {
    public News create(CreateNewsDTO dto, Long authorId) {
        News news = new News();
        news.setTitle(dto.getTitle());
        news.setContent(dto.getContent());
        news.setAuthorId(authorId);
        news.setCreatedAt(OffsetDateTime.now());
        if (dto.isPublishNow()) {
            news.setStatus(NewsStatus.PUBLISHED);
            news.setPublishedAt(OffsetDateTime.now());
        } else {
            news.setStatus(NewsStatus.DRAFT);
            news.setPublishedAt(null);
        }
        return news;
    }
}
