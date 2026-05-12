package com.itasocialacademy.oitassist.news.api.scheduler;

import com.itasocialacademy.oitassist.news.service.interfaces.NewsArchivingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsArchivingScheduler {
    private final NewsArchivingService newsArchivingService;

    @Scheduled(cron = "${app.news.archiving.cron}", zone = "Europe/Kiev")
    public void archivePublishedNewsNightly() {
        try {
            log.info("Starting nightly news archiving job");
            int archivedCount = newsArchivingService.archiveExpiredPublishedNews();
            log.info("Finished nightly news archiving job. Archived count={}", archivedCount);
        } catch (Exception ex) {
            log.error("Error during nightly news archiving job", ex);
        }
    }
}
