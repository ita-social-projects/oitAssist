package com.itasocialacademy.oitassist.news.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.itasocialacademy.oitassist.news.dao.dto.request.CreateNewsDTO;
import com.itasocialacademy.oitassist.news.dao.dto.request.UpdateNewsDto;
import com.itasocialacademy.oitassist.news.dao.dto.response.ResponseNewsDto;
import com.itasocialacademy.oitassist.news.dao.enums.NewsStatus;
import com.itasocialacademy.oitassist.news.service.interfaces.NewsService;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;

class NewsControllerTest {

    @InjectMocks
    private NewsController newsController;

    @Mock
    private NewsService newsService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(newsController).build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateNews() throws Exception {
        CreateNewsDTO dto = new CreateNewsDTO("Title", "Content", false);

        ResponseNewsDto response = new ResponseNewsDto(
            1L,
            "Title",
            "Content",
            NewsStatus.DRAFT,
            OffsetDateTime.now(),
            null);

        when(newsService.save(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/news")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isCreated());

        verify(newsService).save(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateNews() throws Exception {
        UpdateNewsDto updateNewsDto = new UpdateNewsDto();
        updateNewsDto.setId(1L);
        updateNewsDto.setTitle("Updated News Title");
        updateNewsDto.setContent("Updated content.");

        ResponseNewsDto responseNewsDto = new ResponseNewsDto();
        responseNewsDto.setId(1L);
        responseNewsDto.setTitle("Updated News Title");
        responseNewsDto.setContent("Updated content.");

        when(newsService.update(any(UpdateNewsDto.class))).thenReturn(responseNewsDto);

        mockMvc.perform(put("/api/v1/news")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateNewsDto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Updated News Title"))
            .andExpect(jsonPath("$.content").value("Updated content."));

        verify(newsService).update(any(UpdateNewsDto.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetNewsById() throws Exception {
        ResponseNewsDto responseNewsDto = new ResponseNewsDto();
        responseNewsDto.setId(1L);
        responseNewsDto.setTitle("Existing News Title");
        responseNewsDto.setContent("Existing content.");

        when(newsService.getById(1L)).thenReturn(responseNewsDto);

        mockMvc.perform(get("/api/v1/news/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.title").value("Existing News Title"))
            .andExpect(jsonPath("$.content").value("Existing content."));

        verify(newsService).getById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteNews() throws Exception {
        doNothing().when(newsService).delete(1L);

        mockMvc.perform(delete("/api/v1/news/1"))
            .andExpect(status().isOk());

        verify(newsService).delete(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateNewsValidationError() throws Exception {
        CreateNewsDTO createNewsDTO = new CreateNewsDTO("", "", false);

        mockMvc.perform(post("/api/v1/news")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createNewsDTO)))
            .andExpect(status().isBadRequest());
    }
}