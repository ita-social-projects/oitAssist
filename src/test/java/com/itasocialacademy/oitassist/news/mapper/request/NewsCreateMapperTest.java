package com.itasocialacademy.oitassist.news.mapper.request;

import com.itasocialacademy.oitassist.news.dao.dto.request.CreateNewsDTO;
import com.itasocialacademy.oitassist.news.dao.enums.NewsStatus;
import com.itasocialacademy.oitassist.news.dao.model.News;
import org.checkerframework.checker.units.qual.N;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NewsCreateMapperTest {
    private final NewsCreateMapper mapper = new NewsCreateMapper();

    @Test
    void shouldCreateDraftNews(){
        CreateNewsDTO dto = new CreateNewsDTO("Title", "Content", false);
        News news = mapper.create(dto, 1L);
        assertEquals(dto.getTitle(), news.getTitle());
        assertEquals(dto.getContent(), news.getContent());
        assertEquals(1L, news.getAuthorId());
        assertEquals(NewsStatus.DRAFT, news.getStatus());
        assertNull(news.getPublishedAt());
        assertNotNull(news.getCreatedAt());
    }

    @Test
    void shouldCreatePublishedNews(){
        CreateNewsDTO dto = new CreateNewsDTO("Title", "Content", true);
        News news = mapper.create(dto, 1L);
        assertEquals(dto.getTitle(), news.getTitle());
        assertEquals(dto.getContent(), news.getContent());
        assertEquals(1L, news.getAuthorId());
        assertEquals(NewsStatus.PUBLISHED, news.getStatus());
        assertNotNull(news.getPublishedAt());
        assertNotNull(news.getCreatedAt());
    }


}