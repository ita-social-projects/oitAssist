package com.itasocialacademy.oitassist.news.service;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.rest.service.AbstractServiceImpl;
import com.itasocialacademy.oitassist.news.dao.dto.request.CreateNewsDTO;
import com.itasocialacademy.oitassist.news.dao.dto.request.UpdateNewsDto;
import com.itasocialacademy.oitassist.news.dao.dto.response.ResponseNewsDto;
import com.itasocialacademy.oitassist.news.dao.dto.response.ResponseNewsListItemDto;
import com.itasocialacademy.oitassist.news.dao.enums.NewsStatus;
import com.itasocialacademy.oitassist.news.dao.model.News;
import com.itasocialacademy.oitassist.news.dao.repository.NewsRepository;
import com.itasocialacademy.oitassist.news.dao.specification.NewsSpecification;
import com.itasocialacademy.oitassist.news.mapper.request.NewsMapper;
import com.itasocialacademy.oitassist.news.service.interfaces.NewsService;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NewsServiceImpl
    extends AbstractServiceImpl<Long, News, CreateNewsDTO, UpdateNewsDto, ResponseNewsDto, NewsRepository, NewsMapper>
    implements NewsService {
    private final SecurityFacade securityFacade;

    protected NewsServiceImpl(NewsRepository repository, NewsMapper mapper, SecurityFacade securityFacade) {
        super(repository, mapper);
        this.securityFacade = securityFacade;
    }

    private static final int PREVIEWS_LENGTH = 300;

    @Override
    protected void beforeSave(News news, CreateNewsDTO newsDTO) {
        log.info("Creating news with title='{}'", newsDTO.getTitle());

        Long authorId = securityFacade.getCurrentUserId()
            .orElseThrow(() -> new AuthorizationException("User must be logged in to create news",
                ErrorCode.ACCESS_DENIED));

        news.setAuthorId(authorId);
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

    @Override
    public Page<ResponseNewsListItemDto> getPublishedNews(Pageable pageable, String search, LocalDate date) {
        Specification<News> spec = NewsSpecification.withFilters(
            NewsStatus.PUBLISHED,
            search,
            date);
        return repository.findAll(spec, pageable).map(this::toNewsListItemDto);
    }

    private ResponseNewsListItemDto toNewsListItemDto(News news) {
        return new ResponseNewsListItemDto(
            news.getId(),
            news.getTitle(),
            buildPreview(news.getContent()),
            news.getPublishedAt(),
            news.getArchivedAt());
    }

    private String buildPreview(String content) {
        if (content == null) {
            return null;
        }
        return content.length() > PREVIEWS_LENGTH ? content.substring(0, PREVIEWS_LENGTH) + "..." : content;
    }
}
