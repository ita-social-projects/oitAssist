package com.itasocialacademy.oitassist.competition.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.competition.dao.dto.request.CreateTourRequest;
import com.itasocialacademy.oitassist.competition.dao.dto.response.TourResponse;
import com.itasocialacademy.oitassist.competition.service.interfaces.TourService;
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
}
