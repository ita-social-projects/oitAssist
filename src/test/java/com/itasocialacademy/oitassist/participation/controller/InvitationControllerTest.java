package com.itasocialacademy.oitassist.participation.controller;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.participation.dao.dto.request.CreateInvitationRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.request.RejectEnrollmentRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.response.CreateInvitationResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.FailedInvitationResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.ProcessInvitationResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.SucceededInvitationResponse;
import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import com.itasocialacademy.oitassist.participation.service.interfaces.InvitationService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InvitationControllerTest extends ControllerUnitTest<InvitationController> {
    @Mock
    private InvitationService invitationService;

    @InjectMocks
    private InvitationController invitationController;

    @Override
    protected InvitationController getController() {
        return invitationController;
    }

    @Test
    void sendEnrollmentRequest_shouldReturnCreated_whenAllStudentsSucceed() throws Exception {
        CreateInvitationRequest request = CreateInvitationRequest.builder()
            .competitionId(2L)
            .stageId(3L)
            .studentIds(List.of(10L, 11L))
            .build();

        CreateInvitationResponse response = CreateInvitationResponse.builder()
            .competitionId(2L)
            .stageId(3L)
            .succeeded(List.of(
                new SucceededInvitationResponse(1L, 10L),
                new SucceededInvitationResponse(2L, 11L)))
            .failed(List.of())
            .issuedBy(4L)
            .issuedAt(Instant.parse("2026-07-28T10:00:00Z"))
            .build();

        when(invitationService.sendEnrollmentRequest(any(CreateInvitationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/competitions/invitations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.succeeded[0].id").value(1L))
            .andExpect(jsonPath("$.succeeded[0].studentId").value(10L))
            .andExpect(jsonPath("$.failed").isEmpty());

        verify(invitationService).sendEnrollmentRequest(any(CreateInvitationRequest.class));
    }

    @Test
    void sendEnrollmentRequest_shouldReturnCreated_withPartialFailures() throws Exception {
        CreateInvitationRequest request = CreateInvitationRequest.builder()
            .competitionId(2L)
            .stageId(3L)
            .studentIds(List.of(10L, 11L))
            .build();

        CreateInvitationResponse response = CreateInvitationResponse.builder()
            .competitionId(2L)
            .stageId(3L)
            .succeeded(List.of(new SucceededInvitationResponse(1L, 10L)))
            .failed(List.of(new FailedInvitationResponse(11L, "Student already has a pending invitation")))
            .issuedBy(4L)
            .issuedAt(Instant.parse("2026-07-28T10:00:00Z"))
            .build();

        when(invitationService.sendEnrollmentRequest(any(CreateInvitationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/competitions/invitations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.succeeded[0].studentId").value(10L))
            .andExpect(jsonPath("$.failed[0].studentId").value(11L))
            .andExpect(jsonPath("$.failed[0].reason").value("Student already has a pending invitation"));

        verify(invitationService).sendEnrollmentRequest(any(CreateInvitationRequest.class));
    }

    @Test
    void sendEnrollmentRequest_shouldReturnCreated_whenAllStudentsFail() throws Exception {
        CreateInvitationRequest request = CreateInvitationRequest.builder()
            .competitionId(2L)
            .stageId(3L)
            .studentIds(List.of(99L))
            .build();

        CreateInvitationResponse response = CreateInvitationResponse.builder()
            .competitionId(2L)
            .stageId(3L)
            .succeeded(List.of())
            .failed(List.of(new FailedInvitationResponse(99L, "Student not found")))
            .issuedBy(4L)
            .issuedAt(Instant.parse("2026-07-28T10:00:00Z"))
            .build();

        when(invitationService.sendEnrollmentRequest(any(CreateInvitationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/competitions/invitations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.succeeded").isEmpty())
            .andExpect(jsonPath("$.failed[0].reason").value("Student not found"));

        verify(invitationService).sendEnrollmentRequest(any(CreateInvitationRequest.class));
    }

    @Test
    void sendEnrollmentRequest_shouldReturnBadRequest_whenRequestIsInvalid() throws Exception {
        CreateInvitationRequest request = CreateInvitationRequest.builder().build();

        mockMvc.perform(post("/api/v1/competitions/invitations")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void acceptRequest_shouldReturnCreated_whenInvitationIsPending() throws Exception {
        Long invitationId = 1L;

        ProcessInvitationResponse response = ProcessInvitationResponse.builder()
            .id(invitationId)
            .competitionId(2L)
            .stageId(3L)
            .issuedBy(4L)
            .issuedAt(Instant.parse("2026-07-25T10:00:00Z"))
            .studentId(5L)
            .processedAt(Instant.parse("2026-07-28T10:00:00Z"))
            .status(RequestStatus.ACCEPTED)
            .build();

        when(invitationService.acceptRequest(invitationId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/competitions/invitations/accept/{id}", invitationId)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(invitationId))
            .andExpect(jsonPath("$.competitionId").value(2L))
            .andExpect(jsonPath("$.stageId").value(3L))
            .andExpect(jsonPath("$.studentId").value(5L))
            .andExpect(jsonPath("$.status").value("ACCEPTED"));

        verify(invitationService).acceptRequest(invitationId);
    }

    @Test
    void rejectRequest_shouldReturnOk_whenInvitationIsPending() throws Exception {
        Long invitationId = 1L;
        RejectEnrollmentRequest request = new RejectEnrollmentRequest("Does not meet requirements");

        ProcessInvitationResponse response = ProcessInvitationResponse.builder()
            .id(invitationId)
            .competitionId(2L)
            .stageId(3L)
            .issuedBy(4L)
            .issuedAt(Instant.parse("2026-07-25T10:00:00Z"))
            .studentId(5L)
            .processedAt(Instant.parse("2026-07-28T10:00:00Z"))
            .status(RequestStatus.REJECTED)
            .rejectionReason("Does not meet requirements")
            .build();

        when(invitationService.rejectRequest(eq(invitationId), any(RejectEnrollmentRequest.class)))
            .thenReturn(response);

        mockMvc.perform(patch("/api/v1/competitions/invitations/reject/{id}", invitationId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(invitationId))
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.rejectionReason").value("Does not meet requirements"));

        verify(invitationService).rejectRequest(eq(invitationId), any(RejectEnrollmentRequest.class));
    }

    @Test
    void cancelRequest_shouldReturnOk_whenInvitationIsPending() throws Exception {
        Long invitationId = 1L;

        ProcessInvitationResponse response = ProcessInvitationResponse.builder()
            .id(invitationId)
            .competitionId(2L)
            .stageId(3L)
            .issuedBy(4L)
            .issuedAt(Instant.parse("2026-07-25T10:00:00Z"))
            .studentId(4L)
            .processedAt(Instant.parse("2026-07-28T10:00:00Z"))
            .status(RequestStatus.CANCELLED)
            .build();

        when(invitationService.cancelRequest(invitationId)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/competitions/invitations/cancel/{id}", invitationId)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(invitationId))
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(invitationService).cancelRequest(invitationId);
    }
}
