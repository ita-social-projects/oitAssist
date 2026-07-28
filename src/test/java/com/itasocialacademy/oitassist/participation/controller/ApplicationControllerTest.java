package com.itasocialacademy.oitassist.participation.controller;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.participation.controller.ApplicationController;
import com.itasocialacademy.oitassist.participation.dao.dto.request.CreateApplicationRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.request.RejectEnrollmentRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.response.CreateApplicationResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.ProcessApplicationResponse;
import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import com.itasocialacademy.oitassist.participation.service.interfaces.ApplicationService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApplicationControllerTest extends ControllerUnitTest<ApplicationController> {
    @Mock
    private ApplicationService applicationService;

    @InjectMocks
    private ApplicationController applicationController;

    @Override
    protected ApplicationController getController() {
        return applicationController;
    }

    @Test
    void createApplication_shouldReturnCreated_whenRequestIsValid() throws Exception {

        CreateApplicationRequest request = CreateApplicationRequest.builder()
            .competitionId(2L)
            .stageId(3L)
            .build();

        CreateApplicationResponse response = CreateApplicationResponse.builder()
            .id(1L)
            .competitionId(2L)
            .stageId(3L)
            .issuedBy(4L)
            .status(RequestStatus.PENDING)
            .build();

        when(applicationService.sendEnrollmentRequest(any(CreateApplicationRequest.class))).thenReturn(response);
        mockMvc.perform(post("/api/v1/competitions/applications")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.competitionId").value(2L))
            .andExpect(jsonPath("$.stageId").value(3L))
            .andExpect(jsonPath("$.status").value("PENDING"));

        verify(applicationService).sendEnrollmentRequest(any(CreateApplicationRequest.class));
    }

    @Test
    void createApplication_shouldReturnBadRequest_whenRequestIsInvalid() throws Exception {
        CreateApplicationRequest request = CreateApplicationRequest.builder().build();

        mockMvc.perform(post("/api/v1/competitions/applications")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void acceptRequest_shouldReturnCreated_whenApplicationIsPending() throws Exception {
        Long applicationId = 1L;

        ProcessApplicationResponse response = ProcessApplicationResponse.builder()
            .id(applicationId)
            .competitionId(2L)
            .stageId(3L)
            .issuedBy(4L)
            .issuedAt(Instant.parse("2026-06-25T10:00:00Z"))
            .processedBy(5L)
            .processedAt(Instant.parse("2026-06-26T10:00:00Z"))
            .status(RequestStatus.ACCEPTED)
            .build();

        when(applicationService.acceptRequest(applicationId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/competitions/applications/accept/{id}", applicationId)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(applicationId))
            .andExpect(jsonPath("$.competitionId").value(2L))
            .andExpect(jsonPath("$.stageId").value(3L))
            .andExpect(jsonPath("$.processedBy").value(5L))
            .andExpect(jsonPath("$.status").value("ACCEPTED"));

        verify(applicationService).acceptRequest(applicationId);
    }

    @Test
    void rejectRequest_shouldReturnOk_whenApplicationIsPending() throws Exception {
        Long applicationId = 1L;
        RejectEnrollmentRequest request = new RejectEnrollmentRequest("Does not meet requirements");

        ProcessApplicationResponse response = ProcessApplicationResponse.builder()
            .id(applicationId)
            .competitionId(2L)
            .stageId(3L)
            .issuedBy(4L)
            .issuedAt(Instant.parse("2026-06-25T10:00:00Z"))
            .processedBy(5L)
            .processedAt(Instant.parse("2026-06-26T10:00:00Z"))
            .status(RequestStatus.REJECTED)
            .rejectionReason("Does not meet requirements")
            .build();

        when(applicationService.rejectRequest(eq(applicationId), any(RejectEnrollmentRequest.class)))
            .thenReturn(response);

        mockMvc.perform(patch("/api/v1/competitions/applications/reject/{id}", applicationId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(applicationId))
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.rejectionReason").value("Does not meet requirements"));

        verify(applicationService).rejectRequest(eq(applicationId), any(RejectEnrollmentRequest.class));
    }

    @Test
    void cancelRequest_shouldReturnOk_whenApplicationIsPending() throws Exception {
        Long applicationId = 1L;

        ProcessApplicationResponse response = ProcessApplicationResponse.builder()
            .id(applicationId)
            .competitionId(2L)
            .stageId(3L)
            .issuedBy(4L)
            .issuedAt(Instant.parse("2026-06-25T10:00:00Z"))
            .processedBy(4L)
            .processedAt(Instant.parse("2026-06-26T10:00:00Z"))
            .status(RequestStatus.CANCELLED)
            .build();

        when(applicationService.cancelRequest(applicationId)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/competitions/applications/cancel/{id}", applicationId)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(applicationId))
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(applicationService).cancelRequest(applicationId);
    }

}