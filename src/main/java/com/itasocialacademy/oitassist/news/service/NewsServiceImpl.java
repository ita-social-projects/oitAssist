package com.itasocialacademy.oitassist.news.service;

import com.itasocialacademy.oitassist.news.dao.dto.request.CreateNewsDTO;
import com.itasocialacademy.oitassist.news.dao.model.News;
import com.itasocialacademy.oitassist.news.dao.repository.NewsRepository;
import com.itasocialacademy.oitassist.news.mapper.request.NewsCreateMapper;
import com.itasocialacademy.oitassist.news.service.interfaces.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {
    private final NewsRepository newsRepository;
    private final NewsCreateMapper newsCreateMapper;

    @Override
    public void createNews(CreateNewsDTO newsDTO, Long authorId) {
        News news = newsCreateMapper.create(newsDTO, authorId);
        newsRepository.save(news);
    }
}
