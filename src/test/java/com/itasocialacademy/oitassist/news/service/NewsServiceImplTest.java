package com.itasocialacademy.oitassist.news.service;

import com.itasocialacademy.oitassist.news.dao.dto.request.CreateNewsDTO;
import com.itasocialacademy.oitassist.news.dao.dto.request.UpdateNewsDto;
import com.itasocialacademy.oitassist.news.dao.enums.NewsStatus;
import com.itasocialacademy.oitassist.news.dao.model.News;
import com.itasocialacademy.oitassist.news.dao.repository.NewsRepository;
import com.itasocialacademy.oitassist.news.mapper.request.NewsMapper;
import com.itasocialacademy.oitassist.user.api.dto.UserDetailsImpl;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsServiceImplTest {
    @Mock
    private NewsMapper newsMapper;
    @Mock
    private NewsRepository newsRepository;
    @InjectMocks
    private NewsServiceImpl newsService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateDraftNewsAndSave() {
        mockAuthenticatedUser(1L);
        CreateNewsDTO dto = new CreateNewsDTO("Title", "Content", false);

        News news = new News();
        when(newsMapper.toEntity(dto)).thenReturn(news);

        newsService.save(dto);

        assertEquals(1L, news.getAuthorId());
        assertEquals(NewsStatus.DRAFT, news.getStatus());
        assertNull(news.getPublishedAt());
        verify(newsRepository).save(news);
    }

    @Test
    void shouldCreatePublishedNewsAndSave() {
        mockAuthenticatedUser(1L);
        CreateNewsDTO dto = new CreateNewsDTO("Title", "Content", true);

        News news = new News();
        when(newsMapper.toEntity(dto)).thenReturn(news);

        newsService.save(dto);

        assertEquals(1L, news.getAuthorId());
        assertEquals(NewsStatus.PUBLISHED, news.getStatus());
        assertNotNull(news.getPublishedAt());

        verify(newsRepository).save(news);
    }

    @Test
    void shouldReturnPublishedNewsToDraftOnUpdate() {

        UpdateNewsDto dto = new UpdateNewsDto(1L, "Title", "Content", false);

        News existing = new News();
        existing.setId(1L);
        existing.setStatus(NewsStatus.PUBLISHED);
        existing.setPublishedAt(OffsetDateTime.now());

        when(newsRepository.findById(1L)).thenReturn(Optional.of(existing));

        newsService.update(dto);

        assertEquals(NewsStatus.DRAFT, existing.getStatus());
        assertNull(existing.getPublishedAt());

        verify(newsRepository).save(existing);
    }

    @Test
    void shouldPublishDraftNewsOnUpdate() {

        UpdateNewsDto dto = new UpdateNewsDto(1L, "Title", "Content", true);

        News existing = new News();
        existing.setId(1L);
        existing.setStatus(NewsStatus.DRAFT);

        when(newsRepository.findById(1L)).thenReturn(Optional.of(existing));

        newsService.update(dto);

        assertEquals(NewsStatus.PUBLISHED, existing.getStatus());
        assertNotNull(existing.getPublishedAt());

        verify(newsRepository).save(existing);
    }

    @Test
    void shouldNotOverwritePublishedDateIfAlreadyPublished() {

        OffsetDateTime originalDate = OffsetDateTime.now().minusDays(1);

        UpdateNewsDto dto = new UpdateNewsDto(1L, "Title", "Content", true);

        News existing = new News();
        existing.setId(1L);
        existing.setStatus(NewsStatus.PUBLISHED);
        existing.setPublishedAt(originalDate);

        when(newsRepository.findById(1L)).thenReturn(Optional.of(existing));

        newsService.update(dto);

        assertEquals(NewsStatus.PUBLISHED, existing.getStatus());
        assertEquals(originalDate, existing.getPublishedAt());

        verify(newsRepository).save(existing);
    }

    private void mockAuthenticatedUser(Long userId) {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        UserDetailsImpl user = mock(UserDetailsImpl.class);

        when(user.getId()).thenReturn(userId);
        when(authentication.getPrincipal()).thenReturn(user);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }
}