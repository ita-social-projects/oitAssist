package com.itasocialacademy.oitassist.competition.service;

import com.itasocialacademy.oitassist.competition.dao.dto.request.CreateCompetitionRequest;
import com.itasocialacademy.oitassist.competition.dao.dto.response.CompetitionResponse;
import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import com.itasocialacademy.oitassist.competition.dao.repository.CompetitionRepository;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionHierarchyValidationException;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionNotFoundException;
import com.itasocialacademy.oitassist.competition.mapper.CompetitionMapper;
import com.itasocialacademy.oitassist.competition.service.interfaces.CompetitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompetitionServiceImpl implements CompetitionService {
    private final CompetitionRepository competitionRepository;
    private final CompetitionMapper mapper;

    @Override
    @Transactional
    public CompetitionResponse create(CreateCompetitionRequest request) {
        Competition competition = mapper.toEntity(request);
        competition.setCompetitionStatus(CompetitionStatus.DRAFT);

        return mapper.toResponse(competitionRepository.save(competition));
    }

    @Override
    @Transactional(readOnly = true)
    public CompetitionResponse getById(Long id) {
        Competition competition = competitionRepository.findById(id)
            .orElseThrow(() -> new CompetitionNotFoundException("Competition with id " + id + " not found"));
        return mapper.toResponse(competition);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateHierarchyImmutability(Long competitionId) {
        Competition competition = competitionRepository.findById(competitionId)
            .orElseThrow(() -> new CompetitionNotFoundException("Competition with id " + competitionId + " not found"));

        if (competition.getCompetitionStatus() == CompetitionStatus.PUBLISHED) {
            // TODO: Epic Requirement - "restricted if active participations exist"
            // STUB for future integration w ParticipationRequest
            boolean hasActiveParticipations = false;

            if (hasActiveParticipations) {
                throw new CompetitionHierarchyValidationException(
                    "Cannot modify hierarchy: The competition is PUBLISHED and has active participations."
                );
            }
        }
    }
}
