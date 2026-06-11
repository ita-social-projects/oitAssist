package com.itasocialacademy.oitassist.news.service.interfaces;

import com.itasocialacademy.oitassist.news.dao.dto.response.ArchivedNewsByYearDto;
import java.util.List;

public interface NewsArchivingService {
    int archiveExpiredPublishedNews();

    List<ArchivedNewsByYearDto> getArchivedNewsGroupedByYearAndMonth();
}
