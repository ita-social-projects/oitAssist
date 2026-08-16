package com.itasocialacademy.oitassist.evaluation.controller;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.evaluation.api.dto.ParticipantResult;
import com.itasocialacademy.oitassist.evaluation.service.interfaces.ResultsService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResultsControllerTest extends ControllerUnitTest<ResultsController> {
    private static final String RESULTS_URL = "/api/v1/results";

    @Mock
    private ResultsService resultsService;

    @InjectMocks
    private ResultsController resultsController;

    @Override
    protected ResultsController getController() {
        return resultsController;
    }

    @Test
    void getResults_ShouldReturnOk_WhenCompetitionIdProvided() throws Exception {
        ParticipantResult participant = new ParticipantResult("Ігор", 15, List.of());
        when(resultsService.getResultsPage(any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(participant), PageRequest.of(0, 20), 1));

        mockMvc.perform(get(RESULTS_URL).param("competitionId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].participantName").value("Ігор"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getResults_ShouldPassScopeAndSearchToService_WhenProvided() throws Exception {
        when(resultsService.getResultsPage(any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get(RESULTS_URL)
            .param("competitionId", "1")
            .param("stageIds", "1")
            .param("tourIds", "2", "3")
            .param("search", "Ігор"))
            .andExpect(status().isOk());

        verify(resultsService).getResultsPage(
            eq(1L), eq(Set.of(1L)), eq(Set.of(2L, 3L)), eq("Ігор"), any());
    }

    @Test
    void getResults_ShouldPassEmptySets_WhenScopeNotProvided() throws Exception {
        when(resultsService.getResultsPage(any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get(RESULTS_URL).param("competitionId", "1"))
            .andExpect(status().isOk());

        verify(resultsService).getResultsPage(eq(1L), eq(Set.of()), eq(Set.of()), eq(null), any());
    }

    @Test
    void getResults_ShouldPassPageable_WhenPageAndSizeProvided() throws Exception {
        when(resultsService.getResultsPage(any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 0));

        mockMvc.perform(get(RESULTS_URL)
            .param("competitionId", "1")
            .param("page", "1")
            .param("size", "5"))
            .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(resultsService).getResultsPage(any(), any(), any(), any(), captor.capture());
        assertEquals(1, captor.getValue().getPageNumber());
        assertEquals(5, captor.getValue().getPageSize());
    }
}