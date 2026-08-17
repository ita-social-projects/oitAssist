package com.itasocialacademy.oitassist.competition.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import com.itasocialacademy.oitassist.competition.dto.filter.CompetitionSearchFilter;
import com.itasocialacademy.oitassist.competition.dto.request.ChangeCompetitionStatusRequest;
import com.itasocialacademy.oitassist.competition.dto.request.CreateCompetitionRequest;
import com.itasocialacademy.oitassist.competition.dto.response.CompetitionResponse;
import com.itasocialacademy.oitassist.competition.dto.response.CompetitionTreeResponse;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionNotFoundException;
import com.itasocialacademy.oitassist.competition.exceptions.StaleEntityVersionException;
import com.itasocialacademy.oitassist.competition.service.interfaces.CompetitionService;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;

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

        mockCompetitionResponse = CompetitionResponse.builder()
            .id(1L)
            .title("Всеукраїнська Олімпіада 2026")
            .description("Опис тестової олімпіади")
            .dateStart(testDateStart)
            .dateFinish(testDateFinish).competitionStatus(CompetitionStatus.DRAFT).createdBy(100L).updatedBy(100L)
            .build();
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
            .andExpect(jsonPath("$.competitionStatus").value("DRAFT"));

        verify(competitionService).create(any(CreateCompetitionRequest.class));
    }

    // ---- getAllVisible ----

    @Test
    void getAllVisible_shouldReturnPageResponseAnd200() throws Exception {
        Page<CompetitionResponse> page = new PageImpl<>(List.of(mockCompetitionResponse));
        when(competitionService.getAllVisible(any(CompetitionSearchFilter.class), any(Pageable.class)))
            .thenReturn(page);

        mockMvc.perform(get("/api/v1/competitions")
            .param("page", "0")
            .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].id").value(1L))
            .andExpect(jsonPath("$.content[0].title").value("Всеукраїнська Олімпіада 2026"))
            .andExpect(jsonPath("$.pageNumber").value(0))
            .andExpect(jsonPath("$.totalElements").value(1));

        verify(competitionService).getAllVisible(any(CompetitionSearchFilter.class), any(Pageable.class));
    }

    @Test
    void getAllVisible_withTitleAndStatusesParams_shouldBindFilterCorrectly() throws Exception {
        Page<CompetitionResponse> page = new PageImpl<>(List.of(mockCompetitionResponse));
        ArgumentCaptor<CompetitionSearchFilter> filterCaptor = ArgumentCaptor.forClass(CompetitionSearchFilter.class);

        when(competitionService.getAllVisible(any(CompetitionSearchFilter.class), any(Pageable.class)))
            .thenReturn(page);

        mockMvc.perform(get("/api/v1/competitions")
            .param("title", "Олімпіада")
            .param("statuses", "PUBLISHED", "ENROLLMENT")
            .param("page", "0")
            .param("size", "20"))
            .andExpect(status().isOk());

        verify(competitionService).getAllVisible(filterCaptor.capture(), any(Pageable.class));
        assertEquals("Олімпіада", filterCaptor.getValue().title());
        assertEquals(List.of(CompetitionStatus.PUBLISHED, CompetitionStatus.ENROLLMENT),
            filterCaptor.getValue().statuses());
    }

    @Test
    void getAllVisible_dateFinishBeforeDateStart_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/v1/competitions")
            .param("dateStart", "2026-07-10T00:00:00Z")
            .param("dateFinish", "2026-07-01T00:00:00Z"))
            .andExpect(status().isBadRequest());

        verify(competitionService, never()).getAllVisible(any(), any());
    }

    // ---- getCompetitionById ----

    @Test
    void getCompetitionById_existingId_shouldReturn200() throws Exception {
        when(competitionService.getVisibleById(1L)).thenReturn(mockCompetitionResponse);

        mockMvc.perform(get("/api/v1/competitions/{competitionId}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.title").value("Всеукраїнська Олімпіада 2026"))
            .andExpect(jsonPath("$.competitionStatus").value("DRAFT"));

        verify(competitionService).getVisibleById(1L);
    }

    @Test
    void getCompetitionById_notFound_shouldReturn404() throws Exception {
        when(competitionService.getVisibleById(99L)).thenThrow(new CompetitionNotFoundException(99L));

        mockMvc.perform(get("/api/v1/competitions/{competitionId}", 99L))
            .andExpect(status().isNotFound());
    }

    @Test
    void getCompetitionById_draftOfAnotherOrg_shouldReturn403() throws Exception {
        when(competitionService.getVisibleById(1L))
            .thenThrow(new AccessDeniedException("You do not have permission to view this draft competition"));

        mockMvc.perform(get("/api/v1/competitions/{competitionId}", 1L))
            .andExpect(status().isForbidden());
    }

    // ---- getCompetitionTree ----

    @Test
    void getCompetitionTree_existingId_shouldReturn200() throws Exception {
        CompetitionTreeResponse treeResponse = new CompetitionTreeResponse(mockCompetitionResponse, List.of());
        when(competitionService.getCompetitionTree(1L)).thenReturn(treeResponse);

        mockMvc.perform(get("/api/v1/competitions/{competitionId}/tree", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.competition.id").value(1L))
            .andExpect(jsonPath("$.competition.title").value("Всеукраїнська Олімпіада 2026"))
            .andExpect(jsonPath("$.stages").isArray())
            .andExpect(jsonPath("$.stages").isEmpty());

        verify(competitionService).getCompetitionTree(1L);
    }

    @Test
    void getCompetitionTree_notFound_shouldReturn404() throws Exception {
        when(competitionService.getCompetitionTree(99L)).thenThrow(new CompetitionNotFoundException(99L));

        mockMvc.perform(get("/api/v1/competitions/{competitionId}/tree", 99L))
            .andExpect(status().isNotFound());
    }

    @Test
    void getCompetitionTree_draftOfAnotherOrg_shouldReturn403() throws Exception {
        when(competitionService.getCompetitionTree(1L))
            .thenThrow(new AccessDeniedException("You do not have permission to view this draft competition"));

        mockMvc.perform(get("/api/v1/competitions/{competitionId}/tree", 1L))
            .andExpect(status().isForbidden());
    }

    // ---- changeStatus ----

    @Test
    void changeStatus_validRequest_shouldReturn200() throws Exception {
        ChangeCompetitionStatusRequest request = new ChangeCompetitionStatusRequest(CompetitionStatus.PUBLISHED, 1L);

        CompetitionResponse publishedResponse = new CompetitionResponse(
            mockCompetitionResponse.id(),
            mockCompetitionResponse.title(),
            mockCompetitionResponse.description(),
            mockCompetitionResponse.dateStart(),
            mockCompetitionResponse.dateFinish(),
            CompetitionStatus.PUBLISHED,
            mockCompetitionResponse.createdBy(),
            mockCompetitionResponse.updatedBy(),
            1L);

        when(competitionService.changeStatus(eq(1L), eq(request))).thenReturn(publishedResponse);

        mockMvc.perform(patch("/api/v1/competitions/{id}/status", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.competitionStatus").value("PUBLISHED"));

        verify(competitionService).changeStatus(1L, request);
    }

    @Test
    void changeStatus_invalidRequest_nullStatus_shouldReturn400() throws Exception {
        ChangeCompetitionStatusRequest request = new ChangeCompetitionStatusRequest(null, 1L);

        mockMvc.perform(patch("/api/v1/competitions/{id}/status", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void changeStatus_staleVersion_shouldReturn409() throws Exception {
        ChangeCompetitionStatusRequest request = new ChangeCompetitionStatusRequest(CompetitionStatus.PUBLISHED, 1L);

        when(competitionService.changeStatus(eq(1L), eq(request)))
            .thenThrow(new StaleEntityVersionException(Competition.class, 1L));

        mockMvc.perform(patch("/api/v1/competitions/{id}/status", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    void changeStatus_invalidRequest_nullVersion_shouldReturn400() throws Exception {
        ChangeCompetitionStatusRequest request = new ChangeCompetitionStatusRequest(CompetitionStatus.PUBLISHED, null);

        mockMvc.perform(patch("/api/v1/competitions/{id}/status", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    // ---- getArchived ----

    @Test
    void getAllArchived_shouldReturnPageResponseAnd200() throws Exception {
        Page<CompetitionResponse> page = new PageImpl<>(List.of(getArchivedCompetitionResponse()));

        when(competitionService.getArchived(any(CompetitionSearchFilter.class), any(Pageable.class)))
            .thenReturn(page);

        mockMvc.perform(get("/api/v1/competitions/archived")
            .param("page", "0")
            .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].id").value(1L))
            .andExpect(jsonPath("$.content[0].title").value("Всеукраїнська Олімпіада 2026"))
            .andExpect(jsonPath("$.content[0].competitionStatus").value("ARCHIVED"))
            .andExpect(jsonPath("$.pageNumber").value(0))
            .andExpect(jsonPath("$.totalElements").value(1));

        verify(competitionService).getArchived(any(CompetitionSearchFilter.class), any(Pageable.class));
    }

    private CompetitionResponse getArchivedCompetitionResponse() {

        return new CompetitionResponse(
            1L,
            "Всеукраїнська Олімпіада 2026",
            "Опис тестової олімпіади",
            ZonedDateTime.of(2026, 6, 25, 10, 0, 0, 0, ZoneId.of("UTC")),
            ZonedDateTime.of(2026, 6, 25, 10, 0, 0, 0, ZoneId.of("UTC")).plusDays(10),
            CompetitionStatus.ARCHIVED,
            100L,
            100L,
            1L);
    }
}
