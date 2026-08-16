package com.itasocialacademy.oitassist.news.dao.dto.response;

import java.util.List;

public record ArchivedNewsByYearDto(
    int year,
    List<ArchivedNewsByMonthDto> months) {
}
