package com.itasocialacademy.oitassist.news.api.interfaces;

import com.itasocialacademy.oitassist.news.dao.dto.request.CreateNewsDTO;

public interface NewsFacade {
    void createNews(CreateNewsDTO dto, Long authorId);
}
