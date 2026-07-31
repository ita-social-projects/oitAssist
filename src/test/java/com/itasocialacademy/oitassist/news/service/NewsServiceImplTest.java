package com.itasocialacademy.oitassist.news.service;

import com.itasocialacademy.oitassist.news.dao.dto.request.CreateNewsDTO;
import com.itasocialacademy.oitassist.news.dao.dto.request.UpdateNewsDto;
import com.itasocialacademy.oitassist.news.dao.dto.response.ResponseNewsAdminListItemDto;
import com.itasocialacademy.oitassist.news.dao.dto.response.ResponseNewsDto;
import com.itasocialacademy.oitassist.news.dao.dto.response.ResponseNewsListItemDto;
import com.itasocialacademy.oitassist.news.dao.enums.NewsStatus;
import com.itasocialacademy.oitassist.news.dao.model.News;
import com.itasocialacademy.oitassist.news.dao.repository.NewsRepository;
import com.itasocialacademy.oitassist.news.mapper.request.NewsMapper;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsServiceImplTest {
    @Mock
    private NewsMapper newsMapper;
    @Mock
    private NewsRepository newsRepository;
    @Mock
    private SecurityFacade securityFacade;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private NewsServiceImpl newsService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void setUp() {
        newsService = new NewsServiceImpl(newsRepository, newsMapper, securityFacade, eventPublisher);
    }

    @Test
    void shouldCreateDraftNewsAndSave() {
        mockAuthenticatedUser(1L);
        CreateNewsDTO dto = new CreateNewsDTO("Title", "Content", false, List.of(1L));

        News news = new News();
        when(newsMapper.toEntity(dto)).thenReturn(news);
        when(newsRepository.save(news)).thenReturn(news);

        ResponseNewsDto response = new ResponseNewsDto();
        response.setId(1L);
        when(newsMapper.toDto(news)).thenReturn(response);

        newsService.save(dto);

        assertEquals(1L, news.getAuthorId());
        assertEquals(NewsStatus.DRAFT, news.getStatus());
        assertNull(news.getPublishedAt());
        verify(newsRepository).save(news);
    }

    @Test
    void shouldCreatePublishedNewsAndSave() {
        mockAuthenticatedUser(1L);
        CreateNewsDTO dto = new CreateNewsDTO("Title", "Content", true, null);

        News news = new News();
        when(newsMapper.toEntity(dto)).thenReturn(news);
        when(newsRepository.save(news)).thenReturn(news);

        ResponseNewsDto response = new ResponseNewsDto();
        response.setId(1L);
        when(newsMapper.toDto(news)).thenReturn(response);

        newsService.save(dto);

        assertEquals(1L, news.getAuthorId());
        assertEquals(NewsStatus.PUBLISHED, news.getStatus());
        assertNotNull(news.getPublishedAt());

        verify(newsRepository).save(news);
    }

    @Test
    void shouldReturnPublishedNewsToDraftOnUpdate() {
        mockAuthenticatedUser(1L);
        UpdateNewsDto dto = new UpdateNewsDto(1L, "Title", "Content", false, List.of(1L, 2L), List.of());

        News existing = new News();
        existing.setId(1L);
        existing.setStatus(NewsStatus.PUBLISHED);
        existing.setPublishedAt(OffsetDateTime.now());

        when(newsRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(newsRepository.save(existing)).thenReturn(existing);

        ResponseNewsDto response = new ResponseNewsDto();
        response.setId(1L);
        when(newsMapper.toDto(existing)).thenReturn(response);

        newsService.update(dto);

        assertEquals(NewsStatus.DRAFT, existing.getStatus());
        assertNull(existing.getPublishedAt());
        verify(newsRepository).save(existing);
    }

    @Test
    void shouldPublishDraftNewsOnUpdate() {
        mockAuthenticatedUser(1L);
        UpdateNewsDto dto = new UpdateNewsDto(1L, "Title", "Content", true, List.of(1L, 2L), List.of());

        News existing = new News();
        existing.setId(1L);
        existing.setStatus(NewsStatus.DRAFT);

        when(newsRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(newsRepository.save(existing)).thenReturn(existing);

        ResponseNewsDto response = new ResponseNewsDto();
        response.setId(1L);
        when(newsMapper.toDto(existing)).thenReturn(response);

        newsService.update(dto);

        assertEquals(NewsStatus.PUBLISHED, existing.getStatus());
        assertNotNull(existing.getPublishedAt());
        verify(newsRepository).save(existing);
    }

    @Test
    void shouldNotOverwritePublishedDateIfAlreadyPublished() {
        mockAuthenticatedUser(1L);
        OffsetDateTime originalDate = OffsetDateTime.now().minusDays(1);
        UpdateNewsDto dto = new UpdateNewsDto(1L, "Title", "Content", true, List.of(1L, 2L), List.of());

        News existing = new News();
        existing.setId(1L);
        existing.setStatus(NewsStatus.PUBLISHED);
        existing.setPublishedAt(originalDate);

        when(newsRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(newsRepository.save(existing)).thenReturn(existing);

        ResponseNewsDto response = new ResponseNewsDto();
        response.setId(1L);
        when(newsMapper.toDto(existing)).thenReturn(response);

        newsService.update(dto);

        assertEquals(NewsStatus.PUBLISHED, existing.getStatus());
        assertEquals(originalDate, existing.getPublishedAt());
        verify(newsRepository).save(existing);
    }

    @Test
    void shouldReturnMappedPublishedNewsPage() {
        Pageable pageable = PageRequest.of(0, 5);

        News news = new News();

        ResponseNewsListItemDto dto = ResponseNewsListItemDto.builder()
            .id(1L)
            .title("Published news title")
            .contentPreview("Short content")
            .publishedAt(OffsetDateTime.parse("2026-03-12T13:44:56Z"))
            .build();

        Page<News> newsPage = new PageImpl<>(List.of(news), pageable, 1);

        when(newsRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(newsPage);
        when(newsMapper.toListItemDto(news)).thenReturn(dto);

        Page<ResponseNewsListItemDto> result = newsService.getPublishedNews(pageable, null, null);

        assertThat(result.getContent()).hasSize(1);

        ResponseNewsListItemDto item = result.getContent().getFirst();
        assertThat(item.getId()).isEqualTo(1L);
        assertThat(item.getTitle()).isEqualTo("Published news title");
        assertThat(item.getContentPreview()).isEqualTo("Short content");
        assertThat(item.getPublishedAt()).isEqualTo(OffsetDateTime.parse("2026-03-12T13:44:56Z"));

        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(newsRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void shouldTrimContentPreviewWhenContentIsLong() {
        Pageable pageable = PageRequest.of(0, 5);
        News news = new News();

        String expectedPreview = "a".repeat(300) + "...";
        ResponseNewsListItemDto dto = ResponseNewsListItemDto.builder()
            .id(2L)
            .contentPreview(expectedPreview)
            .build();

        Page<News> newsPage = new PageImpl<>(List.of(news), pageable, 1);

        when(newsRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(newsPage);
        when(newsMapper.toListItemDto(news)).thenReturn(dto);

        Page<ResponseNewsListItemDto> result = newsService.getPublishedNews(pageable, null, null);

        assertThat(result.getContent()).containsExactly(dto);

        verify(newsRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void shouldReturnNullPreviewWhenContentIsNull() {
        Pageable pageable = PageRequest.of(0, 5);
        News news = new News();

        ResponseNewsListItemDto dto = ResponseNewsListItemDto.builder()
            .id(3L)
            .contentPreview(null)
            .build();

        Page<News> newsPage = new PageImpl<>(List.of(news), pageable, 1);

        when(newsRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(newsPage);
        when(newsMapper.toListItemDto(news)).thenReturn(dto);

        Page<ResponseNewsListItemDto> result = newsService.getPublishedNews(pageable, null, null);

        assertThat(result.getContent()).containsExactly(dto);

        verify(newsRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void shouldReturnAllNewsForAdminWithAllStatuses() {
        Pageable pageable = PageRequest.of(0, 15);

        News news = new News();

        ResponseNewsAdminListItemDto dto = ResponseNewsAdminListItemDto.builder()
            .id(1L)
            .title("Admin news title")
            .contentPreview("Short preview")
            .status(NewsStatus.PUBLISHED)
            .publishedAt(OffsetDateTime.parse("2026-03-15T10:30:00Z"))
            .archivedAt(null)
            .build();

        Page<News> newsPage = new PageImpl<>(List.of(news), pageable, 1);

        when(newsRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(newsPage);
        when(newsMapper.toAdminListItemDto(news)).thenReturn(dto);

        Page<ResponseNewsAdminListItemDto> result = newsService.getAllNewsForAdmin(pageable, null);

        assertThat(result.getContent()).hasSize(1);

        ResponseNewsAdminListItemDto item = result.getContent().getFirst();
        assertThat(item.getId()).isEqualTo(1L);
        assertThat(item.getTitle()).isEqualTo("Admin news title");
        assertThat(item.getContentPreview()).isEqualTo("Short preview");
        assertThat(item.getStatus()).isEqualTo(NewsStatus.PUBLISHED);

        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(15);
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(newsRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void shouldReturnAllNewsForAdminWithSearch() {
        Pageable pageable = PageRequest.of(0, 15);
        String search = "draft";

        News news = new News();

        ResponseNewsAdminListItemDto dto = ResponseNewsAdminListItemDto.builder()
            .id(2L)
            .title("Draft news title")
            .contentPreview("Draft preview")
            .status(NewsStatus.DRAFT)
            .publishedAt(null)
            .archivedAt(null)
            .build();

        Page<News> newsPage = new PageImpl<>(List.of(news), pageable, 1);

        when(newsRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(newsPage);
        when(newsMapper.toAdminListItemDto(news)).thenReturn(dto);

        Page<ResponseNewsAdminListItemDto> result = newsService.getAllNewsForAdmin(pageable, search);

        assertThat(result.getContent()).hasSize(1);

        ResponseNewsAdminListItemDto item = result.getContent().getFirst();
        assertThat(item.getId()).isEqualTo(2L);
        assertThat(item.getTitle()).isEqualTo("Draft news title");
        assertThat(item.getStatus()).isEqualTo(NewsStatus.DRAFT);

        verify(newsRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void shouldReturnEmptyPageForAdminWhenNoNewsFound() {
        Pageable pageable = PageRequest.of(0, 15);

        Page<News> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(newsRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

        Page<ResponseNewsAdminListItemDto> result = newsService.getAllNewsForAdmin(pageable, null);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);

        verify(newsRepository).findAll(any(Specification.class), eq(pageable));
    }

    private void mockAuthenticatedUser(Long userId) {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(userId));
    }
}