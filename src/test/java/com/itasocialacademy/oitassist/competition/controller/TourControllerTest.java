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
import com.itasocialacademy.oitassist.competition.dto.request.ChangeTourStatusRequest;
import com.itasocialacademy.oitassist.competition.dto.request.CreateTourRequest;
import com.itasocialacademy.oitassist.competition.dto.request.ReorderToursRequest;
import com.itasocialacademy.oitassist.competition.dto.request.UpdateTourRequest;
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
    void getAllTours_shouldReturn200Ok() throws Exception {
        Long stageId = 1L;
        List<TourResponse> mockResponse = List.of(TourResponse.builder().build());

        when(tourService.getAllByStageId(stageId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/stages/{stageId}/tours", stageId)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        verify(tourService).getAllByStageId(stageId);
    }

    @Test
    void updateTour_validRequest_shouldReturn200Ok() throws Exception {
        Long stageId = 1L;
        Long tourId = 10L;
        String requestJson = """
            {
                "title": "Updated Tour",
                "description": "Updated description",
                "dateStart": "2026-07-01T10:00:00Z",
                "dateFinish": "2026-07-01T14:00:00Z",
                "location": "Konotop",
                "version": 1
            }
            """;

        TourResponse mockResponse = TourResponse.builder().build();

        when(tourService.update(eq(stageId), eq(tourId), any(UpdateTourRequest.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(put("/api/v1/stages/{stageId}/tours/{tourId}", stageId, tourId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
            .andExpect(status().isOk());

        verify(tourService).update(eq(stageId), eq(tourId), any(UpdateTourRequest.class));
    }

    @Test
    void changeStatus_validRequest_shouldReturn200Ok() throws Exception {
        Long stageId = 1L;
        Long tourId = 10L;
        String requestJson = """
            {
                "status": "IN_PROGRESS",
                "version": 1
            }
            """;

        TourResponse mockResponse = TourResponse.builder().build();

        when(tourService.changeStatus(eq(stageId), eq(tourId), any(ChangeTourStatusRequest.class)))
            .thenReturn(mockResponse);

        mockMvc.perform(patch("/api/v1/stages/{stageId}/tours/{tourId}/status", stageId, tourId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
            .andExpect(status().isOk());

        verify(tourService).changeStatus(eq(stageId), eq(tourId), any(ChangeTourStatusRequest.class));
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

    @Test
    void deleteTour_shouldReturn204NoContent() throws Exception {
        Long stageId = 1L;
        Long tourId = 10L;

        mockMvc.perform(delete("/api/v1/stages/{stageId}/tours/{tourId}", stageId, tourId))
            .andExpect(status().isNoContent());

        verify(tourService).delete(stageId, tourId);
    }

    @Test
    void getTourById_shouldReturn200Ok() throws Exception {
        Long tourId = 10L;
        TourResponse mockResponse = TourResponse.builder().build();

        when(tourService.getById(tourId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/tours/{tourId}", tourId)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        verify(tourService).getById(tourId);
    }
}
