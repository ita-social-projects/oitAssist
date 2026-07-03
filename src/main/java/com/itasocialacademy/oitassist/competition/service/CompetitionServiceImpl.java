package com.itasocialacademy.oitassist.competition.service;

import com.itasocialacademy.oitassist.competition.api.dto.CompetitionTreeResponse;
import com.itasocialacademy.oitassist.competition.api.dto.StageTreeResponse;
import com.itasocialacademy.oitassist.competition.dao.dto.request.CreateCompetitionRequest;
import com.itasocialacademy.oitassist.competition.dao.dto.response.CompetitionResponse;
import com.itasocialacademy.oitassist.competition.dao.dto.response.TourResponse;
import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import com.itasocialacademy.oitassist.competition.dao.model.Stage;
import com.itasocialacademy.oitassist.competition.dao.model.Tour;
import com.itasocialacademy.oitassist.competition.dao.repository.CompetitionRepository;
import com.itasocialacademy.oitassist.competition.dao.repository.StageRepository;
import com.itasocialacademy.oitassist.competition.dao.repository.TourRepository;
import com.itasocialacademy.oitassist.competition.dao.specification.CompetitionSpecification;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionHierarchyValidationException;
import com.itasocialacademy.oitassist.competition.exceptions.CompetitionNotFoundException;
import com.itasocialacademy.oitassist.competition.mapper.CompetitionMapper;
import com.itasocialacademy.oitassist.competition.mapper.StageMapper;
import com.itasocialacademy.oitassist.competition.mapper.TourMapper;
import com.itasocialacademy.oitassist.competition.service.interfaces.CompetitionService;
import com.itasocialacademy.oitassist.competition.validation.HierarchyValidator;
import com.itasocialacademy.oitassist.core.exceptions.UserContextNotFoundException;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompetitionServiceImpl implements CompetitionService {
    private final SecurityFacade securityFacade;
    private final CompetitionRepository competitionRepository;
    private final StageRepository stageRepository;
    private final TourRepository tourRepository;
    private final CompetitionMapper mapper;
    private final StageMapper stageMapper;
    private final TourMapper tourMapper;
    private final HierarchyValidator validator;

    @Override
    @Transactional
    public CompetitionResponse create(CreateCompetitionRequest request) {
        Competition competition = mapper.toEntity(request);
        competition.setCompetitionStatus(CompetitionStatus.DRAFT);

        return mapper.toResponse(competitionRepository.save(competition));
    }

    @Override
    @Transactional(readOnly = true)
    public CompetitionResponse getById(Long competitionId) {
        Competition competition = competitionRepository.findById(competitionId)
            .orElseThrow(() -> new CompetitionNotFoundException(competitionId));
        return mapper.toResponse(competition);
    }

    @Override
    @Transactional(readOnly = true)
    public CompetitionResponse getVisibleById(Long competitionId) {
        validator.checkVisibilityAccess(competitionId);
        return getById(competitionId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CompetitionResponse> getAllVisible(Pageable pageable) {
        Specification<Competition> spec;

        if (securityFacade.hasRole("ADMIN")) {
            spec = CompetitionSpecification.isVisibleToAdmin();
        } else if (securityFacade.hasRole("ORG")) {
            Long currentUserId = securityFacade.getCurrentUserId()
                .orElseThrow(() -> new UserContextNotFoundException("User ID not found in security context"));
            spec = CompetitionSpecification.isVisibleToOrg(currentUserId);
        } else {
            spec = CompetitionSpecification.isVisibleToUser();
        }

        return competitionRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CompetitionResponse> getArchived(Pageable pageable) {
        return competitionRepository
            .findAll(CompetitionSpecification.isArchived(), pageable)
            .map(mapper::toResponse);
    }

    @Override
    @Transactional
    public CompetitionResponse changeStatus(Long id, CompetitionStatus newStatus) {
        Competition competition = competitionRepository.findById(id)
            .orElseThrow(() -> new CompetitionNotFoundException(id));

        CompetitionStatus currentStatus = competition.getCompetitionStatus();

        validateStatusTransition(currentStatus, newStatus);

        if (newStatus == CompetitionStatus.PUBLISHED) {
            validatePublishingRequirements(id);
        }

        competition.setCompetitionStatus(newStatus);
        return mapper.toResponse(competitionRepository.save(competition));
    }

    @Override
    @Transactional(readOnly = true)
    public CompetitionTreeResponse getCompetitionTree(Long competitionId) {
        validator.checkVisibilityAccess(competitionId);
        Competition competitionEntity = competitionRepository.findById(competitionId)
            .orElseThrow(() -> new CompetitionNotFoundException(competitionId));
        CompetitionResponse competition = mapper.toResponse(competitionEntity);

        List<Stage> stages = stageRepository.findAllByCompetitionIdOrderBySortPositionAsc(competitionId);

        if (stages.isEmpty()) {
            return new CompetitionTreeResponse(competition, List.of());
        }

        List<Long> stageIds = stages.stream().map(Stage::getId).toList();

        List<Tour> allTours = tourRepository.findAllByStageIdInOrderBySortPositionAsc(stageIds);

        Map<Long, List<TourResponse>> toursByStage = allTours.stream()
            .collect(Collectors.groupingBy(
                Tour::getStageId,
                Collectors.mapping(tourMapper::toResponse, Collectors.toList())));

        List<StageTreeResponse> stageTrees = stages.stream()
            .map(stage -> new StageTreeResponse(
                stageMapper.toResponse(stage),
                toursByStage.getOrDefault(stage.getId(), List.of())))
            .toList();

        return new CompetitionTreeResponse(competition, stageTrees);
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
                "Invalid status transition from " + current + " to " + target);
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
                "Cannot publish: All stages must have at least one tour. Found " + emptyStages + " empty stage(s).");
        }
    }
}
