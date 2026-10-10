package com.itasocialacademy.oitassist.competitionhistory.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import com.itasocialacademy.oitassist.competitionhistory.dto.response.CompetitionHistoryResponse;
import com.itasocialacademy.oitassist.competitionhistory.service.interfaces.CompetitionHistoryService;
import com.itasocialacademy.oitassist.core.exceptions.InsufficientPermissionsException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

class CompetitionHistoryControllerTest extends ControllerUnitTest<CompetitionHistoryController> {
    private static final String HISTORY_URL = "/api/v1/users/{userId}/competitions/history";
    private static final Long USER_ID = 1L;

    @Mock
    private CompetitionHistoryService competitionHistoryService;

    @InjectMocks
    private CompetitionHistoryController competitionHistoryController;

    @Override
    protected CompetitionHistoryController getController() {
        return competitionHistoryController;
    }

    @Test
    void getHistory_existingHistory_shouldReturn200WithPage() throws Exception {
        String title = "Всеукраїнська Олімпіада 2026";
        ZonedDateTime dateStart = ZonedDateTime.of(2026, 1, 10, 9, 0, 0, 0, ZoneId.of("UTC"));
        CompetitionHistoryResponse item = CompetitionHistoryResponse.builder()
            .id(10L)
            .title(title)
            .dateStart(dateStart)
            .dateFinish(dateStart.plusDays(10))
            .build();
        Page<CompetitionHistoryResponse> page = new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1);

        when(competitionHistoryService.getHistory(eq(USER_ID), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(HISTORY_URL, USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].id").value(10L))
            .andExpect(jsonPath("$.content[0].title").value(title))
            .andExpect(jsonPath("$.content[0].dateStart").exists())
            .andExpect(jsonPath("$.content[0].dateFinish").exists())
            .andExpect(jsonPath("$.pageNumber").value(0))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getHistory_noHistory_shouldReturnEmptyPage() throws Exception {
        when(competitionHistoryService.getHistory(eq(USER_ID), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get(HISTORY_URL, USER_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty())
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getHistory_noPageParams_shouldUseDefaultPageable() throws Exception {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(competitionHistoryService.getHistory(eq(USER_ID), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get(HISTORY_URL, USER_ID))
            .andExpect(status().isOk());

        verify(competitionHistoryService).getHistory(eq(USER_ID), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, "dateFinish"), pageable.getSort());
    }

    @Test
    void getHistory_customPageParams_shouldPassThemToService() throws Exception {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(competitionHistoryService.getHistory(eq(USER_ID), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get(HISTORY_URL, USER_ID)
            .param("page", "2")
            .param("size", "5")
            .param("sort", "dateStart,asc"))
            .andExpect(status().isOk());

        verify(competitionHistoryService).getHistory(eq(USER_ID), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(2, pageable.getPageNumber());
        assertEquals(5, pageable.getPageSize());
        assertEquals(Sort.by(Sort.Direction.ASC, "dateStart"), pageable.getSort());
    }

    @Test
    void getHistory_notOwnerAndNotAdmin_shouldReturn403() throws Exception {
        when(competitionHistoryService.getHistory(eq(USER_ID), any(Pageable.class)))
            .thenThrow(new InsufficientPermissionsException());

        mockMvc.perform(get(HISTORY_URL, USER_ID))
            .andExpect(status().isForbidden());
    }

    @Test
    void getHistory_invalidUserId_shouldReturn400() throws Exception {
        mockMvc.perform(get(HISTORY_URL, "abc"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(competitionHistoryService);
    }
}