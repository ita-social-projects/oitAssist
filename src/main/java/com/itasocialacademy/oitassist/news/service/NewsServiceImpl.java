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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class NewsServiceImpl
    extends AbstractServiceImpl<Long, News, CreateNewsDTO, UpdateNewsDto, ResponseNewsDto, NewsRepository, NewsMapper>
    implements NewsService {
    protected NewsServiceImpl(NewsRepository repository, NewsMapper mapper) {
        super(repository, mapper);
    }

    @Override
    public ResponseNewsDto save(CreateNewsDTO newsDTO) {
        News news = mapper.toEntity(newsDTO);
        Long authorId = ((UserDetailsImpl) SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal())
            .getId();
        news.setAuthorId(authorId);
        if (newsDTO.isPublishNow()) {
            news.setStatus(NewsStatus.PUBLISHED);
            news.setPublishedAt(OffsetDateTime.now());
        } else {
            news.setStatus(NewsStatus.DRAFT);
            news.setPublishedAt(null);
        }
        return mapper.toDTO(repository.save(news));
    }
}
