package com.itasocialacademy.oitassist.news.service.interfaces;

import com.itasocialacademy.oitassist.news.dao.dto.request.CreateNewsDTO;

public interface NewsService {
    void createNews(CreateNewsDTO newsDTO, Long authorId);
}
