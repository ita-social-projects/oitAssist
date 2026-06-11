package com.itasocialacademy.oitassist.news.dao.dto.response;

import java.util.List;

public record ArchivedNewsByMonthDto(
    int month,
    List<ResponseNewsListItemDto> news) {
}
