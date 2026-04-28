package com.itasocialacademy.oitassist.news.service;

import com.itasocialacademy.oitassist.news.dao.enums.NewsStatus;
import com.itasocialacademy.oitassist.news.dao.repository.NewsRepository;
import com.itasocialacademy.oitassist.news.service.interfaces.NewsArchivingService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class NewsArchivingServiceImpl implements NewsArchivingService {
    private final NewsRepository newsRepository;
    private final Clock clock;

    @Transactional
    @Override
    public int archiveExpiredPublishedNews() {
        LocalDate todayInKyiv = LocalDate.now(clock);
        LocalDate thresholdDate = todayInKyiv.minusDays(30);
        OffsetDateTime archivedAt = OffsetDateTime.now(clock);

        int archivedCount = newsRepository.archivedPublishedNewsOlderThanOneMonth(
            NewsStatus.PUBLISHED.name(),
            NewsStatus.ARCHIVED.name(),
            thresholdDate,
            archivedAt);

        log.info(
            "Archived {} published news item. todayInKyiv={}, thresholdDate={}",
            archivedCount,
            todayInKyiv,
            thresholdDate);
        return archivedCount;
    }
}
