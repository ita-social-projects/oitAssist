package com.itasocialacademy.oitassist.participation.service;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.participation.dao.repository.ParticipationRepository;
import com.itasocialacademy.oitassist.participation.service.interfaces.ParticipationStatusChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ParticipationStatusCheckerImpl implements ParticipationStatusChecker {
    private final ParticipationRepository repository;

    /**
     * {@inheritDoc}
     *
     * <p>
     * Returns {@code false} for an empty status list without querying the database.
     * </p>
     */
    @Override
    public boolean isUserParticipantWithCompetitionStatus(Long userId, List<CompetitionStatus> statuses) {
        if (statuses.isEmpty()) {
            return false;
        }

        return repository.existsByUserIdAndCompetitionStatusIn(userId, statuses);
    }
}