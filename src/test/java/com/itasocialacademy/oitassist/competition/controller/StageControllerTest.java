package com.itasocialacademy.oitassist.competition.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.competition.dao.dto.request.CreateStageRequest;
import com.itasocialacademy.oitassist.competition.dao.dto.response.StageResponse;
import com.itasocialacademy.oitassist.competition.service.interfaces.StageService;
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
}