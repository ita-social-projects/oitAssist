package com.itasocialacademy.oitassist.competition.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.competition.dto.request.ChangeStatusRequest;
import com.itasocialacademy.oitassist.competition.dto.request.CreateCompetitionRequest;
import com.itasocialacademy.oitassist.competition.dto.response.CompetitionResponse;
import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.service.interfaces.CompetitionService;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;

public class CompetitionControllerTest extends ControllerUnitTest<CompetitionController> {

    @Mock
    private CompetitionService competitionService;

    @InjectMocks
    private CompetitionController competitionController;

    @Override
    protected CompetitionController getController() {
        return competitionController;
    }

    private CompetitionResponse mockCompetitionResponse;
    private ZonedDateTime testDateStart;
    private ZonedDateTime testDateFinish;

    @BeforeEach
    public void setUpMockData() {
        testDateStart = ZonedDateTime.of(2026, 6, 25, 10, 0, 0, 0, ZoneId.of("UTC"));
        testDateFinish = testDateStart.plusDays(10);

        mockCompetitionResponse = new CompetitionResponse(
            1L,
            "Всеукраїнська Олімпіада 2026",
            "Опис тестової олімпіади",
            testDateStart,
            testDateFinish,
            CompetitionStatus.DRAFT,
            100L,
            100L);
    }

    @Test
    void createCompetition_validRequest_shouldReturn201() throws Exception {
        CreateCompetitionRequest request = new CreateCompetitionRequest(
            "Всеукраїнська Олімпіада 2026",
            "Опис тестової олімпіади",
            testDateStart,
            testDateFinish);

        when(competitionService.create(any(CreateCompetitionRequest.class))).thenReturn(mockCompetitionResponse);

        mockMvc.perform(post("/api/v1/competitions")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.title").value("Всеукраїнська Олімпіада 2026"))
            .andExpect(jsonPath("$.status").value("DRAFT"));

        verify(competitionService).create(any(CreateCompetitionRequest.class));
    }

    @Test
    void getAllVisible_shouldReturnPageResponseAnd200() throws Exception {
        Page<CompetitionResponse> page = new PageImpl<>(List.of(mockCompetitionResponse));
        when(competitionService.getAllVisible(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/competitions")
            .param("page", "0")
            .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].id").value(1L))
            .andExpect(jsonPath("$.content[0].title").value("Всеукраїнська Олімпіада 2026"))
            .andExpect(jsonPath("$.pageNumber").value(0))
            .andExpect(jsonPath("$.totalElements").value(1));

        verify(competitionService).getAllVisible(any());
    }

    @Test
    void changeStatus_validRequest_shouldReturn200() throws Exception {
        ChangeStatusRequest request = new ChangeStatusRequest(CompetitionStatus.PUBLISHED);

        CompetitionResponse publishedResponse = new CompetitionResponse(
            mockCompetitionResponse.id(),
            mockCompetitionResponse.title(),
            mockCompetitionResponse.description(),
            mockCompetitionResponse.dateStart(),
            mockCompetitionResponse.dateFinish(),
            CompetitionStatus.PUBLISHED,
            mockCompetitionResponse.createdBy(),
            mockCompetitionResponse.updatedBy());

        when(competitionService.changeStatus(eq(1L), eq(CompetitionStatus.PUBLISHED))).thenReturn(publishedResponse);

        mockMvc.perform(patch("/api/v1/competitions/{id}/status", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(competitionService).changeStatus(1L, CompetitionStatus.PUBLISHED);
    }

    @Test
    void changeStatus_invalidRequest_nullStatus_shouldReturn400() throws Exception {
        ChangeStatusRequest request = new ChangeStatusRequest(null);

        mockMvc.perform(patch("/api/v1/competitions/{id}/status", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}
