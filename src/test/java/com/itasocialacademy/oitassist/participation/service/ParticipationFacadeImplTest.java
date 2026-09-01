package com.itasocialacademy.oitassist.participation.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itasocialacademy.oitassist.participation.dao.repository.ParticipationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParticipationFacadeImplTest {

    private static final Long USER_ID = 100L;
    private static final Long COMPETITION_ID = 200L;
    private static final Long STAGE_ID = 300L;

    @Mock
    private ParticipationRepository participationRepository;

    @InjectMocks
    private ParticipationFacadeImpl participationFacade;

    @Test
    void isUserParticipant_existingParticipation_shouldReturnTrue() {
        when(participationRepository
            .existsByUserIdAndCompetitionIdAndStageId(
                USER_ID,
                COMPETITION_ID,
                STAGE_ID))
            .thenReturn(true);

        boolean result = participationFacade.isUserParticipant(
            USER_ID,
            COMPETITION_ID,
            STAGE_ID);

        assertTrue(result);
    }

    @Test
    void isUserParticipant_missingParticipation_shouldReturnFalse() {
        when(participationRepository
            .existsByUserIdAndCompetitionIdAndStageId(
                USER_ID,
                COMPETITION_ID,
                STAGE_ID))
            .thenReturn(false);

        boolean result = participationFacade.isUserParticipant(
            USER_ID,
            COMPETITION_ID,
            STAGE_ID);

        assertFalse(result);
    }

    @Test
    void isUserParticipant_shouldDelegateAllIdentifiersToRepository() {
        participationFacade.isUserParticipant(
            USER_ID,
            COMPETITION_ID,
            STAGE_ID);

        verify(participationRepository)
            .existsByUserIdAndCompetitionIdAndStageId(
                USER_ID,
                COMPETITION_ID,
                STAGE_ID);
    }
}