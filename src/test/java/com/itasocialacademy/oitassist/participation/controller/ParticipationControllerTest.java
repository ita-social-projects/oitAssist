package com.itasocialacademy.oitassist.participation.controller;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.participation.dao.dto.response.ParticipationListItemResponse;
import com.itasocialacademy.oitassist.participation.dao.dto.response.UserSummary;
import com.itasocialacademy.oitassist.participation.service.interfaces.ParticipationService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ParticipationControllerTest extends ControllerUnitTest<ParticipationController> {
    private static final String COMPETITION_BASE_LINK = "/api/v1/competitions/{compId}/stages/{stId}/participants";

    @Mock
    private ParticipationService participationService;

    @InjectMocks
    private ParticipationController participationController;

    @Override
    protected ParticipationController getController() {
        return participationController;
    }

    @Test
    void getParticipationList_shouldReturnOkWithPagedResults() throws Exception {
        ParticipationListItemResponse item = new ParticipationListItemResponse(
            1L, 2L, new UserSummary("Test", "Test Surname", "test@mail.com"));

        Page<ParticipationListItemResponse> page = new PageImpl<>(
            List.of(item), PageRequest.of(0, 20), 1);

        when(participationService.getParticipationList(any(), any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(COMPETITION_BASE_LINK, 2L, 3L)
            .param("page", "0")
            .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].participationId").value(1L))
            .andExpect(jsonPath("$.content[0].studentId").value(2L))
            .andExpect(jsonPath("$.content[0].user.firstName").value("Test"))
            .andExpect(jsonPath("$.totalElements").value(1));

        verify(participationService).getParticipationList(any(), any(), isNull(), any(Pageable.class));
    }

    @Test
    void getParticipationList_withSearchParam_shouldPassSearchToService() throws Exception {
        Page<ParticipationListItemResponse> page = Page.empty(PageRequest.of(0, 20));

        when(participationService.getParticipationList(any(), any(), eq("test"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(COMPETITION_BASE_LINK, 2L, 3L)
            .param("search", "test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty());

        verify(participationService).getParticipationList(any(), any(), eq("test"), any(Pageable.class));
    }

    @Test
    void getParticipationList_noResults_shouldReturnEmptyPage() throws Exception {
        when(participationService.getParticipationList(any(), any(), any(), any(Pageable.class)))
            .thenReturn(Page.empty());

        mockMvc.perform(get(COMPETITION_BASE_LINK, 2L, 3L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty())
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getParticipationList_invalidCompetitionId_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get(COMPETITION_BASE_LINK, "abc", 3L))
            .andExpect(status().isBadRequest());
    }
}
