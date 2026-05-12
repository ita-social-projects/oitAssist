package com.itasocialacademy.oitassist.usercompetition.service;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.usercompetition.dao.repository.UserCompetitionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit test for UserCompetitionServiceImpl")
public class UserCompetitionServiceImplTest {
    @Mock
    private UserCompetitionRepository userCompetitionRepository;

    @InjectMocks
    private UserCompetitionServiceImpl userCompetitionServiceImpl;

    @Test
    @DisplayName("Should return true when user has active competitions")
    void hasActiveCompetitions_ShouldReturnTrue_ifUserHasActiveCompetitions() {
        Long userId = 1L;
        List<CompetitionStatus> statuses = List.of(CompetitionStatus.INCOMING, CompetitionStatus.INPROGRESS);
        List<String> statusStrings = statuses.stream().map(Enum::name).toList();

        when(userCompetitionRepository.existsByUserIdAndStatusIn(userId, statusStrings)).thenReturn(true);

        boolean result = userCompetitionServiceImpl.hasActiveCompetitions(userId, statuses);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false when user has no active competitions")
    void hasActiveCompetitions_ShouldReturnFalse_WhenUserHasNoActiveCompetitions() {
        Long userId = 1L;
        List<CompetitionStatus> statuses = List.of(CompetitionStatus.INCOMING, CompetitionStatus.INPROGRESS);
        List<String> statusStrings = statuses.stream().map(Enum::name).toList();

        when(userCompetitionRepository.existsByUserIdAndStatusIn(userId, statusStrings)).thenReturn(false);

        boolean result = userCompetitionServiceImpl.hasActiveCompetitions(userId, statuses);

        assertThat(result).isFalse();
    }
}
