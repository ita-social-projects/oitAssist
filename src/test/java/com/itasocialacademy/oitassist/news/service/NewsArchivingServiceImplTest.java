package com.itasocialacademy.oitassist.news.service;

import com.itasocialacademy.oitassist.news.dao.enums.NewsStatus;
import com.itasocialacademy.oitassist.news.dao.repository.NewsRepository;
import java.time.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}