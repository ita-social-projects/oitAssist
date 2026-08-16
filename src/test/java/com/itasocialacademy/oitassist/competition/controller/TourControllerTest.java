package com.itasocialacademy.oitassist.competition.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.competition.dto.request.CreateTourRequest;
import com.itasocialacademy.oitassist.competition.dto.request.ReorderToursRequest;
import com.itasocialacademy.oitassist.competition.dto.response.TourResponse;
import com.itasocialacademy.oitassist.competition.service.interfaces.TourService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;

public class TourControllerTest extends ControllerUnitTest<TourController> {

    @Mock
    private TourService tourService;

    @InjectMocks
    private TourController tourController;

    @Override
    protected TourController getController() {
        return tourController;
    }

    @Test
    void createTour_validRequest_shouldReturn201Created() throws Exception {
        // Arrange
        Long stageId = 1L;
        String requestJson = """
            {
                "title": "Tour 1",
                "description": "First tour",
                "dateStart": "2026-07-01T10:00:00Z",
                "dateFinish": "2026-07-01T14:00:00Z",
                "location": "Online"
            }
            """;

        TourResponse mockResponse = TourResponse.builder().build();

        when(tourService.create(eq(stageId), any(CreateTourRequest.class)))
            .thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/stages/{stageId}/tours", stageId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
            .andExpect(status().isCreated());

        verify(tourService).create(eq(stageId), any(CreateTourRequest.class));
    }

    @Test
    void reorderTours_validRequest_shouldReturn200Ok() throws Exception {
        Long stageId = 1L;
        String requestJson = """
            {
                "tourIds": [3, 1, 2]
            }
            """;

        List<TourResponse> mockResponse = List.of(TourResponse.builder().build());

        when(tourService.reorder(eq(stageId), any(ReorderToursRequest.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(patch("/api/v1/stages/{stageId}/tours/order", stageId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
            .andExpect(status().isOk());

        verify(tourService).reorder(eq(stageId), any(ReorderToursRequest.class));
    }

    @Test
    void reorderTours_emptyTourIds_shouldReturn400BadRequest() throws Exception {
        Long stageId = 1L;
        String requestJson = """
            {
                "tourIds": []
            }
            """;

        mockMvc.perform(patch("/api/v1/stages/{stageId}/tours/order", stageId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void reorderTours_missingTourIds_shouldReturn400BadRequest() throws Exception {
        Long stageId = 1L;
        String requestJson = "{}";

        mockMvc.perform(patch("/api/v1/stages/{stageId}/tours/order", stageId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
            .andExpect(status().isBadRequest());
    }
}
