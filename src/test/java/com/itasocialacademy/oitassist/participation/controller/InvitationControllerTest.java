package com.itasocialacademy.oitassist.participation.controller;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.participation.dao.dto.request.CreateInvitationRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.request.RejectEnrollmentRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.response.*;
import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import com.itasocialacademy.oitassist.participation.service.interfaces.InvitationService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class InvitationControllerTest extends ControllerUnitTest<InvitationController> {
    private static final String ENROLLMENT_BASE_LINK = "/api/v1/enrollment/invitations/{id}";
    private static final String COMPETITION_BASE_LINK = "/api/v1/competitions/{compId}/stages/{stId}/invitations";

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
            .studentIds(List.of(10L, 11L)).build();

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

        when(invitationService.sendInvitationRequests(eq(2L), eq(3L), any(CreateInvitationRequest.class)))
            .thenReturn(response);

        mockMvc.perform(post(COMPETITION_BASE_LINK, 2L, 3L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.succeeded[0].id").value(1L))
            .andExpect(jsonPath("$.succeeded[0].studentId").value(10L))
            .andExpect(jsonPath("$.failed").isEmpty());

        verify(invitationService).sendInvitationRequests(eq(2L), eq(3L), any(CreateInvitationRequest.class));
    }

    @Test
    void sendEnrollmentRequest_shouldReturnCreated_withPartialFailures() throws Exception {
        CreateInvitationRequest request = CreateInvitationRequest.builder()
            .studentIds(List.of(10L, 11L)).build();

        CreateInvitationResponse response = CreateInvitationResponse.builder()
            .competitionId(2L)
            .stageId(3L)
            .succeeded(List.of(new SucceededInvitationResponse(1L, 10L)))
            .failed(List.of(new FailedInvitationResponse(11L, "Student already has a pending invitation")))
            .issuedBy(4L)
            .issuedAt(Instant.parse("2026-07-28T10:00:00Z"))
            .build();

        when(invitationService.sendInvitationRequests(eq(2L), eq(3L), any(CreateInvitationRequest.class)))
            .thenReturn(response);

        mockMvc.perform(post(COMPETITION_BASE_LINK, 2L, 3L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.succeeded[0].studentId").value(10L))
            .andExpect(jsonPath("$.failed[0].studentId").value(11L))
            .andExpect(jsonPath("$.failed[0].reason").value("Student already has a pending invitation"));

        verify(invitationService).sendInvitationRequests(eq(2L), eq(3L), any(CreateInvitationRequest.class));
    }

    @Test
    void sendEnrollmentRequest_shouldReturnCreated_whenAllStudentsFail() throws Exception {
        CreateInvitationRequest request = CreateInvitationRequest.builder()
            .studentIds(List.of(99L)).build();

        CreateInvitationResponse response = CreateInvitationResponse.builder()
            .competitionId(2L)
            .stageId(3L)
            .succeeded(List.of())
            .failed(List.of(new FailedInvitationResponse(99L, "Student not found")))
            .issuedBy(4L)
            .issuedAt(Instant.parse("2026-07-28T10:00:00Z"))
            .build();

        when(invitationService.sendInvitationRequests(eq(2L), eq(3L), any(CreateInvitationRequest.class)))
            .thenReturn(response);

        mockMvc.perform(post(COMPETITION_BASE_LINK, 2L, 3L)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.succeeded").isEmpty())
            .andExpect(jsonPath("$.failed[0].reason").value("Student not found"));

        verify(invitationService).sendInvitationRequests(eq(2L), eq(3L), any(CreateInvitationRequest.class));
    }

    @Test
    void sendEnrollmentRequest_shouldReturnBadRequest_whenRequestIsInvalid() throws Exception {
        CreateInvitationRequest request = CreateInvitationRequest.builder().build();

        mockMvc.perform(post(COMPETITION_BASE_LINK, "a", "z")
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

        mockMvc.perform(post(ENROLLMENT_BASE_LINK + "/accept", invitationId)
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

        mockMvc.perform(patch(ENROLLMENT_BASE_LINK + "/reject", invitationId)
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

        mockMvc.perform(patch(ENROLLMENT_BASE_LINK + "/cancel", invitationId)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(invitationId))
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(invitationService).cancelRequest(invitationId);
    }

    @Test
    void getEnrollmentRequests_shouldReturnOkWithPagedResults() throws Exception {
        InvitationListItemResponse item = new InvitationListItemResponse(
            1L, Instant.parse("2026-07-28T10:00:00Z"), RequestStatus.PENDING,
            new UserSummary("Test", "Test Surname", "test@mail.com"));

        Page<InvitationListItemResponse> page = new PageImpl<>(
            List.of(item), PageRequest.of(0, 20), 1);

        when(invitationService.getEnrollmentRequests(any(), any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(COMPETITION_BASE_LINK, 2L, 3L)
            .param("page", "0")
            .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].invitationId").value(1L))
            .andExpect(jsonPath("$.content[0].status").value("PENDING"))
            .andExpect(jsonPath("$.content[0].user.firstName").value("Test"))
            .andExpect(jsonPath("$.totalElements").value(1));

        verify(invitationService).getEnrollmentRequests(any(), any(), isNull(), any(Pageable.class));
    }

    @Test
    void getEnrollmentRequests_withSearchParam_shouldPassSearchToService() throws Exception {
        Page<InvitationListItemResponse> page = Page.empty(PageRequest.of(0, 20));

        when(invitationService.getEnrollmentRequests(any(), any(), eq("test"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(COMPETITION_BASE_LINK, 2L, 3L)
            .param("search", "test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty());

        verify(invitationService).getEnrollmentRequests(any(), any(), eq("test"), any(Pageable.class));
    }

    @Test
    void getEnrollmentRequests_noResults_shouldReturnEmptyPage() throws Exception {
        when(invitationService.getEnrollmentRequests(any(), any(), any(), any(Pageable.class)))
            .thenReturn(Page.empty());

        mockMvc.perform(get(COMPETITION_BASE_LINK, 2L, 3L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty())
            .andExpect(jsonPath("$.totalElements").value(0));
    }
}
