package com.itasocialacademy.oitassist.participation.controller;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.participation.dao.dto.request.AcceptApplicationListRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.request.RejectApplicationListRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.request.RejectEnrollmentRequest;
import com.itasocialacademy.oitassist.participation.dao.dto.response.*;
import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import com.itasocialacademy.oitassist.participation.service.interfaces.ApplicationService;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class ApplicationControllerTest extends ControllerUnitTest<ApplicationController> {
    private static final String ENROLLMENT_BASE_LINK = "/api/v1/enrollment/applications/{id}";
    private static final String ENROLLMENT_BATCH_BASE_LINK = "/api/v1/enrollment/applications";
    private static final String COMPETITION_BASE_LINK = "/api/v1/competitions/{compId}/stages/{stId}/applications";

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
        CreateApplicationResponse response = CreateApplicationResponse.builder()
            .id(1L)
            .competitionId(2L)
            .stageId(3L)
            .issuedBy(4L)
            .status(RequestStatus.PENDING)
            .build();

        when(applicationService.sendApplicationRequest(2L, 3L)).thenReturn(response);

        mockMvc.perform(post(COMPETITION_BASE_LINK, 2L, 3L))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.competitionId").value(2L))
            .andExpect(jsonPath("$.stageId").value(3L))
            .andExpect(jsonPath("$.status").value("PENDING"));

        verify(applicationService).sendApplicationRequest(2L, 3L);
    }

    @Test
    void createApplication_shouldReturnBadRequest_whenRequestIsInvalid() throws Exception {
        mockMvc.perform(post(COMPETITION_BASE_LINK, "a", "z"))
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

        mockMvc.perform(post(ENROLLMENT_BASE_LINK + "/accept", applicationId)
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
    void acceptRequests_shouldReturnCreated_whenAllSucceed() throws Exception {
        AcceptApplicationListRequest request = new AcceptApplicationListRequest(List.of(1L, 2L));

        AcceptedApplicationListResponse response = AcceptedApplicationListResponse.builder()
            .application(new ApplicationDecisionSummary(2L, 3L, 4L, Instant.parse("2026-07-28T10:00:00Z")))
            .succeeded(List.of(
                new SucceededApplicationAcceptingItemResponse(1L, 10L, RequestStatus.ACCEPTED),
                new SucceededApplicationAcceptingItemResponse(2L, 11L, RequestStatus.ACCEPTED)))
            .failed(List.of())
            .build();

        when(applicationService.acceptApplicationList(any(AcceptApplicationListRequest.class))).thenReturn(response);

        mockMvc.perform(post(ENROLLMENT_BATCH_BASE_LINK + "/accept-batch")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.succeeded").isArray())
            .andExpect(jsonPath("$.succeeded[0].applicationId").value(1L))
            .andExpect(jsonPath("$.succeeded[0].status").value("ACCEPTED"))
            .andExpect(jsonPath("$.succeeded[1].applicationId").value(2L))
            .andExpect(jsonPath("$.failed").isEmpty());

        verify(applicationService).acceptApplicationList(any(AcceptApplicationListRequest.class));
    }

    @Test
    void acceptRequests_shouldReturnCreated_withPartialFailures() throws Exception {
        AcceptApplicationListRequest request = new AcceptApplicationListRequest(List.of(1L, 99L));

        AcceptedApplicationListResponse response = AcceptedApplicationListResponse.builder()
            .application(new ApplicationDecisionSummary(2L, 3L, 4L, Instant.parse("2026-07-28T10:00:00Z")))
            .succeeded(List.of(new SucceededApplicationAcceptingItemResponse(1L, 10L, RequestStatus.ACCEPTED)))
            .failed(List.of(new FailedApplicationDecisionItemResponse(99L, "Application not found")))
            .build();

        when(applicationService.acceptApplicationList(any(AcceptApplicationListRequest.class))).thenReturn(response);

        mockMvc.perform(post(ENROLLMENT_BATCH_BASE_LINK + "/accept-batch")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.succeeded[0].applicationId").value(1L))
            .andExpect(jsonPath("$.failed[0].applicationId").value(99L))
            .andExpect(jsonPath("$.failed[0].reason").value("Application not found"));

        verify(applicationService).acceptApplicationList(any(AcceptApplicationListRequest.class));
    }

    @Test
    void acceptRequests_shouldReturnCreated_whenAllFail() throws Exception {
        AcceptApplicationListRequest request = new AcceptApplicationListRequest(List.of(1L));

        AcceptedApplicationListResponse response = AcceptedApplicationListResponse.builder()
            .application(new ApplicationDecisionSummary(2L, 3L, 4L, Instant.parse("2026-07-28T10:00:00Z")))
            .succeeded(List.of())
            .failed(List.of(new FailedApplicationDecisionItemResponse(1L, "Application is not pending")))
            .build();

        when(applicationService.acceptApplicationList(any(AcceptApplicationListRequest.class))).thenReturn(response);

        mockMvc.perform(post(ENROLLMENT_BATCH_BASE_LINK + "/accept-batch")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.succeeded").isEmpty())
            .andExpect(jsonPath("$.failed[0].reason").value("Application is not pending"));
    }

    @Test
    void acceptRequests_shouldReturnBadRequest_whenRequestIsInvalid() throws Exception {
        AcceptApplicationListRequest request = new AcceptApplicationListRequest(List.of());

        mockMvc.perform(post(ENROLLMENT_BATCH_BASE_LINK + "/accept-batch")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
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

        mockMvc.perform(patch(ENROLLMENT_BASE_LINK + "/reject", applicationId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(applicationId))
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.rejectionReason").value("Does not meet requirements"));

        verify(applicationService).rejectRequest(eq(applicationId), any(RejectEnrollmentRequest.class));
    }

    @Test
    void rejectRequests_shouldReturnCreated_whenAllSucceed() throws Exception {
        RejectApplicationListRequest request = new RejectApplicationListRequest(List.of(1L, 2L), "Reason");

        RejectedApplicationListResponse response = RejectedApplicationListResponse.builder()
            .application(new ApplicationDecisionSummary(2L, 3L, 4L, Instant.parse("2026-07-28T10:00:00Z")))
            .succeeded(List.of(
                new SucceededApplicationRejectingItemResponse(1L, 10L, RequestStatus.REJECTED),
                new SucceededApplicationRejectingItemResponse(2L, 11L, RequestStatus.REJECTED)))
            .failed(List.of())
            .build();

        when(applicationService.rejectApplicationList(any(RejectApplicationListRequest.class))).thenReturn(response);

        mockMvc.perform(post(ENROLLMENT_BATCH_BASE_LINK + "/reject-batch")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.succeeded").isArray())
            .andExpect(jsonPath("$.succeeded[0].applicationId").value(1L))
            .andExpect(jsonPath("$.succeeded[0].status").value("REJECTED"))
            .andExpect(jsonPath("$.succeeded[1].applicationId").value(2L))
            .andExpect(jsonPath("$.failed").isEmpty());

        verify(applicationService).rejectApplicationList(any(RejectApplicationListRequest.class));
    }

    @Test
    void rejectRequests_shouldReturnCreated_withPartialFailures() throws Exception {
        RejectApplicationListRequest request = new RejectApplicationListRequest(List.of(1L, 99L), "Reason");

        RejectedApplicationListResponse response = RejectedApplicationListResponse.builder()
            .application(new ApplicationDecisionSummary(2L, 3L, 4L, Instant.parse("2026-07-28T10:00:00Z")))
            .succeeded(List.of(new SucceededApplicationRejectingItemResponse(1L, 10L, RequestStatus.REJECTED)))
            .failed(List.of(new FailedApplicationDecisionItemResponse(99L, "Application not found")))
            .build();

        when(applicationService.rejectApplicationList(any(RejectApplicationListRequest.class))).thenReturn(response);

        mockMvc.perform(post(ENROLLMENT_BATCH_BASE_LINK + "/reject-batch")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.succeeded[0].applicationId").value(1L))
            .andExpect(jsonPath("$.failed[0].applicationId").value(99L))
            .andExpect(jsonPath("$.failed[0].reason").value("Application not found"));

        verify(applicationService).rejectApplicationList(any(RejectApplicationListRequest.class));
    }

    @Test
    void rejectRequests_shouldReturnCreated_whenAllFail() throws Exception {
        RejectApplicationListRequest request = new RejectApplicationListRequest(List.of(1L), "Reason");

        RejectedApplicationListResponse response = RejectedApplicationListResponse.builder()
            .application(new ApplicationDecisionSummary(2L, 3L, 4L, Instant.parse("2026-07-28T10:00:00Z")))
            .succeeded(List.of())
            .failed(List.of(new FailedApplicationDecisionItemResponse(1L, "Application is not pending")))
            .build();

        when(applicationService.rejectApplicationList(any(RejectApplicationListRequest.class))).thenReturn(response);

        mockMvc.perform(post(ENROLLMENT_BATCH_BASE_LINK + "/reject-batch")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.succeeded").isEmpty())
            .andExpect(jsonPath("$.failed[0].reason").value("Application is not pending"));
    }

    @Test
    void rejectRequests_shouldReturnBadRequest_whenRequestIsInvalid() throws Exception {
        RejectApplicationListRequest request = new RejectApplicationListRequest(List.of(), "Reason");

        mockMvc.perform(post(ENROLLMENT_BATCH_BASE_LINK + "/reject-batch")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
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

        mockMvc.perform(patch(ENROLLMENT_BASE_LINK + "/cancel", applicationId)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(applicationId))
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(applicationService).cancelRequest(applicationId);
    }

    @Test
    void getEnrollmentRequests_shouldReturnOkWithPagedResults() throws Exception {
        ApplicationListItemResponse item = new ApplicationListItemResponse(
            1L, Instant.parse("2026-07-28T10:00:00Z"), RequestStatus.PENDING,
            new UserSummary("Test", "Test Surname", "test@mail.com"));

        Page<ApplicationListItemResponse> page = new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1);

        when(applicationService.getEnrollmentRequests(any(), any(), any(), any(Pageable.class)))
            .thenReturn(page);

        mockMvc.perform(get(COMPETITION_BASE_LINK, 2L, 3L)
            .param("page", "0")
            .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].applicationId").value(1L))
            .andExpect(jsonPath("$.content[0].status").value("PENDING"))
            .andExpect(jsonPath("$.content[0].user.firstName").value("Test"))
            .andExpect(jsonPath("$.totalElements").value(1));

        verify(applicationService).getEnrollmentRequests(any(), any(), isNull(), any(Pageable.class));
    }

    @Test
    void getEnrollmentRequests_withSearchParam_shouldPassSearchToService() throws Exception {
        when(applicationService.getEnrollmentRequests(any(), any(), eq("test"), any(Pageable.class)))
            .thenReturn(Page.empty(PageRequest.of(0, 20)));

        mockMvc.perform(get(COMPETITION_BASE_LINK, 2L, 3L)
            .param("search", "test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty());

        verify(applicationService).getEnrollmentRequests(any(), any(), eq("test"),
            any(Pageable.class));
    }

    @Test
    void getEnrollmentRequests_noResults_shouldReturnEmptyPage() throws Exception {
        when(applicationService.getEnrollmentRequests(any(), any(), any(), any(Pageable.class)))
            .thenReturn(Page.empty());

        mockMvc.perform(get(COMPETITION_BASE_LINK, 2L, 3L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty())
            .andExpect(jsonPath("$.totalElements").value(0));
    }
}