package com.itasocialacademy.oitassist.competition.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.competition.dto.request.ChangeStageStatusRequest;
import com.itasocialacademy.oitassist.competition.dto.request.CreateStageRequest;
import com.itasocialacademy.oitassist.competition.dto.request.UpdateStageRequest;
import com.itasocialacademy.oitassist.competition.dto.response.StageResponse;
import com.itasocialacademy.oitassist.competition.service.interfaces.StageService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;

public class StageControllerTest extends ControllerUnitTest<StageController> {

    @Mock
    private StageService stageService;

    @InjectMocks
    private StageController stageController;

    @Override
    protected StageController getController() {
        return stageController;
    }

    @Test
    void createStage_validRequest_shouldReturn201Created() throws Exception {
        // Arrange
        Long competitionId = 1L;
        String requestJson = """
            {
                "title": "Stage 1",
                "description": "First stage",
                "dateStart": "2026-07-01T10:00:00Z",
                "dateFinish": "2026-07-05T18:00:00Z",
                "scope": "NATIONAL"
            }
            """;

        StageResponse mockResponse = StageResponse.builder().build();

        when(stageService.create(eq(competitionId), any(CreateStageRequest.class)))
            .thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/competitions/{competitionId}/stages", competitionId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
            .andExpect(status().isCreated());

        verify(stageService).create(eq(competitionId), any(CreateStageRequest.class));
    }

    @Test
    void getAllStages_shouldReturn200Ok() throws Exception {
        Long competitionId = 1L;
        List<StageResponse> mockResponse = List.of(StageResponse.builder().build());

        when(stageService.getAllByCompetitionId(competitionId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/competitions/{competitionId}/stages", competitionId)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        verify(stageService).getAllByCompetitionId(competitionId);
    }

    @Test
    void updateStage_validRequest_shouldReturn200Ok() throws Exception {
        Long competitionId = 1L;
        Long stageId = 5L;
        String requestJson = """
            {
                "title": "Updated Stage",
                "description": "Updated description",
                "dateStart": "2026-07-01T10:00:00Z",
                "dateFinish": "2026-07-05T18:00:00Z",
                "scope": "NATIONAL",
                "version": 1
            }
            """;

        StageResponse mockResponse = StageResponse.builder().build();

        when(stageService.update(eq(competitionId), eq(stageId), any(UpdateStageRequest.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(put("/api/v1/competitions/{competitionId}/stages/{stageId}", competitionId, stageId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
            .andExpect(status().isOk());

        verify(stageService).update(eq(competitionId), eq(stageId), any(UpdateStageRequest.class));
    }

    @Test
    void changeStatus_validRequest_shouldReturn200Ok() throws Exception {
        Long competitionId = 1L;
        Long stageId = 5L;
        String requestJson = """
            {
                "status": "IN_PROGRESS",
                "version": 1
            }
            """;

        StageResponse mockResponse = StageResponse.builder().build();

        when(stageService.changeStatus(eq(competitionId), eq(stageId), any(ChangeStageStatusRequest.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(patch("/api/v1/competitions/{competitionId}/stages/{stageId}/status", competitionId, stageId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
            .andExpect(status().isOk());

        verify(stageService).changeStatus(eq(competitionId), eq(stageId), any(ChangeStageStatusRequest.class));
    }

    @Test
    void deleteStage_shouldReturn204NoContent() throws Exception {
        Long competitionId = 1L;
        Long stageId = 5L;

        mockMvc.perform(delete("/api/v1/competitions/{competitionId}/stages/{stageId}", competitionId, stageId))
            .andExpect(status().isNoContent());

        verify(stageService).delete(competitionId, stageId);
    }

    @Test
    void getStageById_shouldReturn200Ok() throws Exception {
        Long stageId = 5L;
        StageResponse mockResponse = StageResponse.builder().build();

        when(stageService.getById(stageId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/stages/{stageId}", stageId)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        verify(stageService).getById(stageId);
    }
}