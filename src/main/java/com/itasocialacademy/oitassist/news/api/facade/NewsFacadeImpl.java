package com.itasocialacademy.oitassist.news.api.facade;

import com.itasocialacademy.oitassist.news.api.interfaces.NewsFacade;
import com.itasocialacademy.oitassist.news.dao.dto.request.CreateNewsDTO;
import com.itasocialacademy.oitassist.news.service.interfaces.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NewsFacadeImpl implements NewsFacade {
    private final NewsService newsService;

    @Override
    public void createNews(CreateNewsDTO dto, Long authorId) {
        newsService.createNews(dto, authorId);
    }
}
