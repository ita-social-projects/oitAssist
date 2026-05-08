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
        OffsetDateTime nowKyiv = OffsetDateTime.now(clock);
        LocalDate todayInKyiv = nowKyiv.toLocalDate();
        LocalDate thresholdDate = todayInKyiv.minusDays(30);

        int archivedCount = newsRepository.archivedPublishedNewsOlderThanOneMonth(
            NewsStatus.PUBLISHED.name(),
            NewsStatus.ARCHIVED.name(),
            thresholdDate,
            nowKyiv);

        log.info(
            "Archived {} published news items. todayInKyiv={}, thresholdDate={}",
            archivedCount,
            todayInKyiv,
            thresholdDate);
        return archivedCount;
    }
}
