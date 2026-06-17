package com.itasocialacademy.oitassist.news.service;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.AuthorizationException;
import com.itasocialacademy.oitassist.core.rest.service.AbstractServiceImpl;
import com.itasocialacademy.oitassist.filemanager.api.events.FilesAttachRequestedEvent;
import com.itasocialacademy.oitassist.filemanager.api.events.FilesDetachRequestedEvent;
import com.itasocialacademy.oitassist.filemanager.dao.enums.RelatedEntityType;
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
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class NewsServiceImpl
    extends AbstractServiceImpl<Long, News, CreateNewsDTO, UpdateNewsDto, ResponseNewsDto, NewsRepository, NewsMapper>
    implements NewsService {
    private final SecurityFacade securityFacade;
    private final ApplicationEventPublisher eventPublisher;

    protected NewsServiceImpl(NewsRepository repository, NewsMapper mapper, SecurityFacade securityFacade,
        ApplicationEventPublisher eventPublisher) {
        super(repository, mapper);
        this.securityFacade = securityFacade;
        this.eventPublisher = eventPublisher;
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
    @Transactional
    public ResponseNewsDto save(CreateNewsDTO dto) {
        ResponseNewsDto saved = super.save(dto);
        publishAttachEvent(saved.getId(), dto.getFileIds());
        return saved;
    }

    @Override
    @Transactional
    public ResponseNewsDto update(UpdateNewsDto dto) {
        ResponseNewsDto updated = super.update(dto);
        publishAttachEvent(updated.getId(), dto.getFileIds());
        publishDetachEvent(updated.getId(), dto.getRemovedFileIds());
        return updated;
    }

    @Override
    public Page<ResponseNewsListItemDto> getPublishedNews(Pageable pageable, String search, LocalDate date) {
        Specification<News> spec = NewsSpecification.withFilters(
            NewsStatus.PUBLISHED,
            search,
            date);
        return repository.findAll(spec, pageable).map(this::toNewsListItemDto);
    }

    private void publishAttachEvent(Long newsId, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }

        Long authorId = securityFacade.getCurrentUserId()
            .orElseThrow(() -> new AuthorizationException("User must be logged in to modify news",
                ErrorCode.ACCESS_DENIED));

        eventPublisher.publishEvent(
            new FilesAttachRequestedEvent(newsId, RelatedEntityType.NEWS, fileIds, authorId));
    }

    private void publishDetachEvent(Long newsId, List<Long> removedFileIds) {
        if (removedFileIds == null || removedFileIds.isEmpty()) {
            return;
        }

        Long userId = securityFacade.getCurrentUserId()
            .orElseThrow(() -> new AuthorizationException("User must be logged in to modify news",
                ErrorCode.ACCESS_DENIED));

        eventPublisher
            .publishEvent(new FilesDetachRequestedEvent(RelatedEntityType.NEWS, newsId, removedFileIds, userId));
    }

    private ResponseNewsListItemDto toNewsListItemDto(News news) {
        return new ResponseNewsListItemDto(
            news.getId(),
            news.getTitle(),
            buildPreview(news.getContent()),
            news.getPublishedAt());
    }

    private String buildPreview(String content) {
        if (content == null) {
            return null;
        }
        return content.length() > PREVIEWS_LENGTH ? content.substring(0, PREVIEWS_LENGTH) + "..." : content;
    }
}
