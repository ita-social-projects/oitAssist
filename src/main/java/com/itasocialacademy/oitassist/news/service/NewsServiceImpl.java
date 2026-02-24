package com.itasocialacademy.oitassist.news.service;

import com.itasocialacademy.oitassist.core.rest.service.AbstractServiceImpl;
import com.itasocialacademy.oitassist.news.dao.dto.request.CreateNewsDTO;
import com.itasocialacademy.oitassist.news.dao.dto.request.UpdateNewsDto;
import com.itasocialacademy.oitassist.news.dao.dto.response.ResponseNewsDto;
import com.itasocialacademy.oitassist.news.dao.enums.NewsStatus;
import com.itasocialacademy.oitassist.news.dao.model.News;
import com.itasocialacademy.oitassist.news.dao.repository.NewsRepository;
import com.itasocialacademy.oitassist.news.mapper.request.NewsMapper;
import com.itasocialacademy.oitassist.news.service.interfaces.NewsService;
import com.itasocialacademy.oitassist.user.api.dto.UserDetailsImpl;
import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NewsServiceImpl
    extends AbstractServiceImpl<Long, News, CreateNewsDTO, UpdateNewsDto, ResponseNewsDto, NewsRepository, NewsMapper>
    implements NewsService {
    protected NewsServiceImpl(NewsRepository repository, NewsMapper mapper) {
        super(repository, mapper);
    }

    @Override
    protected void beforeSave(News news, CreateNewsDTO newsDTO) {
        log.info("Creating news with title='{}'", newsDTO.getTitle());
        Long authorId = ((UserDetailsImpl) SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal())
            .getId();

        news.setAuthorId(authorId);
        news.setCreatedAt(OffsetDateTime.now());
        applyPublishLogic(news, newsDTO.isPublishNow());
    }

    @Override
    protected void beforeUpdate(News entity, UpdateNewsDto dto) {
        log.info("Updating news id={}", entity.getId());
        applyPublishLogic(entity, dto.isPublishNow());
    }

    private void applyPublishLogic(News news, boolean publishNow) {
        if (publishNow) {
            if (news.getStatus() != NewsStatus.PUBLISHED) {
                log.debug("Publishing news id={}", news.getId());
                news.setStatus(NewsStatus.PUBLISHED);
                news.setPublishedAt(OffsetDateTime.now());
            }
        } else {
            if (news.getStatus() == null || news.getStatus() == NewsStatus.PUBLISHED) {
                log.debug("Returning news id={} to draft", news.getId());
                news.setStatus(NewsStatus.DRAFT);
                news.setPublishedAt(null);
            }
        }
    }
}
