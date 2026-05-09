package com.itasocialacademy.oitassist.news.api.scheduler;

import com.itasocialacademy.oitassist.news.service.interfaces.NewsArchivingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsArchivingSchedulerTest {
    @Mock
    private NewsArchivingService newsArchivingService;

    private NewsArchivingScheduler newsArchivingScheduler;

    @BeforeEach
    void setUp() {
        newsArchivingScheduler = new NewsArchivingScheduler(newsArchivingService);
    }

    @Test
    void archivePublishedNewsNightly_ShouldCallArchivingService() {
        when(newsArchivingService.archiveExpiredPublishedNews()).thenReturn(2);

        newsArchivingScheduler.archivePublishedNewsNightly();

        verify(newsArchivingService).archiveExpiredPublishedNews();
    }

    @Test
    void archivePublishedNewsNightly_ShouldNotThrowException_WhenServiceFails() {
        when(newsArchivingService.archiveExpiredPublishedNews())
            .thenThrow(new RuntimeException("DB test error"));

        assertDoesNotThrow(() -> newsArchivingScheduler.archivePublishedNewsNightly());

        verify(newsArchivingService).archiveExpiredPublishedNews();
    }
}