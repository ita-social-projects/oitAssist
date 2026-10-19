package com.itasocialacademy.oitassist.news.service;

import com.itasocialacademy.oitassist.news.dao.dto.response.ArchivedNewsByMonthDto;
import com.itasocialacademy.oitassist.news.dao.dto.response.ArchivedNewsByYearDto;
import com.itasocialacademy.oitassist.news.dao.dto.response.ResponseNewsListItemDto;
import com.itasocialacademy.oitassist.news.dao.enums.NewsStatus;
import com.itasocialacademy.oitassist.news.dao.model.News;
import com.itasocialacademy.oitassist.news.dao.repository.NewsRepository;
import com.itasocialacademy.oitassist.news.service.interfaces.NewsArchivingService;
import java.time.*;
import java.util.*;
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

    private static final ZoneId KYIV_ZONE = ZoneId.of("Europe/Kyiv");

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

    @Transactional(readOnly = true)
    @Override
    public List<ArchivedNewsByYearDto> getArchivedNewsGroupedByYearAndMonth() {
        List<News> archivedNews = newsRepository.findArchivedNewsOrderByPublishedAtDesc();

        Map<Integer, Map<Integer, List<ResponseNewsListItemDto>>> grouped = new TreeMap<>(
            Comparator.reverseOrder());

        for (News news : archivedNews) {
            OffsetDateTime targetDate = news.getPublishedAt() != null ? news.getPublishedAt() : news.getCreatedAt();
            if (targetDate == null) {
                targetDate = OffsetDateTime.now(clock);
            }
            YearMonth yearMonth = YearMonth.from(targetDate.atZoneSameInstant(KYIV_ZONE));

            grouped
                .computeIfAbsent(yearMonth.getYear(), year -> new TreeMap<>(Comparator.reverseOrder()))
                .computeIfAbsent(yearMonth.getMonthValue(), month -> new ArrayList<>())
                .add(toNewsListItemDto(news, targetDate));
        }
        List<ArchivedNewsByYearDto> result = grouped.entrySet().stream()
            .map(yearEntry -> new ArchivedNewsByYearDto(
                yearEntry.getKey(),
                yearEntry.getValue().entrySet().stream()
                    .map(monthEntry -> new ArchivedNewsByMonthDto(
                        monthEntry.getKey(),
                        monthEntry.getValue().stream()
                            .sorted(Comparator.comparing(
                                ResponseNewsListItemDto::getPublishedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                            .toList()))
                    .toList()))
            .toList();

        log.info(
            "Fetched archived news grouped by year and month. newsCount={}, yearGroupsCount={}",
            archivedNews.size(),
            result.size());

        return result;
    }

    private ResponseNewsListItemDto toNewsListItemDto(News news, OffsetDateTime targetDate) {
        return ResponseNewsListItemDto.builder()
            .id(news.getId())
            .title(news.getTitle())
            .publishedAt(targetDate)
            .contentPreview(news.getContent())
            .archivedAt(news.getArchivedAt())
            .build();
    }
}
