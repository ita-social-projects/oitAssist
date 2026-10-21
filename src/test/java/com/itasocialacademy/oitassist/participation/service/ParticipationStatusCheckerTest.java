package com.itasocialacademy.oitassist.participation.service;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.participation.dao.repository.ParticipationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParticipationStatusCheckerTest {
    @Mock
    private ParticipationRepository participationRepository;

    @InjectMocks
    private ParticipationStatusCheckerImpl participationStatusCheckerImpl;

    @Test
    void isUserParticipantWithCompetitionStatus_emptyStatuses_shouldReturnFalse() {
        Long currentUserId = 1L;
        List<CompetitionStatus> competitionStatuses = List.of();

        boolean result =
            participationStatusCheckerImpl.isUserParticipantWithCompetitionStatus(currentUserId, competitionStatuses);

        assertFalse(result);
    }

    @Test
    void isUserParticipantWithCompetitionStatus_noMatchingParticipation_shouldReturnFalse() {
        Long currentUserId = 1L;
        List<CompetitionStatus> competitionStatuses = List.of(CompetitionStatus.ENROLLMENT);

        when(participationRepository.existsByUserIdAndCompetitionStatusIn(currentUserId, competitionStatuses))
            .thenReturn(false);

        boolean result =
            participationStatusCheckerImpl.isUserParticipantWithCompetitionStatus(currentUserId, competitionStatuses);

        assertFalse(result);
    }

    @Test
    void isUserParticipantWithCompetitionStatus_matchingParticipationExists_shouldReturnTrue() {
        Long currentUserId = 1L;
        List<CompetitionStatus> competitionStatuses = List.of(CompetitionStatus.ENROLLMENT);

        when(participationRepository.existsByUserIdAndCompetitionStatusIn(currentUserId, competitionStatuses))
            .thenReturn(true);

        boolean result =
            participationStatusCheckerImpl.isUserParticipantWithCompetitionStatus(currentUserId, competitionStatuses);

        assertTrue(result);
        verify(participationRepository).existsByUserIdAndCompetitionStatusIn(currentUserId, competitionStatuses);
    }
}
