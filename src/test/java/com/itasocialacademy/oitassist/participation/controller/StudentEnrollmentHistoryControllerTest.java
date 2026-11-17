package com.itasocialacademy.oitassist.participation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.competition.dao.enums.StageScope;
import com.itasocialacademy.oitassist.core.exceptions.InsufficientPermissionsException;
import com.itasocialacademy.oitassist.participation.dao.dto.response.ApplicationHistoryItemResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.CompetitionSummary;
import com.itasocialacademy.oitassist.participation.dao.dto.response.InvitationHistoryItemResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.ParticipationHistoryItemResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.StageSummary;
import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import com.itasocialacademy.oitassist.participation.service.interfaces.StudentEnrollmentHistoryService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class StudentEnrollmentHistoryControllerTest extends ControllerUnitTest<StudentEnrollmentHistoryController> {
    private static final String INVITATIONS_URL = "/api/v1/users/{userId}/invitations";
    private static final String APPLICATIONS_URL = "/api/v1/users/{userId}/applications";
    private static final String PARTICIPATIONS_URL = "/api/v1/users/{userId}/participations";
    private static final Long USER_ID = 4L;
    private static final Instant ISSUED_AT = Instant.parse("2026-06-07T09:50:30Z");

    @Mock
    private StudentEnrollmentHistoryService historyService;

    @InjectMocks
    private StudentEnrollmentHistoryController historyController;

    @Override
    protected StudentEnrollmentHistoryController getController() {
        return historyController;
    }

    @Test
    void getInvitations_existingItems_shouldReturn200WithPage() throws Exception {
        InvitationHistoryItemResponse item = new InvitationHistoryItemResponse(
            1L,
            ISSUED_AT,
            RequestStatus.PENDING,
            new CompetitionSummary(10L, "Olympiad"),
            new StageSummary(20L, "City stage", StageScope.CITY));
        Page<InvitationHistoryItemResponse> page = new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1);
        when(historyService.getInvitations(eq(USER_ID), isNull(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(INVITATIONS_URL, USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].invitationId").value(1L))
            .andExpect(jsonPath("$.content[0].issuedAt").exists())
            .andExpect(jsonPath("$.content[0].status").value("PENDING"))
            .andExpect(jsonPath("$.content[0].competition.title").value("Olympiad"))
            .andExpect(jsonPath("$.content[0].stage.scope").value("CITY"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getInvitations_noParams_shouldUseDefaultPageableAndNullStatus() throws Exception {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(historyService.getInvitations(eq(USER_ID), isNull(), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get(INVITATIONS_URL, USER_ID))
            .andExpect(status().isOk());

        verify(historyService).getInvitations(eq(USER_ID), isNull(), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, "issuedAt"), pageable.getSort());
    }

    @Test
    void getInvitations_statusParam_shouldPassStatusToService() throws Exception {
        when(historyService.getInvitations(eq(USER_ID), eq(RequestStatus.PENDING), any(Pageable.class)))
            .thenReturn(Page.empty());

        mockMvc.perform(get(INVITATIONS_URL, USER_ID).param("status", "PENDING"))
            .andExpect(status().isOk());

        verify(historyService).getInvitations(eq(USER_ID), eq(RequestStatus.PENDING), any(Pageable.class));
    }

    @Test
    void getInvitations_invalidStatus_shouldReturn400() throws Exception {
        mockMvc.perform(get(INVITATIONS_URL, USER_ID).param("status", "FOO"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(historyService);
    }

    @Test
    void getInvitations_serviceThrowsInsufficientPermissions_shouldReturn403() throws Exception {
        when(historyService.getInvitations(eq(USER_ID), isNull(), any(Pageable.class)))
            .thenThrow(new InsufficientPermissionsException());

        mockMvc.perform(get(INVITATIONS_URL, USER_ID))
            .andExpect(status().isForbidden());
    }

    @Test
    void getApplications_existingItems_shouldReturn200WithPage() throws Exception {
        ApplicationHistoryItemResponse item = new ApplicationHistoryItemResponse(
            2L,
            ISSUED_AT,
            RequestStatus.ACCEPTED,
            new CompetitionSummary(10L, "Olympiad"),
            new StageSummary(20L, "City stage", StageScope.CITY));
        Page<ApplicationHistoryItemResponse> page = new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1);
        when(historyService.getApplications(eq(USER_ID), eq(RequestStatus.ACCEPTED), any(Pageable.class)))
            .thenReturn(page);

        mockMvc.perform(get(APPLICATIONS_URL, USER_ID).param("status", "ACCEPTED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].applicationId").value(2L))
            .andExpect(jsonPath("$.content[0].issuedAt").exists())
            .andExpect(jsonPath("$.content[0].status").value("ACCEPTED"))
            .andExpect(jsonPath("$.content[0].competition.id").value(10L))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getApplications_noParams_shouldUseDefaultPageableAndNullStatus() throws Exception {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(historyService.getApplications(eq(USER_ID), isNull(), any(Pageable.class)))
            .thenReturn(Page.empty());

        mockMvc.perform(get(APPLICATIONS_URL, USER_ID))
            .andExpect(status().isOk());

        verify(historyService).getApplications(eq(USER_ID), isNull(), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(20, pageable.getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, "issuedAt"), pageable.getSort());
    }

    @Test
    void getParticipations_noParams_shouldUseDefaultPageable() throws Exception {
        ParticipationHistoryItemResponse item = new ParticipationHistoryItemResponse(
            3L,
            new CompetitionSummary(10L, "Olympiad"),
            new StageSummary(20L, "City stage", StageScope.CITY));
        Page<ParticipationHistoryItemResponse> page = new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(historyService.getParticipations(eq(USER_ID), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(PARTICIPATIONS_URL, USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].participationId").value(3L))
            .andExpect(jsonPath("$.content[0].stage.title").value("City stage"))
            .andExpect(jsonPath("$.totalElements").value(1));

        verify(historyService).getParticipations(eq(USER_ID), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(20, pageable.getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, "id"), pageable.getSort());
    }

    @Test
    void getParticipations_serviceThrowsInsufficientPermissions_shouldReturn403() throws Exception {
        when(historyService.getParticipations(eq(USER_ID), any(Pageable.class)))
            .thenThrow(new InsufficientPermissionsException());

        mockMvc.perform(get(PARTICIPATIONS_URL, USER_ID))
            .andExpect(status().isForbidden());
    }
}