package com.itasocialacademy.oitassist.competition.service;

import com.itasocialacademy.oitassist.competition.dao.dto.request.CreateCompetitionRequest;
import com.itasocialacademy.oitassist.competition.dao.dto.response.CompetitionResponse;
import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import com.itasocialacademy.oitassist.competition.dao.repository.CompetitionRepository;
import com.itasocialacademy.oitassist.competition.dao.repository.StageRepository;
import com.itasocialacademy.oitassist.competition.dao.specification.CompetitionSpecification;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionHierarchyValidationException;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionNotFoundException;
import com.itasocialacademy.oitassist.competition.mapper.CompetitionMapper;
import com.itasocialacademy.oitassist.competition.service.interfaces.CompetitionService;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompetitionServiceImpl implements CompetitionService {
    private final CompetitionRepository competitionRepository;
    private final CompetitionMapper mapper;
    private final SecurityFacade securityFacade;
    private final StageRepository stageRepository;

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
    public CompetitionResponse getVisibleById(Long id) {
        Competition competition = competitionRepository.findById(id)
            .orElseThrow(() -> new CompetitionNotFoundException("Competition with id " + id + " not found"));
        boolean isAdmin = securityFacade.hasRole("ADMIN");
        boolean isOwner = securityFacade.hasRole("ORG") &&
            competition.getCreatedBy().equals(securityFacade.getCurrentUserId().orElse(-1L));

        if (competition.getCompetitionStatus() == CompetitionStatus.DRAFT) {
            if (!isAdmin && !isOwner) {
                throw new AccessDeniedException("You do not have permission to view this draft competition");
            }
        }
        return mapper.toResponse(competition);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateHierarchyImmutability(Long competitionId) {
        Competition competition = competitionRepository.findById(competitionId)
            .orElseThrow(() -> new CompetitionNotFoundException("Competition with id " + competitionId + " not found"));

        if (competition.getCompetitionStatus() == CompetitionStatus.ARCHIVED) {
            throw new CompetitionHierarchyValidationException(
                "Cannot modify hierarchy: Competition is ARCHIVED (read-only).");
        }

        if (competition.getCompetitionStatus() == CompetitionStatus.PUBLISHED ||
            competition.getCompetitionStatus() == CompetitionStatus.FINISHED) {
            // TODO: Epic Requirement - "restricted if active participations exist"
            // STUB for future integration w ParticipationRequest
            boolean hasActiveParticipations = false;

            if (hasActiveParticipations) {
                throw new CompetitionHierarchyValidationException(
                    "Cannot modify hierarchy: The competition is PUBLISHED and has active participations.");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CompetitionResponse> getAllVisible(Pageable pageable) {
        Specification<Competition> spec;

        if (securityFacade.hasRole("ADMIN")) {
            spec = CompetitionSpecification.isVisibleToAdmin();
        } else if (securityFacade.hasRole("ORG")) {
            Long currentUserId = securityFacade.getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException(
                    "User ID not found in security context")); //TODO: better to use custom ex
            spec = CompetitionSpecification.isVisibleToOrg(currentUserId);
        } else {
            spec = CompetitionSpecification.isVisibleToUser();
        }

        return competitionRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CompetitionResponse> getArchived(Pageable pageable) {
        return competitionRepository.findAll(CompetitionSpecification.isArchived(), pageable)
            .map(mapper::toResponse);
    }

    @Override
    @Transactional
    public CompetitionResponse changeStatus(Long id, CompetitionStatus newStatus) {
        Competition competition = competitionRepository.findById(id)
            .orElseThrow(() -> new CompetitionNotFoundException("Competition not found"));

        CompetitionStatus currentStatus = competition.getCompetitionStatus();

        validateStatusTransition(currentStatus, newStatus);

        if (newStatus == CompetitionStatus.PUBLISHED) {
            validatePublishingRequirements(id);
        }

        competition.setCompetitionStatus(newStatus);
        return mapper.toResponse(competitionRepository.save(competition));
    }

    private void validateStatusTransition(CompetitionStatus current, CompetitionStatus target) {
        if (current == target) {
            return;
        }

        boolean isValid = switch (current) {
            case DRAFT -> target == CompetitionStatus.PUBLISHED;
            case PUBLISHED -> target == CompetitionStatus.FINISHED;
            case FINISHED -> target == CompetitionStatus.ARCHIVED;
            case ARCHIVED -> false;
        };

        if (!isValid) {
            throw new CompetitionHierarchyValidationException(
                "Invalid status transition from " + current + " to " + target
            );
        }
    }

    private void validatePublishingRequirements(Long competitionId) {
        if (!stageRepository.existsByCompetitionId(competitionId)) {
            throw new CompetitionHierarchyValidationException(
                "Cannot publish: Competition must have at least one stage.");
        }

        long emptyStages = stageRepository.countStagesWithoutTours(competitionId);
        if (emptyStages > 0) {
            throw new CompetitionHierarchyValidationException(
                "Cannot publish: All stages must have at least one tour. Found " + emptyStages + " empty stage(s)."
            );
        }
    }
}
