package com.itasocialacademy.oitassist.news.service;

import com.itasocialacademy.oitassist.news.dao.dto.response.ArchivedNewsByYearDto;
import com.itasocialacademy.oitassist.news.dao.enums.NewsStatus;
import com.itasocialacademy.oitassist.news.dao.model.News;
import com.itasocialacademy.oitassist.news.dao.repository.NewsRepository;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class NewsArchivingServiceImplTest {

    @Mock
    private NewsRepository newsRepository;
    private NewsArchivingServiceImpl newsArchivingService;
    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(
            Instant.parse("2026-03-31T00:05:00Z"),
            ZoneId.of("Europe/Kyiv"));
        newsArchivingService = new NewsArchivingServiceImpl(newsRepository, clock);
    }

    @Test
    void archiveExpiredPublishedNews_ShouldArchiveNewsWithCorrectThresholdDate() {
        when(newsRepository.archivedPublishedNewsOlderThanOneMonth(
            eq(NewsStatus.PUBLISHED.name()),
            eq(NewsStatus.ARCHIVED.name()),
            eq(LocalDate.of(2026, 3, 1)),
            any(OffsetDateTime.class))).thenReturn(3);

        int result = newsArchivingService.archiveExpiredPublishedNews();

        assertEquals(3, result);

        verify(newsRepository).archivedPublishedNewsOlderThanOneMonth(
            eq(NewsStatus.PUBLISHED.name()),
            eq(NewsStatus.ARCHIVED.name()),
            eq(LocalDate.of(2026, 3, 1)),
            any(OffsetDateTime.class));
    }

    @Test
    void archiveExpiredPublishedNews_ShouldReturnZero_WhenNoNewsArchived() {
        when(newsRepository.archivedPublishedNewsOlderThanOneMonth(
            eq(NewsStatus.PUBLISHED.name()),
            eq(NewsStatus.ARCHIVED.name()),
            eq(LocalDate.of(2026, 3, 1)),
            any(OffsetDateTime.class))).thenReturn(0);

        int result = newsArchivingService.archiveExpiredPublishedNews();

        assertEquals(0, result);

        verify(newsRepository).archivedPublishedNewsOlderThanOneMonth(
            eq(NewsStatus.PUBLISHED.name()),
            eq(NewsStatus.ARCHIVED.name()),
            eq(LocalDate.of(2026, 3, 1)),
            any(OffsetDateTime.class));
    }

    @Test
    void getArchivedNewsGroupedByYearAndMonth_ShouldReturnEmptyList_WhenNoArchivedNews() {
        when(newsRepository.findArchivedNewsOrderByArchivedAtDesc()).thenReturn(List.of());

        List<ArchivedNewsByYearDto> result = newsArchivingService.getArchivedNewsGroupedByYearAndMonth();

        assertTrue(result.isEmpty());

        verify(newsRepository).findArchivedNewsOrderByArchivedAtDesc();
    }

    @Test
    void getArchivedNewsGroupedByYearAndMonth_ShouldGroupNewsByYearAndMonthInDescendingOrder() {
        News news2026May = buildArchivedNews(
            1L,
            "News 2026 May",
            OffsetDateTime.parse("2026-05-10T10:00:00+03:00"));

        News news2026April = buildArchivedNews(
            2L,
            "News 2026 April",
            OffsetDateTime.parse("2026-04-10T10:00:00+03:00"));

        News news2025December = buildArchivedNews(
            3L,
            "News 2025 December",
            OffsetDateTime.parse("2025-12-10T10:00:00+02:00"));

        when(newsRepository.findArchivedNewsOrderByArchivedAtDesc())
            .thenReturn(List.of(news2025December, news2026April, news2026May));

        List<ArchivedNewsByYearDto> result = newsArchivingService.getArchivedNewsGroupedByYearAndMonth();

        assertEquals(2, result.size());

        assertEquals(2026, result.get(0).year());
        assertEquals(2025, result.get(1).year());

        assertEquals(2, result.get(0).months().size());
        assertEquals(5, result.get(0).months().get(0).month());
        assertEquals(4, result.get(0).months().get(1).month());

        assertEquals(1, result.get(0).months().get(0).news().size());
        assertEquals("News 2026 May", result.get(0).months().get(0).news().get(0).getTitle());

        assertEquals(12, result.get(1).months().get(0).month());
        assertEquals("News 2025 December", result.get(1).months().get(0).news().get(0).getTitle());
    }

    @Test
    void getArchivedNewsGroupedByYearAndMonth_ShouldGroupByKyivTimezone() {
        News news = buildArchivedNews(
            1L,
            "Kyiv timezone news",
            OffsetDateTime.parse("2026-04-30T22:30:00Z"));

        when(newsRepository.findArchivedNewsOrderByArchivedAtDesc()).thenReturn(List.of(news));

        List<ArchivedNewsByYearDto> result = newsArchivingService.getArchivedNewsGroupedByYearAndMonth();

        assertEquals(1, result.size());
        assertEquals(2026, result.get(0).year());
        assertEquals(5, result.get(0).months().get(0).month());
    }

    private News buildArchivedNews(Long id, String title, OffsetDateTime archivedAt) {
        News news = new News();
        news.setId(id);
        news.setTitle(title);
        news.setContent("Some content for preview");
        news.setStatus(NewsStatus.ARCHIVED);
        news.setPublishedAt(archivedAt.minusDays(30));
        news.setArchivedAt(archivedAt);
        return news;
    }
}