package com.itasocialacademy.oitassist.news.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itasocialacademy.oitassist.news.dao.dto.request.CreateNewsDTO;
import com.itasocialacademy.oitassist.news.dao.dto.request.UpdateNewsDto;
import com.itasocialacademy.oitassist.news.dao.dto.response.ResponseNewsDto;
import com.itasocialacademy.oitassist.news.dao.dto.response.ResponseNewsListItemDto;
import com.itasocialacademy.oitassist.news.dao.enums.NewsStatus;
import com.itasocialacademy.oitassist.news.service.interfaces.NewsService;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        mockMvc = MockMvcBuilders.standaloneSetup(newsController)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .build();
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

    @Test
    void testGetPublishedNews_shouldReturnPagedResponse() throws Exception {
        ResponseNewsListItemDto newsItem = new ResponseNewsListItemDto(
            1L,
            "Published news title",
            "Short preview text...",
            OffsetDateTime.parse("2026-03-12T13:44:56Z"),
            null);

        Page<ResponseNewsListItemDto> page = new PageImpl<>(
            List.of(newsItem),
            PageRequest.of(0, 5),
            1);

        when(newsService.getPublishedNews(any(), eq(null), eq(null))).thenReturn(page);

        mockMvc.perform(get("/api/v1/news"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Published news title"))
            .andExpect(jsonPath("$.content[0].contentPreview").value("Short preview text..."))
            .andExpect(jsonPath("$.content[0].archivedAt").isEmpty())
            .andExpect(jsonPath("$.pageNumber").value(0))
            .andExpect(jsonPath("$.pageSize").value(5))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(true));

        verify(newsService).getPublishedNews(any(), eq(null), eq(null));
    }

    @Test
    void testGetPublishedNews_withoutParams_shouldUseDefaultPageable() throws Exception {
        Page<ResponseNewsListItemDto> page = new PageImpl<>(
            List.of(),
            PageRequest.of(0, 5),
            0);

        when(newsService.getPublishedNews(any(), eq(null), eq(null))).thenReturn(page);

        mockMvc.perform(get("/api/v1/news"))
            .andExpect(status().isOk());

        verify(newsService).getPublishedNews(
            argThat(pageable -> pageable.getPageNumber() == 0 &&
                pageable.getPageSize() == 5 &&
                pageable.getSort().getOrderFor("publishedAt") != null &&
                pageable.getSort().getOrderFor("publishedAt").isDescending()),
            eq(null),
            eq(null));
    }

    @Test
    void testGetPublishedNews_withSearchParam_returnsMatchingResults() throws Exception {
        String search = "tech";
        ResponseNewsListItemDto item = new ResponseNewsListItemDto(
            10L,
            "Tech News",
            "Latest on technology...",
            OffsetDateTime.parse("2026-03-20T15:30:00Z"),
            null);

        Page<ResponseNewsListItemDto> page = new PageImpl<>(
            List.of(item),
            PageRequest.of(0, 5),
            1);

        when(newsService.getPublishedNews(any(), eq(search), eq(null))).thenReturn(page);

        mockMvc.perform(get("/api/v1/news")
            .param("search", search))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(10))
            .andExpect(jsonPath("$.content[0].title").value("Tech News"))
            .andExpect(jsonPath("$.content[0].archivedAt").isEmpty())
            .andExpect(jsonPath("$.totalElements").value(1));

        verify(newsService).getPublishedNews(any(), eq(search), eq(null));
    }

    @Test
    void testGetPublishedNews_withSearchParam_noResults() throws Exception {
        String search = "nonexistent";
        Page<ResponseNewsListItemDto> page = new PageImpl<>(
            List.of(),
            PageRequest.of(0, 5),
            0);

        when(newsService.getPublishedNews(any(), eq(search), eq(null))).thenReturn(page);

        mockMvc.perform(get("/api/v1/news")
            .param("search", search))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(0))
            .andExpect(jsonPath("$.totalElements").value(0));

        verify(newsService).getPublishedNews(any(), eq(search), eq(null));
    }
}