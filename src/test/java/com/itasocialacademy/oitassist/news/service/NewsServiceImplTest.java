package com.itasocialacademy.oitassist.news.service;

import com.itasocialacademy.oitassist.news.dao.dto.request.CreateNewsDTO;
import com.itasocialacademy.oitassist.news.dao.model.News;
import com.itasocialacademy.oitassist.news.dao.repository.NewsRepository;
import com.itasocialacademy.oitassist.news.mapper.request.NewsCreateMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsServiceImplTest {
    @Mock
    private NewsCreateMapper newsCreateMapper;
    @Mock
    private NewsRepository newsRepository;
    @InjectMocks
    private NewsServiceImpl newsService;


    @Test
    void shouldCreateNewsAndSave(){
        CreateNewsDTO dto = new CreateNewsDTO("Title", "Content", false);
        News news = new News();
        when(newsCreateMapper.create(dto, 1L)).thenReturn(news);
        newsService.createNews(dto, 1L);

        verify(newsCreateMapper).create(dto, 1L);
        verify(newsRepository).save(news);
    }
}